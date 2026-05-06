package com.hotpulse.skill;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class SummarizeSkill implements Skill<String, String> {

    private final ChatClient chatClient;

    @Override
    public String name() {
        return "SummarizeSkill";
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(20);
    }

    @Override
    public SkillResult<String> execute(String text) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        try {
            String prompt = loadPromptTemplate("prompts/summarize.st")
                    .replace("{{text}}", truncate(text, 3000));

            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return SkillResult.ok(summary.trim(), traceId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("SummarizeSkill failed", e);
            return SkillResult.error(e.getMessage(), traceId, System.currentTimeMillis() - start);
        }
    }

    private String loadPromptTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
