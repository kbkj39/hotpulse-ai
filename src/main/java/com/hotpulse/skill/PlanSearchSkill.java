package com.hotpulse.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.dto.TaskPlanDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanSearchSkill implements Skill<String, TaskPlanDto> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "PlanSearchSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(30);
    }

    @Override
    public SkillResult<TaskPlanDto> execute(String query) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            String prompt = loadPromptTemplate("prompts/plan_search.st")
                    .replace("{{query}}", query);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String json = extractJson(response);
            TaskPlanDto plan = objectMapper.readValue(json, TaskPlanDto.class);
            return SkillResult.ok(plan, traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("PlanSearchSkill failed for query: {}", query, e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }

    private String loadPromptTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
