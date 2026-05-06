package com.hotpulse.service.rag;

import com.hotpulse.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public List<SearchResponse.Evidence> search(String query, int topK) {
        try {
            float[] queryVector = embeddingModel.embed(query);
            String vectorStr = toVectorString(queryVector);

            String sql = """
                    SELECT c.id AS chunk_id, c.text AS snippet, 
                           d.id AS document_id, d.source_url AS url, d.published_at,
                           1 - (e.embedding <=> ?::vector) AS score
                    FROM embeddings e
                    JOIN chunks c ON e.chunk_id = c.id
                    JOIN documents d ON c.document_id = d.id
                    ORDER BY e.embedding <=> ?::vector
                    LIMIT ?
                    """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                SearchResponse.Evidence evidence = new SearchResponse.Evidence();
                evidence.setDocumentId(rs.getLong("document_id"));
                evidence.setSnippet(rs.getString("snippet"));
                evidence.setScore(rs.getDouble("score"));
                evidence.setUrl(rs.getString("url"));
                Timestamp publishedAt = rs.getTimestamp("published_at");
                if (publishedAt != null) {
                    evidence.setPublishedAt(publishedAt.toInstant());
                }
                return evidence;
            }, vectorStr, vectorStr, topK);
        } catch (Exception e) {
            log.error("Vector search failed for query: {}", query, e);
            return new ArrayList<>();
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
