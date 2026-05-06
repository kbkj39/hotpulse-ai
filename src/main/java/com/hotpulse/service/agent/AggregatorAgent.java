package com.hotpulse.service.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.common.AgentConstants;
import com.hotpulse.dto.HotspotResponse;
import com.hotpulse.entity.Document;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.repository.DocumentRepository;
import com.hotpulse.repository.HotspotRepository;
import com.hotpulse.service.hotspot.HotspotScoringService;
import com.hotpulse.service.hotspot.HotspotSocketService;
import com.hotpulse.skill.SummarizeSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorAgent {

    private final HotspotRepository hotspotRepository;
    private final DocumentRepository documentRepository;
    private final HotspotScoringService hotspotScoringService;
    private final SummarizeSkill summarizeSkill;
    private final HotspotSocketService hotspotSocketService;
    private final AgentExecutionTracker tracker;
    private final ObjectMapper objectMapper;

    public List<Hotspot> aggregate(Long executionId,
                                    List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> analysisResults) {
        tracker.recordStep(executionId, AgentConstants.AGGREGATOR_AGENT, AgentConstants.STATUS_RUNNING,
                "正在聚合与去重 " + analysisResults.size() + " 条分析结果...", null);
        try {
            // 语义去重（基于简单标题相似度去重）
            List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> deduped = deduplicate(analysisResults);

            List<Hotspot> hotspots = new ArrayList<>();
            for (Map.Entry<Document, AnalyzerAgent.AnalysisResult> entry : deduped) {
                Document doc = entry.getKey();
                AnalyzerAgent.AnalysisResult result = entry.getValue();

                long ageHours = doc.getPublishedAt() != null
                        ? ChronoUnit.HOURS.between(doc.getPublishedAt(), Instant.now())
                        : 24;
                double hotScore = hotspotScoringService.computeHotScore(
                        result.importanceScore(), ageHours, 1.0);

                // 生成摘要（如果尚未有摘要）
                if (doc.getSummary() == null || doc.getSummary().isBlank()) {
                    var summaryResult = summarizeSkill.execute(doc.getContent());
                    if (summaryResult.isOk()) {
                        doc.setSummary(summaryResult.data());
                        documentRepository.save(doc);
                    }
                }

                // 使用 IngestService 在入库时生成的标签（避免重复调用大模型）
                String tagsJson = doc.getTagsJson() != null ? doc.getTagsJson() : "[]";

                Hotspot hotspot = new Hotspot();
                hotspot.setDocumentId(doc.getId());
                hotspot.setExecutionId(executionId);
                hotspot.setTruthScore(result.truthScore());
                hotspot.setRelevanceScore(result.relevanceScore());
                hotspot.setImportanceScore(result.importanceScore());
                hotspot.setHotScore(hotScore);
                hotspot.setAnalysisEvidence(result.evidence());
                hotspot.setTags(tagsJson);

                Hotspot saved = hotspotRepository.save(hotspot);
                hotspots.add(saved);

                // 通过 Socket.IO 实时推送新热点到前端列表
                hotspotSocketService.pushNewHotspot(buildSocketResponse(saved, doc, tagsJson));
            }

            // 按 hotScore 排序
            hotspots.sort(Comparator.comparingDouble(Hotspot::getHotScore).reversed());

            tracker.recordStep(executionId, AgentConstants.AGGREGATOR_AGENT, AgentConstants.STATUS_DONE,
                    "聚合完成，生成 " + hotspots.size() + " 条热点", null);
            return hotspots;
        } catch (Exception e) {
            log.error("AggregatorAgent failed", e);
            tracker.recordStep(executionId, AgentConstants.AGGREGATOR_AGENT, AgentConstants.STATUS_FAILED,
                    "聚合异常: " + e.getMessage(), null);
            return Collections.emptyList();
        }
    }

    private HotspotResponse buildSocketResponse(Hotspot hotspot, Document doc, String tagsJson) {
        HotspotResponse resp = new HotspotResponse();
        resp.setId(hotspot.getId());
        resp.setTitle(doc.getTitle());
        resp.setSummary(doc.getSummary());
        resp.setSource(doc.getSourceName());
        resp.setPublishedAt(doc.getPublishedAt());
        resp.setUrl(doc.getSourceUrl());
        resp.setTruthScore(hotspot.getTruthScore());
        resp.setRelevanceScore(hotspot.getRelevanceScore());
        resp.setImportanceScore(hotspot.getImportanceScore());
        resp.setHotScore(hotspot.getHotScore());
        resp.setAnalysisEvidence(hotspot.getAnalysisEvidence());
        try {
            if (tagsJson != null && !tagsJson.isBlank()) {
                resp.setTags(objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) {
            resp.setTags(List.of());
        }
        return resp;
    }

    private List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> deduplicate(
            List<Map.Entry<Document, AnalyzerAgent.AnalysisResult>> entries) {
        Set<String> seenTitles = new HashSet<>();
        return entries.stream()
                .filter(e -> {
                    String normalizedTitle = normalizeTitle(e.getKey().getTitle());
                    return seenTitles.add(normalizedTitle);
                })
                .collect(Collectors.toList());
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        String normalized = title.toLowerCase().replaceAll("[\\s\\p{Punct}]", "");
        return normalized.substring(0, Math.min(normalized.length(), 30));
    }
}
