package com.hotpulse.service.agent;

import com.hotpulse.common.AgentConstants;
import com.hotpulse.dto.SearchResponse;
import com.hotpulse.entity.Document;
import com.hotpulse.service.iwencai.IwencaiSkillService;
import com.hotpulse.skill.VerifyTruthfulnessSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerAgent {

    private static final double TRUTH_THRESHOLD     = 0.4;
    private static final double RELEVANCE_THRESHOLD = 0.25;
    /** 批量发送给 LLM 的最大文档数，避免单次 prompt 过长 */
    private static final int BATCH_SIZE = 8;

    private final IwencaiSkillService iwencaiSkillService;
    private final VerifyTruthfulnessSkill verifyTruthfulnessSkill;
    private final AgentExecutionTracker tracker;

    /**
     * 批量分析文档列表（主入口）。
     *
     * <p>优化流程：
     * <ol>
     *   <li>外部证据查询执行一次，所有文档共享证据</li>
     *   <li>基于关键词命中计算每篇文档相关性</li>
     *   <li>LLM 真实性评估按 BATCH_SIZE 分组，大幅减少 LLM 调用次数</li>
     * </ol>
     *
     * @return 通过阈值过滤的 (Document, AnalysisResult) 列表
     */
    public List<Map.Entry<Document, AnalysisResult>> analyzeBatch(
            Long executionId, List<Document> documents, String originalQuery) {
        if (documents.isEmpty()) return List.of();

        tracker.recordStep(executionId, AgentConstants.ANALYZER_AGENT, AgentConstants.STATUS_RUNNING,
                "批量分析 " + documents.size() + " 篇文档...", null);

        // Step 1: 共享 RAG 证据（整个批次只查一次）
        List<SearchResponse.Evidence> evidences = fetchSharedEvidences(originalQuery);

        // Step 2: 用关键词命中计算每篇文档相关性，避免依赖 embedding API
        List<Double> relevanceScores = documents.stream()
                .map(doc -> computeKeywordRelevance(doc, originalQuery))
                .collect(Collectors.toList());

        // Step 3: 按 BATCH_SIZE 分组，批量调用 LLM 进行真实性评估
        List<Double> truthScores = batchVerifyTruth(documents, evidences);

        // Step 4: 组装结果，过滤低质量文档
        List<Map.Entry<Document, AnalysisResult>> results = new ArrayList<>();
        int passed = 0;
        int lowTruth = 0;
        int lowRelevance = 0;
        int lowBoth = 0;
        List<DocumentAnalysisTrace> traces = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            double truth = truthScores.get(i);
            double relevance = relevanceScores.get(i);
            double importance = computeImportanceScore(doc, evidences, relevance);
            boolean truthPassed = truth >= TRUTH_THRESHOLD;
            boolean relevancePassed = relevance >= RELEVANCE_THRESHOLD;
            boolean accepted = truthPassed && relevancePassed;

            String reason = "通过";
            if (!accepted) {
                if (!truthPassed && !relevancePassed) {
                    lowBoth++;
                    reason = "真实性和相关性不足";
                } else if (!truthPassed) {
                    lowTruth++;
                    reason = "真实性不足";
                } else {
                    lowRelevance++;
                    reason = "相关性不足";
                }
            }

            traces.add(new DocumentAnalysisTrace(doc, truth, relevance, importance, accepted, reason));

            if (!accepted) {
                continue;
            }
            results.add(Map.entry(doc, new AnalysisResult(truth, relevance, importance,
                    "批量评估 score=" + String.format("%.2f", truth))));
            passed++;
        }

        logAnalysisTraces(executionId, traces);

        String summary = buildAnalysisSummary(documents.size(), passed, lowRelevance, lowTruth, lowBoth, traces);
        tracker.recordStep(executionId, AgentConstants.ANALYZER_AGENT, AgentConstants.STATUS_DONE,
                summary, null);

        return results;
    }

    /** 保留单篇接口供外部兼容调用（内部委托给批量逻辑）。*/
    public AnalysisResult analyze(Long executionId, Document document, String originalQuery) {
        List<Map.Entry<Document, AnalysisResult>> batch = analyzeBatch(executionId, List.of(document), originalQuery);
        return batch.isEmpty() ? null : batch.get(0).getValue();
    }

    // ──────────────────────────── private helpers ────────────────────────────

    /**
     * 调用同花顺问财 SkillHub 获取金融领域权威证据，
     * 替代原本地 RAG 向量检索，无需自建知识库。
     */
    private List<SearchResponse.Evidence> fetchSharedEvidences(String query) {
        try {
            List<SearchResponse.Evidence> evidences = iwencaiSkillService.query(query, 10);
            if (!evidences.isEmpty()) {
                log.debug("Iwencai returned {} evidences for query='{}'", evidences.size(), query);
            }
            return evidences;
        } catch (Exception e) {
            log.warn("Iwencai skill query failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 按 BATCH_SIZE 分组批量调用 LLM 真实性评估，减少 LLM 调用次数。
     */
    private List<Double> batchVerifyTruth(List<Document> docs, List<SearchResponse.Evidence> evidences) {
        List<Double> results = new ArrayList<>(docs.size());

        int total = docs.size();
        for (int from = 0; from < total; from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, total);
            List<Document> chunk = docs.subList(from, to);
            try {
                List<VerifyTruthfulnessSkill.BatchResult> batchResults =
                        verifyTruthfulnessSkill.executeBatch(
                                new VerifyTruthfulnessSkill.BatchInput(chunk, evidences));
                batchResults.stream()
                        .map(VerifyTruthfulnessSkill.BatchResult::score)
                        .forEach(results::add);
            } catch (Exception e) {
                log.warn("Batch truth verification failed for chunk [{},{}]: {}", from, to, e.getMessage());
                IntStream.range(0, chunk.size()).forEach(i -> results.add(0.5));
            }
        }
        return results;
    }

    private double computeImportanceScore(Document doc,
                                          List<SearchResponse.Evidence> evidences,
                                          double relevanceScore) {
        double evidenceMax = evidences.stream()
                .mapToDouble(SearchResponse.Evidence::getScore)
                .max()
                .orElse(0.4);
        double docBonus = 0;
        if (doc.getSummary() != null && !doc.getSummary().isBlank()) docBonus += 0.05;
        if (doc.getTitle() != null && doc.getTitle().length() > 10) docBonus += 0.05;
        // 相关性越高，重要性加权越大
        double relevanceBonus = relevanceScore * 0.1;
        return Math.min(1.0, evidenceMax + docBonus + relevanceBonus);
    }

    /** 关键词覆盖率，用于判断监控关键词与候选文档的相关性。*/
    private double computeKeywordRelevance(Document doc, String query) {
        if (query.isBlank()) return 0.5;
        String docText = (
                (doc.getTitle() != null ? doc.getTitle() : "")
                + " " + (doc.getSummary() != null ? doc.getSummary() : "")
                + " " + (doc.getContent() != null ? doc.getContent().substring(0, Math.min(doc.getContent().length(), 500)) : "")
        ).toLowerCase();
        String[] terms = query.toLowerCase().split("[\\s,，。！？、；:：]+");
        long meaningful = java.util.Arrays.stream(terms).filter(t -> t.length() >= 2).count();
        if (meaningful == 0) return 0.5;
        long matches = java.util.Arrays.stream(terms).filter(t -> t.length() >= 2 && docText.contains(t)).count();
        return Math.min(1.0, 0.3 + (double) matches / meaningful * 0.7);
    }

    private String buildAnalysisSummary(int total,
                                        int passed,
                                        int lowRelevance,
                                        int lowTruth,
                                        int lowBoth,
                                        List<DocumentAnalysisTrace> traces) {
        StringBuilder sb = new StringBuilder();
        sb.append("分析完成：").append(total).append(" 篇 → 通过 ").append(passed).append(" 篇");
        sb.append("；过滤：相关性不足 ").append(lowRelevance)
                .append("，真实性不足 ").append(lowTruth)
                .append("，两者不足 ").append(lowBoth);

        traces.stream()
                .sorted((a, b) -> Double.compare(b.combinedScore(), a.combinedScore()))
                .limit(3)
                .forEach(trace -> sb.append("\n")
                        .append(trace.accepted() ? "通过" : "过滤")
                        .append("：")
                        .append(shortTitle(trace.doc()))
                        .append(" truth=").append(formatScore(trace.truth()))
                        .append(" relevance=").append(formatScore(trace.relevance()))
                        .append("（").append(trace.reason()).append("）"));
        return sb.toString();
    }

    private void logAnalysisTraces(Long executionId, List<DocumentAnalysisTrace> traces) {
        traces.stream()
                .sorted((a, b) -> Double.compare(b.combinedScore(), a.combinedScore()))
                .forEach(trace -> log.info(
                        "AnalyzerAgent result executionId={} docId={} accepted={} reason={} truth={} relevance={} importance={} title={}",
                        executionId,
                        trace.doc().getId(),
                        trace.accepted(),
                        trace.reason(),
                        formatScore(trace.truth()),
                        formatScore(trace.relevance()),
                        formatScore(trace.importance()),
                        shortTitle(trace.doc())));
    }

    private String shortTitle(Document doc) {
        String title = doc.getTitle() != null && !doc.getTitle().isBlank()
                ? doc.getTitle()
                : "无标题";
        return title.length() <= 60 ? title : title.substring(0, 57) + "...";
    }

    private String formatScore(double value) {
        return String.format("%.2f", value);
    }

    public record AnalysisResult(
            double truthScore,
            double relevanceScore,
            double importanceScore,
            String evidence
    ) {}

    private record DocumentAnalysisTrace(
            Document doc,
            double truth,
            double relevance,
            double importance,
            boolean accepted,
            String reason
    ) {
        double combinedScore() {
            return truth * 0.5 + relevance * 0.4 + importance * 0.1;
        }
    }
}
