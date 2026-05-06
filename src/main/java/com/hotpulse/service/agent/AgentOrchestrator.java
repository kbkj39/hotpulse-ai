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
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService virtualThreadExecutor;
    private final ChatClient chatClient;

    public void execute(Long executionId, String query, Long conversationId) {
        try {
            log.info("AgentOrchestrator starting executionId={} query={}", executionId, query);

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
            // 加载对话历史（排除刚入库的当前 user 消息，取倒数 10 条提供上下文）
            String historyContext = "";
            if (conversationId != null) {
                List<com.hotpulse.entity.Message> history =
                        messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
                // 最后一条是当前 user 消息，截掉；最多取前 10 条避免超出 token 限制
                List<com.hotpulse.entity.Message> prevMessages = history.size() > 1
                        ? history.subList(Math.max(0, history.size() - 11), history.size() - 1)
                        : Collections.emptyList();
                if (!prevMessages.isEmpty()) {
                    historyContext = prevMessages.stream()
                            .map(m -> ("user".equals(m.getRole()) ? "用户" : "助手") + ": " + m.getContent())
                            .collect(Collectors.joining("\n"));
                }
            }

            String promptText;
            if (!historyContext.isEmpty()) {
                promptText = """
                        你是 HotPulse AI，一个专注财经热点的智能助手。以下是本次对话的历史记录：

                        %s

                        用户: %s

                        请基于对话历史，以友好、简洁的方式回答用户。
                        """.formatted(historyContext, query);
            } else {
                promptText = """
                        你是 HotPulse AI，一个专注财经热点的智能助手。
                        用户消息：%s
                        请以友好、简洁的方式回答。
                        """.formatted(query);
            }

            String answer;
            try {
                answer = chatClient.prompt()
                        .user(promptText)
                        .call()
                        .content();
            } catch (Exception e) {
                log.error("DirectChat LLM call failed", e);
                answer = "抱歉，生成回答时发生错误：" + e.getMessage();
            }

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

        List<CompletableFuture<Document>> futures = candidates.stream()
                .limit(20) // 限制单次最大抓取数量
                .map(candidate -> CompletableFuture.supplyAsync(
                        () -> crawlerAgent.crawl(executionId, candidate.getUrl(), defaultSourceId),
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

    private void sendFinalEvent(Long executionId, List<Hotspot> hotspots, String answer) {        // 将 Hotspot 实体转换为 DTO，供前端 SSE 末尾事件消费
        List<HotspotResponse> hotspotResponses = hotspots.stream()
                .map(hotspotService::toResponse)
                .collect(Collectors.toList());

        AgentStepEvent finalEvent = new AgentStepEvent(
                "System",
                AgentConstants.STATUS_DONE,
                "所有 Agent 执行完毕，共生成 " + hotspots.size() + " 条热点",
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
            tracker.recordStep(executionId, "ScheduledCrawl", AgentConstants.STATUS_RUNNING,
                    "定时监控抓取启动，关键词: " + String.join(", ", keywords), null);

            List<Source> sources = sourceRepository.findByEnabledTrue();
            if (sources.isEmpty()) {
                log.warn("executeScheduled: 没有已启用的数据源，终止本次定时抓取");
                executionService.markDone(executionId);
                sendFinalEvent(executionId, java.util.List.of(), null);
                return;
            }

            List<CandidateItem> candidates = parallelSearch(executionId, sources, keywords);
            List<Document> documents = parallelCrawl(executionId, candidates, sources);
            String query = String.join(" ", keywords);
            List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> analysisResults =
                    analyze(executionId, documents, query);
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
