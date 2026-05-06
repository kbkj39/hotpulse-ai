package com.hotpulse.service.rag;

import com.hotpulse.entity.Chunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public void embedChunks(List<Chunk> chunks, String sourceUrl, Instant publishedAt) {
        for (Chunk chunk : chunks) {
            try {
                float[] vector = embeddingModel.embed(chunk.getText());
                String vectorStr = toVectorString(vector);
                jdbcTemplate.update(
                        "INSERT INTO embeddings (chunk_id, model, embedding) VALUES (?, ?, ?::vector)",
                        chunk.getId(),
                        "text-embedding-3-large",
                        vectorStr
                );
            } catch (Exception e) {
                log.error("Failed to embed chunk id={}", chunk.getId(), e);
            }
        }
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
