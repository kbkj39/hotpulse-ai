package com.hotpulse.service.crawler;

import com.hotpulse.entity.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PageFetcher {

    private final RssFetcher rssFetcher;
    private final JsoupFetcher jsoupFetcher;
    private final ApiFetcher apiFetcher;

    /**
     * 根据信息源类型路由到对应的 Fetcher，返回候选 URL 列表
     */
    public List<CandidateItem> fetchCandidates(Source source, List<String> keywords) {
        return switch (source.getType()) {
            case "RSS" -> rssFetcher.fetch(source, keywords);
            case "API" -> apiFetcher.fetch(source, keywords);
            default   -> jsoupFetcher.fetchCandidates(source, keywords);
        };
    }

    /**
     * 抓取单个 URL 的原始内容
     */
    public String fetchRawContent(String url, Source source) {
        if ("RSS".equals(source.getType())) {
            return "";
        }
        return jsoupFetcher.fetchContent(url);
    }
}
