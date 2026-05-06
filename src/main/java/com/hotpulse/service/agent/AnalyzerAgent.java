package com.hotpulse.service.agent;

import com.hotpulse.client.EmbeddingClient;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
    private final EmbeddingClient embeddingClient;
    private final AgentExecutionTracker tracker;
    private final ExecutorService virtualThreadExecutor;

    /**
     * 批量分析文档列表（主入口）。
     *
     * <p>优化流程：
     * <ol>
     *   <li>RAG 查询执行一次，所有文档共享证据</li>
     *   <li>Query embedding 执行一次，并行计算每篇语义相关性</li>
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

        // Step 2: 计算 query embedding（一次），并行计算每篇文档语义相关性
        float[] queryVector = safeEmbed(originalQuery);
        List<Double> relevanceScores = computeSemanticRelevances(documents, queryVector);

        // Step 3: 按 BATCH_SIZE 分组，批量调用 LLM 进行真实性评估
        List<Double> truthScores = batchVerifyTruth(documents, evidences);

        // Step 4: 组装结果，过滤低质量文档
        List<Map.Entry<Document, AnalysisResult>> results = new ArrayList<>();
        int passed = 0;
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            double truth = truthScores.get(i);
            double relevance = relevanceScores.get(i);
            double importance = computeImportanceScore(doc, evidences, relevance);

            if (truth < TRUTH_THRESHOLD || relevance < RELEVANCE_THRESHOLD) {
                log.debug("文档过滤 docId={} truth={:.2f} relevance={:.2f}", doc.getId(), truth, relevance);
                continue;
            }
            results.add(Map.entry(doc, new AnalysisResult(truth, relevance, importance,
                    "批量评估 score=" + String.format("%.2f", truth))));
            passed++;
        }

        tracker.recordStep(executionId, AgentConstants.ANALYZER_AGENT, AgentConstants.STATUS_DONE,
                "分析完成：" + documents.size() + " 篇 → 通过 " + passed + " 篇", null);

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
     * 并行计算每篇文档与 query 的语义余弦相似度。
     * 文档侧使用「标题 + 摘要」的 embedding（比全文 embedding 快且代表性强）。
     * 若 embedding 失败则退化为关键词覆盖率。
     */
    private List<Double> computeSemanticRelevances(List<Document> docs, float[] queryVector) {
        if (queryVector.length == 0) {
            // embedding 不可用，退化为关键词匹配
            return docs.stream()
                    .map(d -> computeKeywordRelevance(d, ""))
                    .collect(Collectors.toList());
        }

        List<CompletableFuture<Double>> futures = docs.stream()
                .map(doc -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String docText = buildDocRepresentation(doc);
                        float[] docVector = embeddingClient.embed(docText);
                        return (double) cosineSimilarity(queryVector, docVector);
                    } catch (Exception e) {
                        log.debug("Embedding failed for doc {}, fallback to keyword", doc.getId());
                        return computeKeywordRelevance(doc, "");
                    }
                }, virtualThreadExecutor))
                .collect(Collectors.toList());

        return futures.stream()
                .map(f -> {
                    try { return f.join(); }
                    catch (Exception e) { return 0.4; }
                })
                .collect(Collectors.toList());
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
        // 语义相关性越高，重要性加权越大
        double relevanceBonus = relevanceScore * 0.1;
        return Math.min(1.0, evidenceMax + docBonus + relevanceBonus);
    }

    /** 文档表示：标题 + 摘要（若有），用于 embedding 计算，比全文短且语义集中。*/
    private String buildDocRepresentation(Document doc) {
        StringBuilder sb = new StringBuilder();
        if (doc.getTitle() != null) sb.append(doc.getTitle()).append(" ");
        if (doc.getSummary() != null) sb.append(doc.getSummary());
        else if (doc.getContent() != null)
            sb.append(doc.getContent(), 0, Math.min(doc.getContent().length(), 300));
        return sb.toString().trim();
    }

    /** 关键词覆盖率（退化方案，当 embedding 不可用时使用）。*/
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

    private float[] safeEmbed(String text) {
        try {
            return embeddingClient.embed(text);
        } catch (Exception e) {
            log.warn("Failed to embed query, will use keyword fallback: {}", e.getMessage());
            return new float[0];
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) return 0.5f;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0f;
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public record AnalysisResult(
            double truthScore,
            double relevanceScore,
            double importanceScore,
            String evidence
    ) {}
}
