package com.hotpulse.service.crawler;

import com.hotpulse.entity.Source;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RssFetcher {

    public List<CandidateItem> fetch(Source source, List<String> keywords) {
        try {
            SyndFeed feed = fetchFeed(source.getBaseUrl());

            List<CandidateItem> matched = feed.getEntries().stream()
                    .filter(entry -> matchesKeywords(entry, keywords))
                    .map(entry -> toCandidateItem(entry))
                    .collect(Collectors.toList());

            // 关键词匹配为空时，回退到最近 20 条，让 AnalyzerAgent 做语义相关性判断
            if (matched.isEmpty()) {
                log.info("RSS keywords matched 0 entries for [{}], falling back to top 20 recent entries", source.getName());
                return feed.getEntries().stream()
                        .limit(20)
                        .map(entry -> toCandidateItem(entry))
                        .collect(Collectors.toList());
            }

            return matched;
        } catch (Exception e) {
            log.error("RSS fetch failed for source: {}", source.getName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 通过 HTTP 获取 RSS 原始内容，剥离 DOCTYPE 声明后再解析。
     * <p>
     * 部分 RSS（如澎湃新闻）包含 {@code <!DOCTYPE>} 声明，直接用 XmlReader 会触发
     * JDOM2 的 XXE 安全拦截（disallow-doctype-decl=true）。
     * 移除 DOCTYPE 既解决了解析异常，也消除了 XXE 攻击风险。
     */
    private SyndFeed fetchFeed(String feedUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(feedUrl).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 HotPulse/1.0");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setInstanceFollowRedirects(true);
        try {
            String charset = extractCharset(conn.getContentType());
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                String content = new String(bytes, charset != null ? charset : StandardCharsets.UTF_8.name());
                // 移除 DOCTYPE 声明（含可选的内部子集 [...]），防止 XXE 并通过解析器安全检查
                content = content.replaceAll("(?i)<!DOCTYPE[^\\[>]*(?:\\[[^\\]]*])?\\s*>", "");
                return new SyndFeedInput().build(new StringReader(content));
            }
        } finally {
            conn.disconnect();
        }
    }

    private String extractCharset(String contentType) {
        if (contentType == null) return null;
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.toLowerCase().startsWith("charset=")) {
                return part.substring("charset=".length()).trim().replace("\"", "");
            }
        }
        return null;
    }

    private CandidateItem toCandidateItem(SyndEntry entry) {
        Instant publishedAt = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant()
                : Instant.now();
        return new CandidateItem(entry.getTitle(), entry.getLink(), publishedAt);
    }

    private boolean matchesKeywords(SyndEntry entry, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        String title = entry.getTitle() != null ? entry.getTitle().toLowerCase() : "";
        // 同时检查 description（摘要），扩大匹配范围
        String description = "";
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            description = entry.getDescription().getValue().toLowerCase();
        }
        String combined = title + " " + description;
        return keywords.stream().anyMatch(kw -> combined.contains(kw.toLowerCase()));
    }
}
