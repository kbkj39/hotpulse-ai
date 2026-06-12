package com.hotpulse.service.hotspot;

import com.hotpulse.dto.HotspotResponse;
import com.hotpulse.dto.TrendPoint;
import com.hotpulse.entity.Document;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.repository.DocumentRepository;
import com.hotpulse.repository.AgentExecutionRepository;
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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotspotService {

    private final HotspotRepository hotspotRepository;
    private final DocumentRepository documentRepository;
    private final AgentExecutionRepository agentExecutionRepository;
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
        resp.setMonitorKeyword(resolveMonitorKeyword(hotspot.getExecutionId()));

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

    private String resolveMonitorKeyword(Long executionId) {
        if (executionId == null) {
            return null;
        }
        return agentExecutionRepository.findById(executionId)
                .map(execution -> parseMonitorKeyword(execution.getQuery()))
                .orElse(null);
    }

    private String parseMonitorKeyword(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String prefix = "监控抓取:";
        if (query.startsWith(prefix)) {
            String keyword = query.substring(prefix.length()).trim();
            return keyword.isBlank() ? null : keyword;
        }
        return null;
    }

    public List<TrendPoint> getTrends(String interval, String monitorKeyword, String tag, String keyword) {
        String normalizedMonitorKeyword = normalize(monitorKeyword);
        String normalizedTag = normalize(tag);
        String normalizedKeyword = normalize(keyword);
        String cacheKey = "trends:%s:%s:%s:%s".formatted(
                interval,
                normalizedMonitorKeyword,
                normalizedTag,
                normalizedKeyword);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> cachedList && !cachedList.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                List<TrendPoint> result = (List<TrendPoint>) cachedList;
                return result;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached trends", e);
            }
        }

        String pgInterval = switch (interval) {
            case "hour" -> "hour";
            case "day" -> "day";
            default -> "hour";
        };

        Instant startTime = "day".equals(pgInterval)
                ? Instant.now().minus(30, ChronoUnit.DAYS)
                : Instant.now().minus(7, ChronoUnit.DAYS);

        List<Object[]> rows = hotspotRepository.getTrendStats(
                pgInterval,
                startTime,
                normalizedMonitorKeyword,
                normalizedTag,
                normalizedKeyword);
        List<TrendPoint> points = rows.stream()
                .map(row -> new TrendPoint(
                        toInstant(row[0]),
                        toLong(row[1]),
                        toDouble(row[2]),
                        toDouble(row[3])
                ))
                .toList();

        try {
            redisTemplate.opsForValue().set(cacheKey, points, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache trends: {}", e.getMessage());
        }

        return points;
    }

    private static Instant toInstant(Object value) {
        return switch (value) {
            case null -> Instant.EPOCH;
            case Instant instant -> instant;
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
            case LocalDateTime localDateTime -> localDateTime.toInstant(ZoneOffset.UTC);
            case Timestamp timestamp -> timestamp.toInstant();
            case Date date -> date.toInstant();
            default -> throw new IllegalArgumentException("Unexpected timestamp type: " + value.getClass());
        };
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
