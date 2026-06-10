package com.hotpulse.skill;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpandKeywordsSkill implements Skill<ExpandKeywordsSkill.Input, List<String>> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public record Input(List<String> keywords, int maxKeywords) {}

    @Override
    public String name() {
        return "ExpandKeywordsSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(20);
    }

    @Override
    public SkillResult<List<String>> execute(Input input) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            String keywordsJson = objectMapper.writeValueAsString(input.keywords());
            String prompt = loadPromptTemplate("prompts/expand_keywords.st")
                    .replace("{{keywords}}", keywordsJson)
                    .replace("{{maxKeywords}}", String.valueOf(input.maxKeywords()));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String json = extractJsonArray(response);
            List<String> expanded = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return SkillResult.ok(expanded, traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("ExpandKeywordsSkill failed for keywords: {}", input.keywords(), e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }

    private String loadPromptTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
