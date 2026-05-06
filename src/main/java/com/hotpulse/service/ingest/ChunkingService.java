package com.hotpulse.service.ingest;

import com.hotpulse.entity.Chunk;
import com.hotpulse.repository.ChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChunkingService {

    private static final int TARGET_CHUNK_SIZE = 900; // tokens 近似字符数
    private static final int OVERLAP_SIZE = 128;

    private final ChunkRepository chunkRepository;

    public List<Chunk> chunk(Long documentId, String text) {
        List<String> segments = splitToSegments(text);
        List<Chunk> chunks = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            Chunk chunk = new Chunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i);
            chunk.setText(segments.get(i));
            chunk.setTokenCount(estimateTokenCount(segments.get(i)));
            chunks.add(chunk);
        }

        return chunkRepository.saveAll(chunks);
    }

    private List<String> splitToSegments(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return segments;
        }

        String[] paragraphs = text.split("\n\n+");
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() > TARGET_CHUNK_SIZE) {
                if (!current.isEmpty()) {
                    segments.add(current.toString().trim());
                    // 保留重叠部分
                    String overlap = current.length() > OVERLAP_SIZE
                            ? current.substring(current.length() - OVERLAP_SIZE)
                            : current.toString();
                    current = new StringBuilder(overlap).append("\n\n");
                }
            }
            current.append(paragraph).append("\n\n");
        }
        if (!current.isEmpty()) {
            segments.add(current.toString().trim());
        }

        if (segments.isEmpty() && !text.isBlank()) {
            // Fallback: 按字符硬切分
            for (int i = 0; i < text.length(); i += TARGET_CHUNK_SIZE - OVERLAP_SIZE) {
                int end = Math.min(i + TARGET_CHUNK_SIZE, text.length());
                segments.add(text.substring(i, end));
            }
        }

        return segments;
    }

    private int estimateTokenCount(String text) {
        return text.length() / 4; // 粗略估算：4 字符 ≈ 1 token
    }
}
