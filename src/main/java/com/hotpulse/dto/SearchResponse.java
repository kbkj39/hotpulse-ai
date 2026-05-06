package com.hotpulse.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class SearchResponse {
    private String answer;
    private List<Evidence> evidences;

    @Getter
    @Setter
    public static class Evidence {
        private Long documentId;
        private String snippet;
        private Double score;
        private String url;
        private Instant publishedAt;
    }
}
