package com.hotpulse.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.common.AgentConstants;
import com.hotpulse.dto.AgentStepEvent;
import com.hotpulse.dto.HotspotResponse;
import com.hotpulse.dto.SearchResponse;
import com.hotpulse.dto.TaskPlanDto;
import com.hotpulse.entity.Document;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.entity.Source;
import com.hotpulse.repository.MessageRepository;
import com.hotpulse.repository.SourceRepository;
import com.hotpulse.service.crawler.CandidateItem;
import com.hotpulse.service.hotspot.HotspotService;
import com.hotpulse.service.hotspot.KeywordExpansionService;
import com.hotpulse.service.iwencai.IwencaiSkillService;
import com.hotpulse.sse.AgentSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private static final int MAX_CRAWL_CANDIDATES = 20;
    private static final int MAX_CRAWL_CANDIDATES_PER_SOURCE = 5;
    private static final int HOTSPOT_CONTEXT_MAX_CHARS = 4000;
    private static final int CHAT_HOTSPOT_SEARCH_LIMIT = 8;
    private static final int CHAT_HOTSPOT_CANDIDATE_LIMIT = 30;

    private final PlannerAgent plannerAgent;
    private final SearcherAgent searcherAgent;
    private final CrawlerAgent crawlerAgent;
    private final AnalyzerAgent analyzerAgent;
    private final AggregatorAgent aggregatorAgent;
    private final AgentExecutionTracker tracker;
    private final AgentExecutionService executionService;
    private final AgentSseService agentSseService;
    private final SourceRepository sourceRepository;
    private final IwencaiSkillService iwencaiSkillService;
    private final HotspotService hotspotService;
    private final KeywordExpansionService keywordExpansionService;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService virtualThreadExecutor;
    private final ChatClient chatClient;

    public void execute(Long executionId, String query, Long conversationId) {
        execute(executionId, query, conversationId, null);
    }

    public void execute(Long executionId, String query, Long conversationId, Long hotspotId) {
        try {
            log.info("AgentOrchestrator starting executionId={} query={} hotspotId={}",
                    executionId, query, hotspotId);

            if (hotspotId != null) {
                executeHotspotChat(executionId, query, conversationId, hotspotId);
                return;
            }

            if (isCasualChat(query)) {
                executeCasualChat(executionId, query, conversationId);
                return;
            }

            Optional<String> localSearchKeyword = extractHotspotSearchKeyword(query);
            if (localSearchKeyword.isPresent()) {
                executeHotspotLibrarySearch(executionId, query, conversationId, localSearchKeyword.get());
                return;
            }

            // Step 1: Planner（含意图分类）
            TaskPlanDto plan = plannerAgent.plan(executionId, query);

            // 持久化 Task Plan JSON（供历史审查接口使用）
            try {
                executionService.saveTaskPlan(executionId, objectMapper.writeValueAsString(plan));
            } catch (Exception e) {
                log.warn("Failed to save task plan for executionId={}", executionId);
            }

            // 意图路由：chat 直接走对话管线，search 走全量检索管线
            if ("chat".equalsIgnoreCase(plan.getIntent())) {
                executeDirectChat(executionId, query, conversationId);
                return;
            }

            // Step 2: 并行搜索（SearcherAgent × N）
            List<Source> sources = resolveSources(plan);
            List<CandidateItem> allCandidates = parallelSearch(executionId, sources, plan.getKeywords());

            // Step 3: 并行抓取（CrawlerAgent × N）
            List<Document> documents = parallelCrawl(executionId, allCandidates, sources);

            // Step 4: 逐条分析（AnalyzerAgent，各自独立）
            List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> analysisResults = analyze(executionId, documents, query);

            // Step 5: 聚合
            List<Hotspot> hotspots = aggregatorAgent.aggregate(executionId, analysisResults);

            // Step 6: 用同花顺问财获取证据，结合热点摘要生成回答
            List<SearchResponse.Evidence> iwencaiEvidences = fetchIwencaiEvidences(query);
            String answer = buildHotspotSummary(hotspots, query);

            // Step 7: 保存 assistant 消息到对话历史
            if (conversationId != null) {
                saveAssistantMessage(conversationId, answer, iwencaiEvidences);
            }

            // Step 8: 完成，推送最终 SSE 事件（含 answer 和 hotspots[]）
            executionService.markDone(executionId);
            sendFinalEvent(executionId, hotspots, answer);

        } catch (Exception e) {
            log.error("AgentOrchestrator failed for executionId={}", executionId, e);
            executionService.markFailed(executionId);

            String errorAnswer = "抱歉，查询执行失败：" + e.getMessage();

            // 保存错误消息到对话历史，让用户在聊天窗口看到具体原因
            if (conversationId != null) {
                saveAssistantMessage(conversationId, errorAnswer, java.util.List.of());
            }

            // 推送包含错误信息的终止 SSE 事件，前端可渲染出错误回答
            AgentStepEvent errorEvent = new AgentStepEvent(
                    "System",
                    AgentConstants.STATUS_FAILED,
                    e.getMessage(),
                    Instant.now(),
                    errorAnswer,
                    java.util.List.of()
            );
            agentSseService.broadcast(executionId.toString(), errorEvent);
        } finally {
            agentSseService.complete(executionId.toString());
        }
    }

    /**
     * 纯对话模式：不启动搜索/抓取管线，直接用 LLM 结合对话历史回复用户。
     */
    private void executeDirectChat(Long executionId, String query, Long conversationId) {
        tracker.recordStep(executionId, "ChatAgent", AgentConstants.STATUS_RUNNING,
                "正在生成对话回答...", null);
        try {
            String promptText = buildDirectChatPrompt(query, conversationId, null);
            String answer = callChatLlm(promptText);

            tracker.recordStep(executionId, "ChatAgent", AgentConstants.STATUS_DONE,
                    "已生成对话回答", null);

            if (conversationId != null) {
                saveAssistantMessage(conversationId, answer, java.util.List.of());
            }

            executionService.markDone(executionId);
            sendFinalEvent(executionId, java.util.List.of(), answer);

        } catch (Exception e) {
            log.error("executeDirectChat failed for executionId={}", executionId, e);
            throw e;
        }
    }

    private void executeCasualChat(Long executionId, String query, Long conversationId) {
        tracker.recordStep(executionId, "Chat", AgentConstants.STATUS_RUNNING,
                "正在识别对话意图...", null);
        String answer = """
                你好，我是 HotPulse AI，主要帮你围绕已抓取的财经热点做检索和分析。

                你可以这样问：
                - 搜索 A股 热点
                - 苹果 AI 最近有哪些热点？
                - 选中一条热点后，问它为什么重要、影响哪些公司、后续看什么
                """;
        tracker.recordStep(executionId, "Chat", AgentConstants.STATUS_DONE,
                "已生成对话引导", null);
        if (conversationId != null) {
            saveAssistantMessage(conversationId, answer, java.util.List.of());
        }
        executionService.markDone(executionId);
        sendFinalEvent(executionId, java.util.List.of(), answer, "对话完成");
    }

    private void executeHotspotLibrarySearch(Long executionId, String query, Long conversationId, String keyword) {
        tracker.recordStep(executionId, "Intent", AgentConstants.STATUS_DONE,
                "识别为热点库检索，关键词：" + keyword, null);
        tracker.recordStep(executionId, "HotspotSearch", AgentConstants.STATUS_RUNNING,
                "正在检索已有热点库...", null);

        @SuppressWarnings("unchecked")
        List<HotspotResponse> candidates = (List<HotspotResponse>) hotspotService
                .getHotspots("relevance", 1, CHAT_HOTSPOT_CANDIDATE_LIMIT, null, null, null, keyword)
                .getOrDefault("items", java.util.List.of());
        List<HotspotResponse> hotspots = rankChatHotspotResults(keyword, candidates)
                .stream()
                .limit(CHAT_HOTSPOT_SEARCH_LIMIT)
                .toList();

        String answer = buildHotspotLibraryAnswer(keyword, hotspots);
        tracker.recordStep(executionId, "HotspotSearch", AgentConstants.STATUS_DONE,
                hotspots.isEmpty() ? "热点库暂无匹配内容" : "已找到 " + hotspots.size() + " 条相关热点", null);

        if (conversationId != null) {
            saveAssistantMessage(conversationId, answer, java.util.List.of());
        }

        executionService.markDone(executionId);
        sendFinalDtoEvent(executionId, hotspots, answer,
                hotspots.isEmpty() ? "检索完成，暂无匹配热点" : "检索完成，共返回 " + hotspots.size() + " 条热点");
    }

    /**
     * 热点上下文对话：基于已选热点资料 + 对话历史回答，不触发搜索/抓取管线。
     */
    private void executeHotspotChat(Long executionId, String query, Long conversationId, Long hotspotId) {
        tracker.recordStep(executionId, "HotspotChat", AgentConstants.STATUS_RUNNING,
                "正在加载热点上下文...", null);
        try {
            HotspotResponse hotspot = hotspotService.getHotspotDetail(hotspotId);
            String hotspotContext = buildHotspotContextBlock(hotspot);
            String promptText = buildDirectChatPrompt(query, conversationId, hotspotContext);

            tracker.recordStep(executionId, "HotspotChat", AgentConstants.STATUS_RUNNING,
                    "正在基于热点上下文生成回答...", null);

            String answer = callChatLlm(promptText);

            tracker.recordStep(executionId, "HotspotChat", AgentConstants.STATUS_DONE,
                    "已基于热点上下文生成回答", null);

            if (conversationId != null) {
                saveAssistantMessage(conversationId, answer, java.util.List.of());
            }

            executionService.markDone(executionId);
            sendFinalEvent(executionId, java.util.List.of(), answer);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("Hotspot not found for hotspotId={}", hotspotId);
            String errorAnswer = "抱歉，所选热点不存在或已被删除。";
            tracker.recordStep(executionId, "HotspotChat", AgentConstants.STATUS_FAILED,
                    errorAnswer, null);
            if (conversationId != null) {
                saveAssistantMessage(conversationId, errorAnswer, java.util.List.of());
            }
            executionService.markFailed(executionId);
            sendFinalEvent(executionId, java.util.List.of(), errorAnswer);
        } catch (Exception e) {
            log.error("executeHotspotChat failed for executionId={} hotspotId={}", executionId, hotspotId, e);
            throw e;
        }
    }

    private String buildDirectChatPrompt(String query, Long conversationId, String hotspotContextBlock) {
        String historyContext = buildHistoryContext(conversationId);
        boolean hasHotspot = hotspotContextBlock != null && !hotspotContextBlock.isBlank();
        boolean hasHistory = !historyContext.isBlank();

        if (hasHotspot && hasHistory) {
            return """
                    你是 HotPulse AI，一个专注财经热点的智能助手。用户正在讨论以下热点，请优先基于热点资料与对话历史回答，不要编造未提供的信息。

                    【当前热点资料】
                    %s

                    【对话历史】
                    %s

                    用户: %s

                    请基于热点资料与对话历史，以友好、简洁的方式回答用户。
                    """.formatted(hotspotContextBlock, historyContext, query);
        }
        if (hasHotspot) {
            return """
                    你是 HotPulse AI，一个专注财经热点的智能助手。用户正在讨论以下热点，请基于资料回答，不要编造未提供的信息。

                    【当前热点资料】
                    %s

                    用户: %s

                    请基于热点资料，以友好、简洁的方式回答用户。
                    """.formatted(hotspotContextBlock, query);
        }
        if (hasHistory) {
            return """
                    你是 HotPulse AI，一个专注财经热点的智能助手。以下是本次对话的历史记录：

                    %s

                    用户: %s

                    请基于对话历史，以友好、简洁的方式回答用户。
                    """.formatted(historyContext, query);
        }
        return """
                你是 HotPulse AI，一个专注财经热点的分析助手。你的核心能力是基于已抓取热点、信息源和对话历史做解释、归纳与判断辅助。
                用户消息：%s
                如果用户想搜索最新内容，请说明你会优先检索当前热点库；如果库内没有内容，引导用户去热点雷达添加或触发监控关键词。
                请以克制、清晰、专业的方式回答，不要使用波浪号或过度口语化表达。
                """.formatted(query);
    }

    private boolean isCasualChat(String query) {
        String normalized = normalizeQuery(query);
        return Set.of("你好", "您好", "hello", "hi", "嗨", "在吗", "你是谁", "你能做什么", "能做什么")
                .contains(normalized);
    }

    private Optional<String> extractHotspotSearchKeyword(String query) {
        String normalized = normalizeQuery(query);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        boolean hasSearchVerb = normalized.matches(".*(搜索|搜|查找|查询|检索|找一下|找找|帮我找|帮我查).*");
        boolean hasHotspotNoun = normalized.contains("热点") || normalized.contains("新闻") || normalized.contains("资讯") || normalized.contains("相关内容");
        boolean hasAnalysisVerb = normalized.matches(".*(为什么|原因|影响|怎么看|如何看|是否|可信吗|重要|风险|后续|解读|分析).*");
        if ((!hasSearchVerb && !hasHotspotNoun) || hasAnalysisVerb) {
            return Optional.empty();
        }

        String keyword = normalized
                .replaceAll("(请|帮我|帮忙|可以|能不能|能否|麻烦|一下|搜索|搜一下|搜|查找|查询|查一下|检索|找一下|找找|帮我找|帮我查)", " ")
                .replaceAll("(最新|最近|相关内容|相关|新闻|资讯|热点|内容|关于|的)", " ")
                .replaceAll("[，。！？、；：,.!?;:\\[\\]【】()（）\"'“”]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (keyword.isBlank() || keyword.length() > 40) {
            return Optional.empty();
        }
        return Optional.of(formatKeywordForDisplay(keyword));
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().replaceAll("\\s+", " ");
    }

    private String formatKeywordForDisplay(String keyword) {
        if ("a股".equalsIgnoreCase(keyword)) {
            return "A股";
        }
        if ("ai".equalsIgnoreCase(keyword)) {
            return "AI";
        }
        return keyword;
    }

    private String buildHotspotLibraryAnswer(String keyword, List<HotspotResponse> hotspots) {
        if (hotspots.isEmpty()) {
            return "当前热点库里暂时没有找到与「" + keyword + "」直接相关的内容。你可以先在热点雷达里添加或触发这个关键词，抓取完成后我再继续帮你分析。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("我在当前热点库里找到 ").append(hotspots.size())
                .append(" 条与「").append(keyword).append("」相关的热点：\n\n");
        hotspots.stream().limit(5).forEach(h -> {
            sb.append("- ").append(h.getTitle() != null ? h.getTitle() : "（无标题）");
            if (h.getSource() != null && !h.getSource().isBlank()) {
                sb.append("｜").append(h.getSource());
            }
            if (h.getSummary() != null && !h.getSummary().isBlank()) {
                sb.append("\n  ").append(truncateInline(h.getSummary(), 90));
            }
            sb.append("\n");
        });
        if (hotspots.size() > 5) {
            sb.append("\n其余 ").append(hotspots.size() - 5).append(" 条可以在右侧热点列表继续查看。");
        }
        return sb.toString();
    }

    private List<HotspotResponse> rankChatHotspotResults(String keyword, List<HotspotResponse> candidates) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .sorted(Comparator
                        .comparingDouble((HotspotResponse h) -> chatSearchScore(h, normalizedKeyword)).reversed()
                        .thenComparing((HotspotResponse h) -> Optional.ofNullable(h.getHotScore()).orElse(0.0), Comparator.reverseOrder())
                        .thenComparing((HotspotResponse h) -> Optional.ofNullable(h.getPublishedAt()).orElse(Instant.EPOCH), Comparator.reverseOrder()))
                .toList();
    }

    private double chatSearchScore(HotspotResponse hotspot, String normalizedKeyword) {
        double score = Optional.ofNullable(hotspot.getRelevanceScore()).orElse(0.0);
        String title = lower(hotspot.getTitle());
        String summary = lower(hotspot.getSummary());
        String monitorKeyword = lower(hotspot.getMonitorKeyword());
        String source = lower(hotspot.getSource());
        if (title.contains(normalizedKeyword)) {
            score += 5.0;
        }
        if (hotspot.getTags() != null && hotspot.getTags().stream().anyMatch(tag -> lower(tag).contains(normalizedKeyword))) {
            score += 4.0;
        }
        if (summary.contains(normalizedKeyword)) {
            score += 2.0;
        }
        if (monitorKeyword.contains(normalizedKeyword)) {
            score += 1.5;
        }
        if (source.contains(normalizedKeyword)) {
            score += 0.5;
        }
        return score;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String truncateInline(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "…";
    }

    private String buildHistoryContext(Long conversationId) {
        if (conversationId == null) {
            return "";
        }
        List<com.hotpulse.entity.Message> history =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<com.hotpulse.entity.Message> prevMessages = history.size() > 1
                ? history.subList(Math.max(0, history.size() - 11), history.size() - 1)
                : Collections.emptyList();
        if (prevMessages.isEmpty()) {
            return "";
        }
        return prevMessages.stream()
                .map(m -> ("user".equals(m.getRole()) ? "用户" : "助手") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String buildHotspotContextBlock(HotspotResponse hotspot) {
        String title = hotspot.getTitle() != null && !hotspot.getTitle().isBlank()
                ? hotspot.getTitle() : "（无标题）";
        String summary = hotspot.getSummary() != null ? hotspot.getSummary() : "";
        String source = hotspot.getSource() != null ? hotspot.getSource() : "";
        String url = hotspot.getUrl() != null ? hotspot.getUrl() : "";
        String evidence = hotspot.getAnalysisEvidence() != null ? hotspot.getAnalysisEvidence() : "";
        String fullText = truncate(hotspot.getFullText(), HOTSPOT_CONTEXT_MAX_CHARS);

        return """
                标题: %s
                来源: %s
                链接: %s
                摘要: %s
                真实性评分: %s
                相关性评分: %s
                重要性评分: %s
                分析证据: %s
                正文摘录:
                %s
                """.formatted(
                title,
                source,
                url,
                summary,
                formatScore(hotspot.getTruthScore()),
                formatScore(hotspot.getRelevanceScore()),
                formatScore(hotspot.getImportanceScore()),
                evidence,
                fullText);
    }

    private String formatScore(Double score) {
        return score != null ? String.format("%.2f", score) : "N/A";
    }

    private String truncate(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return "（无正文）";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "…";
    }

    private String callChatLlm(String promptText) {
        try {
            return chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Chat LLM call failed", e);
            return "抱歉，生成回答时发生错误：" + e.getMessage();
        }
    }

    private List<SearchResponse.Evidence> fetchIwencaiEvidences(String query) {
        try {
            return iwencaiSkillService.query(query, 8);
        } catch (Exception e) {
            log.warn("Iwencai evidence fetch failed for query: {}", query, e);
            return java.util.List.of();
        }
    }

    private void saveAssistantMessage(Long conversationId, String answer, List<SearchResponse.Evidence> evidences) {
        try {
            com.hotpulse.entity.Message msg = new com.hotpulse.entity.Message();
            msg.setConversationId(conversationId);
            msg.setRole("assistant");
            msg.setContent(answer);
            if (evidences != null && !evidences.isEmpty()) {
                try {
                    msg.setSourcesJson(objectMapper.writeValueAsString(evidences));
                } catch (Exception ignored) {}
            }
            messageRepository.save(msg);
        } catch (Exception e) {
            log.warn("Failed to save assistant message for conversationId={}", conversationId, e);
        }
    }

    private List<Source> resolveSources(TaskPlanDto plan) {
        // 始终使用数据库中所有已启用的信息源。
        // Planner 生成的 sources 列表仅作日志参考，不做过滤：
        // 因为 LLM 倾向于推荐 Bloomberg/TechCrunch 等英文源，而我们数据库存的是中文源，
        // 用名称匹配做过滤会导致大部分中文源被错误剔除。
        return sourceRepository.findByEnabledTrue();
    }

    private List<CandidateItem> parallelSearch(Long executionId, List<Source> sources, List<String> keywords) {
        if (sources.isEmpty()) return Collections.emptyList();

        List<CompletableFuture<List<CandidateItem>>> futures = sources.stream()
                .map(source -> CompletableFuture.supplyAsync(
                        () -> searcherAgent.search(executionId, source, keywords),
                        virtualThreadExecutor))
                .collect(Collectors.toList());

        return futures.stream()
                .map(f -> {
                    try {
                        return f.join();
                    } catch (Exception e) {
                        log.warn("SearcherAgent future failed", e);
                        return Collections.<CandidateItem>emptyList();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<Document> parallelCrawl(Long executionId, List<CandidateItem> candidates, List<Source> sources) {
        if (candidates.isEmpty()) return Collections.emptyList();

        Long defaultSourceId = sources.isEmpty() ? null : sources.get(0).getId();
        List<CandidateItem> selectedCandidates = selectCrawlCandidates(candidates);

        List<CompletableFuture<Document>> futures = selectedCandidates.stream()
                .map(candidate -> CompletableFuture.supplyAsync(
                        () -> crawlerAgent.crawl(
                                executionId,
                                candidate.getUrl(),
                                candidate.getSourceId() != null ? candidate.getSourceId() : defaultSourceId,
                                candidate.getPublishedAt()),
                        virtualThreadExecutor))
                .collect(Collectors.toList());

        return futures.stream()
                .map(f -> {
                    try {
                        return f.join();
                    } catch (Exception e) {
                        log.warn("CrawlerAgent future failed", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<CandidateItem> selectCrawlCandidates(List<CandidateItem> candidates) {
        Map<String, List<CandidateItem>> bySource = new LinkedHashMap<>();
        Set<String> seenUrls = new HashSet<>();

        for (CandidateItem candidate : candidates) {
            if (candidate.getUrl() == null || candidate.getUrl().isBlank()) {
                continue;
            }
            if (!seenUrls.add(candidate.getUrl())) {
                continue;
            }
            String sourceKey = candidate.getSourceId() != null
                    ? candidate.getSourceId().toString()
                    : (candidate.getSourceName() != null ? candidate.getSourceName() : "unknown");
            bySource.computeIfAbsent(sourceKey, ignored -> new ArrayList<>()).add(candidate);
        }

        List<CandidateItem> selected = new ArrayList<>();
        int maxPerSource = bySource.size() <= 1
                ? MAX_CRAWL_CANDIDATES
                : MAX_CRAWL_CANDIDATES_PER_SOURCE;

        for (List<CandidateItem> sourceCandidates : bySource.values()) {
            sourceCandidates.stream()
                    .limit(maxPerSource)
                    .forEach(selected::add);
            if (selected.size() >= MAX_CRAWL_CANDIDATES) {
                break;
            }
        }

        return selected.stream()
                .limit(MAX_CRAWL_CANDIDATES)
                .collect(Collectors.toList());
    }

    private List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> analyze(
            Long executionId, List<Document> documents, String query) {
        // 委托给新的批量分析方法（RAG 共享 + 批量 LLM + 并行 embedding）
        try {
            return analyzerAgent.analyzeBatch(executionId, documents, query);
        } catch (Exception e) {
            log.warn("analyzeBatch failed, results may be empty: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 当 RAG 知识库无内容或 LLM 限流时，用热点标题拼装一条有用的回答。
     */
    private String buildHotspotSummary(List<Hotspot> hotspots, String query) {
        if (hotspots.isEmpty()) {
            return "未找到与「" + query + "」相关的热点内容，建议稍后重试或换个关键词。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("根据最新资讯，为您找到 ").append(hotspots.size()).append(" 条相关热点：\n\n");
        hotspots.stream().limit(5).forEach(h -> {
            HotspotResponse r = hotspotService.toResponse(h);
            sb.append("• ").append(r.getTitle() != null ? r.getTitle() : "（无标题）");
            if (r.getSource() != null) {
                sb.append("（").append(r.getSource()).append("）");
            }
            sb.append("\n");
        });
        if (hotspots.size() > 5) {
            sb.append("……及其他 ").append(hotspots.size() - 5).append(" 条，详见右侧热点列表。");
        }
        return sb.toString();
    }

    private void sendFinalEvent(Long executionId, List<Hotspot> hotspots, String answer) {
        sendFinalEvent(executionId, hotspots, answer, "处理完成，共返回 " + hotspots.size() + " 条热点");
    }

    private void sendFinalEvent(Long executionId, List<Hotspot> hotspots, String answer, String message) {
        // 将 Hotspot 实体转换为 DTO，供前端 SSE 末尾事件消费
        List<HotspotResponse> hotspotResponses = hotspots.stream()
                .map(hotspotService::toResponse)
                .collect(Collectors.toList());

        sendFinalDtoEvent(executionId, hotspotResponses, answer, message);
    }

    private void sendFinalDtoEvent(Long executionId, List<HotspotResponse> hotspotResponses, String answer, String message) {
        AgentStepEvent finalEvent = new AgentStepEvent(
                "System",
                AgentConstants.STATUS_DONE,
                message,
                Instant.now(),
                answer,
                hotspotResponses
        );
        agentSseService.broadcast(executionId.toString(), finalEvent);
    }

    /**
     * 定时监控模式：由 {@code CrawlScheduleJob} 调用，跳过 PlannerAgent，
     * 直接用传入的监控关键词执行 搜索 → 抓取 → 分析 → 聚合 全流程。
     * 结果异步写入 hotspot 表，不推送 SSE 事件（无在线用户等待）。
     */
    public void executeScheduled(Long executionId, List<String> keywords) {
        try {
            List<String> originalKeywords = keywords != null ? keywords : java.util.List.of();
            List<String> expandedKeywords = keywordExpansionService.expand(originalKeywords);
            List<String> expandedAliases = keywordExpansionService.aliasesOnly(originalKeywords);
            String keywordText = String.join(", ", originalKeywords);
            String startMessage = expandedAliases.isEmpty()
                    ? "定时监控抓取启动，关键词: " + keywordText
                    : "定时监控抓取启动，关键词: " + keywordText
                    + "，扩展词: " + String.join(", ", expandedAliases);
            tracker.recordStep(executionId, "ScheduledCrawl", AgentConstants.STATUS_RUNNING,
                    startMessage, null);

            List<Source> sources = sourceRepository.findByEnabledTrue();
            if (sources.isEmpty()) {
                log.warn("executeScheduled: 没有已启用的数据源，终止本次定时抓取");
                executionService.markDone(executionId);
                sendFinalEvent(executionId, java.util.List.of(), null);
                return;
            }

            List<CandidateItem> candidates = parallelSearch(executionId, sources, expandedKeywords);
            List<Document> documents = parallelCrawl(executionId, candidates, sources);
            List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> analysisResults =
                    analyze(executionId, documents, keywordText);
            List<Hotspot> hotspots = aggregatorAgent.aggregate(executionId, analysisResults);

            executionService.markDone(executionId);
            tracker.recordStep(executionId, "ScheduledCrawl", AgentConstants.STATUS_DONE,
                    "定时监控抓取完成，共生成 " + hotspots.size() + " 条热点", null);
            sendFinalEvent(executionId, hotspots, null);
            log.info("executeScheduled done, executionId={}, docs={}, hotspots={}", executionId, documents.size(), hotspots.size());
        } catch (Exception e) {
            log.error("executeScheduled failed for executionId={}", executionId, e);
            executionService.markFailed(executionId);
            AgentStepEvent errorEvent = new AgentStepEvent(
                    "System",
                    AgentConstants.STATUS_FAILED,
                    "监控抓取失败: " + e.getMessage(),
                    Instant.now(),
                    null,
                    java.util.List.of()
            );
            agentSseService.broadcast(executionId.toString(), errorEvent);
        } finally {
            agentSseService.complete(executionId.toString());
        }
    }
}
