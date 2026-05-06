package com.hotpulse.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class HotspotResponse {
    private Long id;
    private String title;
    private String summary;
    private String source;
    private Instant publishedAt;
    private Double hotScore;
    private Double truthScore;
    private Double relevanceScore;
    private Double importanceScore;
    private String analysisEvidence;
    private List<String> tags;
    private String url;
    private String fullText;
    private String executionId;
}
