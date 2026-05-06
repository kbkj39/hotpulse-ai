package com.hotpulse.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装 Embedding API 调用，提供批量请求优化，返回 float[] 向量。
 * 对外屏蔽 Spring AI EmbeddingModel 的底层细节。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingModel embeddingModel;

    /**
     * 将单个文本转为向量。
     */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * 批量将文本列表转为向量列表（逐条调用，保留顺序）。
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            try {
                results.add(embeddingModel.embed(text));
            } catch (Exception e) {
                log.error("Failed to embed text snippet: {}", text.substring(0, Math.min(50, text.length())), e);
                results.add(new float[0]);
            }
        }
        return results;
    }
}
