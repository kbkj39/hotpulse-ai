package com.hotpulse.skill;

import com.hotpulse.entity.Document;
import com.hotpulse.service.ingest.IngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class FetchAndIngestSkill implements Skill<FetchAndIngestSkill.Input, Document> {

    private final IngestService ingestService;

    public record Input(String url, Long sourceId) {}

    @Override
    public String name() {
        return "FetchAndIngestSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(60);
    }

    @Override
    public SkillResult<Document> execute(Input input) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            Document doc = ingestService.ingest(input.url(), input.sourceId());
            return SkillResult.ok(doc, traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("FetchAndIngestSkill failed for url: {}", input.url(), e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }
}
