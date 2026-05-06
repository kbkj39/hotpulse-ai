package com.hotpulse.service.iwencai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.dto.SearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 同花顺问财 SkillHub 客户端服务。
 *
 * <p>调用 POST https://openapi.iwencai.com/v1/query2data 接口，
 * 将返回的金融结构化数据转为 {@link SearchResponse.Evidence} 列表，
 * 供 {@link com.hotpulse.service.agent.AnalyzerAgent} 进行真实性与相关性评估。
 *
 * <p>API 格式参考：同花顺「基本资料查询」Skill SKILL.md。
 */
@Slf4j
@Service
public class IwencaiSkillService {

    private static final String QUERY2DATA_PATH = "/v1/query2data";
    /** 外部权威数据源的默认证据相关性分值 */
    private static final double DEFAULT_EVIDENCE_SCORE = 0.85;
    /** 单次查询返回条数上限 */
    private static final int DEFAULT_LIMIT = 10;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IwencaiSkillService(
            @Value("${iwencai.base-url:https://openapi.iwencai.com}") String baseUrl,
            @Value("${iwencai.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 查询同花顺金融知识库，返回结构化证据列表。
     *
     * @param query 自然语言查询词（如"奶龙相关上市公司"、"加密货币最新热点"等）
     * @param limit 返回条数，最大 10
     * @return 证据列表；若接口失败则返回空列表（不抛异常，降级处理）
     */
    public List<SearchResponse.Evidence> query(String query, int limit) {
        List<SearchResponse.Evidence> evidences = new ArrayList<>();
        try {
            Map<String, String> payload = Map.of(
                    "query", query,
                    "page", "1",
                    "limit", String.valueOf(Math.min(limit, DEFAULT_LIMIT)),
                    "is_cache", "1",
                    "expand_index", "true"
            );

            String responseBody = restClient.post()
                    .uri(QUERY2DATA_PATH)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            evidences = parseResponse(responseBody, query);
            log.debug("IwencaiSkillService query='{}' -> {} evidences", query, evidences.size());
        } catch (Exception e) {
            log.warn("IwencaiSkillService query failed for '{}': {}", query, e.getMessage());
        }
        return evidences;
    }

    // ─── private helpers ───────────────────────────────────────────────────

    private List<SearchResponse.Evidence> parseResponse(String body, String query) {
        List<SearchResponse.Evidence> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            int statusCode = root.path("status_code").asInt(-1);
            if (statusCode != 0) {
                log.warn("Iwencai API error status_code={} msg={}",
                        statusCode, root.path("status_msg").asText());
                return result;
            }

            JsonNode datas = root.path("datas");
            if (!datas.isArray()) return result;

            for (JsonNode item : datas) {
                SearchResponse.Evidence ev = new SearchResponse.Evidence();
                ev.setScore(DEFAULT_EVIDENCE_SCORE);
                ev.setUrl("https://www.iwencai.com/unifiedwap/chat");
                ev.setSnippet(formatDataItem(item, query));
                // documentId / publishedAt 不适用于外部数据，保持 null
                result.add(ev);
            }
        } catch (Exception e) {
            log.warn("Failed to parse Iwencai response: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 将同花顺返回的单条结构化数据记录格式化为可读摘要文本，
     * 供 LLM 真实性评估 prompt 使用。
     */
    private String formatDataItem(JsonNode item, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("[同花顺数据] 查询: ").append(query).append(" → ");
        Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
        int count = 0;
        while (fields.hasNext() && count < 8) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String value = entry.getValue().asText("");
            if (!value.isBlank() && !"null".equals(value)) {
                sb.append(entry.getKey()).append(": ").append(value).append("; ");
                count++;
            }
        }
        return sb.toString().stripTrailing();
    }
}
