package com.hotpulse.service.crawler;

import com.hotpulse.entity.Source;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
public class JsoupFetcher {

    private static final int MAX_CONCURRENT_PER_DOMAIN = 2;
    private static final int REQUEST_DELAY_MS = 1000;

    private final java.util.concurrent.ConcurrentHashMap<String, Semaphore> domainSemaphores =
            new java.util.concurrent.ConcurrentHashMap<>();

    public List<CandidateItem> fetchCandidates(Source source, List<String> keywords) {
        Semaphore semaphore = domainSemaphores.computeIfAbsent(
                extractDomain(source.getBaseUrl()),
                k -> new Semaphore(MAX_CONCURRENT_PER_DOMAIN));
        try {
            semaphore.acquire();
            Thread.sleep(REQUEST_DELAY_MS);

            Document doc = Jsoup.connect(source.getBaseUrl())
                    .userAgent("Mozilla/5.0 (compatible; HotPulseBot/1.0)")
                    .timeout(15000)
                    .get();

            Elements links = doc.select("a[href]");
            List<CandidateItem> candidates = new ArrayList<>();
            for (Element link : links) {
                String title = link.text().trim();
                String href = link.absUrl("href");
                if (!title.isEmpty() && !href.isEmpty() && matchesKeywords(title, keywords)) {
                    candidates.add(new CandidateItem(title, href, Instant.now()));
                }
            }
            return candidates;
        } catch (Exception e) {
            log.error("Jsoup fetch candidates failed for source: {}", source.getName(), e);
            return Collections.emptyList();
        } finally {
            semaphore.release();
        }
    }

    public String fetchContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; HotPulseBot/1.0)")
                    .timeout(15000)
                    .get();
            return doc.select("article, .article-body, .post-content, main, body").first() != null
                    ? doc.select("article, .article-body, .post-content, main, body").first().text()
                    : doc.text();
        } catch (Exception e) {
            log.error("Jsoup fetch content failed for url: {}", url, e);
            return "";
        }
    }

    private boolean matchesKeywords(String title, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return true;
        String lower = title.toLowerCase();
        return keywords.stream().anyMatch(kw -> lower.contains(kw.toLowerCase()));
    }

    private String extractDomain(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
