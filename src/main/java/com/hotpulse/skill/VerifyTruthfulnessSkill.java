package com.hotpulse.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.dto.SearchResponse;
import com.hotpulse.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerifyTruthfulnessSkill implements Skill<VerifyTruthfulnessSkill.Input, VerifyTruthfulnessSkill.Result> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public record Input(Document document, List<SearchResponse.Evidence> evidences) {}

    public record Result(double score, String evidence) {}

    /** 批量输入：多篇文档共享同一组 RAG 证据，一次 LLM 调用返回所有分值。*/
    public record BatchInput(List<Document> documents, List<SearchResponse.Evidence> evidences) {}

    public record BatchResult(int index, double score, String reason) {}

    @Override
    public String name() {
        return "VerifyTruthfulnessSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(30);
    }

    @Override
    public SkillResult<Result> execute(Input input) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            String evidenceText = input.evidences().stream()
                    .map(e -> "- " + e.getSnippet() + " [来源: " + e.getUrl() + "]")
                    .collect(Collectors.joining("\n"));

            String prompt = loadPromptTemplate("prompts/verify_truthfulness.st")
                    .replace("{{title}}", input.document().getTitle())
                    .replace("{{content}}", truncate(input.document().getContent(), 2000))
                    .replace("{{evidences}}", evidenceText);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            Result result = parseResult(response);
            return SkillResult.ok(result, traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("VerifyTruthfulnessSkill failed", e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }

    /**
     * 批量评估：将多篇文章和共享证据一次性发给 LLM，返回每篇的评分结果。
     * 相较于逐篇调用，节省 (N-1) 次 LLM 往返，速度提升显著。
     */
    public List<BatchResult> executeBatch(BatchInput input) {
        List<Document> docs = input.documents();
        List<BatchResult> fallback = buildFallback(docs.size());
        if (docs.isEmpty()) return fallback;

        try {
            String evidenceText = input.evidences().isEmpty()
                    ? "（无参考证据）"
                    : input.evidences().stream()
                            .limit(10)
                            .map(e -> "- " + truncate(e.getSnippet(), 200) + " [来源: " + e.getUrl() + "]")
                            .collect(Collectors.joining("\n"));

            StringBuilder articles = new StringBuilder();
            for (int i = 0; i < docs.size(); i++) {
                Document d = docs.get(i);
                articles.append("[").append(i).append("] 标题: ").append(d.getTitle()).append("\n");
                articles.append("    内容摘要: ").append(truncate(
                        d.getSummary() != null ? d.getSummary() : d.getContent(), 300)).append("\n\n");
            }

            String prompt = loadPromptTemplate("prompts/verify_truthfulness_batch.st")
                    .replace("{{evidences}}", evidenceText)
                    .replace("{{articles}}", articles.toString());

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseBatchResult(response, docs.size());
        } catch (Exception e) {
            log.error("VerifyTruthfulnessSkill.executeBatch failed", e);
            return fallback;
        }
    }

    private List<BatchResult> parseBatchResult(String response, int size) {
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start < 0 || end < 0 || end <= start) return buildFallback(size);

            String json = response.substring(start, end + 1);
            JsonNode array = objectMapper.readTree(json);
            List<BatchResult> results = buildFallback(size);
            for (JsonNode node : array) {
                int idx = node.path("index").asInt(-1);
                if (idx >= 0 && idx < size) {
                    double score = node.path("score").asDouble(0.5);
                    String reason = node.path("reason").asText("");
                    results.set(idx, new BatchResult(idx, Math.max(0, Math.min(1, score)), reason));
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Failed to parse batch truth result: {}", e.getMessage());
            return buildFallback(size);
        }
    }

    private List<BatchResult> buildFallback(int size) {
        List<BatchResult> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new BatchResult(i, 0.5, "评估失败，使用默认分值"));
        }
        return list;
    }

    private Result parseResult(String response) {
        try {
            double score = 0.5;
            if (response.contains("score:") || response.contains("分值:")) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.toLowerCase().contains("score:") || line.contains("分值:")) {
                        String numStr = line.replaceAll("[^0-9.]", "");
                        if (!numStr.isEmpty()) {
                            score = Double.parseDouble(numStr);
                            if (score > 1.0) score = score / 100.0;
                        }
                    }
                }
            }
            return new Result(score, response);
        } catch (Exception e) {
            return new Result(0.5, response);
        }
    }

    private String loadPromptTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
