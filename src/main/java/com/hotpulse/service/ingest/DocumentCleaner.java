package com.hotpulse.service.ingest;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class DocumentCleaner {

    public String clean(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        // 使用 Jsoup 去除脚本、样式、广告等
        org.jsoup.nodes.Document doc = Jsoup.parse(rawHtml);
        doc.select("script, style, nav, header, footer, aside, .ad, .advertisement").remove();

        String text = doc.select("article, .article-body, .post-content, main").text();
        if (text.isBlank()) {
            text = doc.body() != null ? doc.body().text() : doc.text();
        }
        return text.trim();
    }

    public String extractTitle(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        org.jsoup.nodes.Document doc = Jsoup.parse(rawHtml);
        String title = doc.title();
        if (title == null || title.isBlank()) {
            org.jsoup.nodes.Element h1 = doc.selectFirst("h1");
            title = h1 != null ? h1.text() : "";
        }
        return title.trim();
    }
}
