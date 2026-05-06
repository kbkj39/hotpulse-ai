package com.hotpulse.service.hotspot;

import com.hotpulse.dto.HotspotResponse;
import com.hotpulse.entity.Document;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.repository.DocumentRepository;
import com.hotpulse.repository.HotspotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotspotService {

    private final HotspotRepository hotspotRepository;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "hotspots:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public Map<String, Object> getHotspots(String sort, int page, int limit, String tag, String keyword) {
        String cacheKey = CACHE_PREFIX + sort + ":" + page + ":" + limit + ":" + tag;

        Page<Hotspot> hotspotPage = switch (sort) {
            case "importance" -> hotspotRepository.findByTagAndKeywordOrderByImportanceScore(tag, normalize(keyword), PageRequest.of(page - 1, limit));
            case "relevance"  -> hotspotRepository.findByTagAndKeywordOrderByRelevanceScore(tag, normalize(keyword), PageRequest.of(page - 1, limit));
            case "time"       -> hotspotRepository.findByTagAndKeywordOrderByCreatedAtDesc(tag, normalize(keyword), PageRequest.of(page - 1, limit));
            default           -> hotspotRepository.findByTagAndKeywordOrderByHotScore(tag, normalize(keyword), PageRequest.of(page - 1, limit));
        };

        List<HotspotResponse> items = hotspotPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return Map.of(
                "total", hotspotPage.getTotalElements(),
                "items", items
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public HotspotResponse getHotspotDetail(Long id) {
        Hotspot hotspot = hotspotRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("热点不存在: " + id));
        HotspotResponse resp = toResponse(hotspot);
        documentRepository.findById(hotspot.getDocumentId()).ifPresent(doc -> {
            resp.setFullText(doc.getContent());
            resp.setExecutionId(hotspot.getExecutionId() != null ? hotspot.getExecutionId().toString() : null);
        });
        return resp;
    }

    public HotspotResponse toResponse(Hotspot hotspot) {
        HotspotResponse resp = new HotspotResponse();
        resp.setId(hotspot.getId());
        resp.setTruthScore(hotspot.getTruthScore());
        resp.setRelevanceScore(hotspot.getRelevanceScore());
        resp.setImportanceScore(hotspot.getImportanceScore());
        resp.setHotScore(hotspot.getHotScore());
        resp.setAnalysisEvidence(hotspot.getAnalysisEvidence());

        try {
            if (hotspot.getTags() != null && !hotspot.getTags().isBlank()) {
                List<String> tags = objectMapper.readValue(hotspot.getTags(), new TypeReference<List<String>>() {});
                resp.setTags(tags);
            }
        } catch (Exception e) {
            resp.setTags(List.of());
        }

        documentRepository.findById(hotspot.getDocumentId()).ifPresent(doc -> {
            resp.setTitle(doc.getTitle());
            resp.setSummary(doc.getSummary());
            resp.setPublishedAt(doc.getPublishedAt());
            resp.setUrl(doc.getSourceUrl());
            resp.setSource(doc.getSourceName());
        });

        return resp;
    }
}
