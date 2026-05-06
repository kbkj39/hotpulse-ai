package com.hotpulse.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 封装 OpenRouter 专用请求头与限流逻辑，
 * 作为 Spring AI 底层适配层被 ChatClient / EmbeddingClient 复用。
 */
@Component
public class OpenRouterClient {

    private final RestClient restClient;

    public OpenRouterClient(
            @Value("${spring.ai.openai.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${spring.ai.openai.api-key:}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", "https://hotpulse.local")
                .defaultHeader("X-Title", "HotPulse AI")
                .build();
    }

    public RestClient getRestClient() {
        return restClient;
    }
}
