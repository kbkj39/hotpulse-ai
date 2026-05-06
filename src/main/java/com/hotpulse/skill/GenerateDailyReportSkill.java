package com.hotpulse.skill;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.entity.Document;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateDailyReportSkill implements Skill<List<Hotspot>, String> {

    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public String name() {
        return "GenerateDailyReportSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofMinutes(2);
    }

    @Override
    public SkillResult<String> execute(List<Hotspot> hotspots) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            // 批量加载关联文档，避免 N+1 查询
            List<Long> docIds = hotspots.stream().map(Hotspot::getDocumentId).toList();
            Map<Long, Document> docMap = documentRepository.findAllById(docIds)
                    .stream().collect(Collectors.toMap(Document::getId, d -> d));

            String hotspotText = hotspots.stream()
                    .map(h -> {
                        Document doc = docMap.get(h.getDocumentId());
                        if (doc == null) return null;
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format("【热度:%.2f】 %s", h.getHotScore(), doc.getTitle()));
                        if (doc.getSourceName() != null) {
                            sb.append("  来源: ").append(doc.getSourceName());
                        }
                        if (doc.getPublishedAt() != null) {
                            sb.append("  时间: ").append(DATE_FMT.format(doc.getPublishedAt()));
                        }
                        if (doc.getSummary() != null && !doc.getSummary().isBlank()) {
                            sb.append("\n  摘要: ").append(doc.getSummary());
                        }
                        try {
                            if (doc.getTagsJson() != null && !doc.getTagsJson().isBlank()) {
                                List<String> tags = objectMapper.readValue(doc.getTagsJson(),
                                        new TypeReference<List<String>>() {});
                                if (!tags.isEmpty()) {
                                    sb.append("\n  标签: ").append(String.join(" ", tags));
                                }
                            }
                        } catch (Exception ignored) {}
                        return sb.toString();
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.joining("\n\n"));

            if (hotspotText.isBlank()) {
                return SkillResult.error("没有可用的热点文档内容", traceId, System.currentTimeMillis() - start);
            }

            String prompt = loadPromptTemplate("prompts/daily_report.st")
                    .replace("{{hotspots}}", hotspotText);

            String report = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return SkillResult.ok(report.trim(), traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("GenerateDailyReportSkill failed", e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }

    private String loadPromptTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
