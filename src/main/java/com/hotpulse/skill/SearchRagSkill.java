package com.hotpulse.skill;

import com.hotpulse.dto.SearchResponse;
import com.hotpulse.service.rag.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchRagSkill implements Skill<SearchRagSkill.Input, List<SearchResponse.Evidence>> {

    private final VectorSearchService vectorSearchService;

    public record Input(String query, int topK) {}

    @Override
    public String name() {
        return "SearchRagSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(10);
    }

    @Override
    public SkillResult<List<SearchResponse.Evidence>> execute(Input input) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            List<SearchResponse.Evidence> evidences = vectorSearchService.search(input.query(), input.topK());
            return SkillResult.ok(evidences, traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("SearchRagSkill failed for query: {}", input.query(), e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }
}
