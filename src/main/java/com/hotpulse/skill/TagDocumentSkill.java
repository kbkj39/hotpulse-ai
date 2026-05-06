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
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagDocumentSkill implements Skill<String, List<String>> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "TagDocumentSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(15);
    }

    @Override
    public SkillResult<List<String>> execute(String titleAndContent) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            String prompt = loadPromptTemplate("prompts/tag_document.st")
                    .replace("{{text}}", truncate(titleAndContent, 2000));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            List<String> tags = parseTagsFromResponse(response);
            return SkillResult.ok(tags, traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("TagDocumentSkill failed", e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }

    private List<String> parseTagsFromResponse(String response) {
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start >= 0 && end > start) {
                String json = response.substring(start, end + 1);
                return objectMapper.readValue(json, new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse tags JSON, returning empty list");
        }
        return Collections.emptyList();
    }

    private String loadPromptTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
