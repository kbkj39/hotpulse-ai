package com.hotpulse.service.crawler;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CandidateItem {
    private String title;
    private String url;
    private Instant publishedAt;
    private String snippet;
    private Long sourceId;
    private String sourceName;

    public CandidateItem(String title, String url, Instant publishedAt) {
        this.title = title;
        this.url = url;
        this.publishedAt = publishedAt;
    }
}
