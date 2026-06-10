package com.hotpulse.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TrendPoint {
    private Instant timestamp;
    private Long count;
    private Double avgHotScore;
    private Double avgImportanceScore;

    public TrendPoint() {}

    public TrendPoint(Instant timestamp, Long count, Double avgHotScore, Double avgImportanceScore) {
        this.timestamp = timestamp;
        this.count = count;
        this.avgHotScore = avgHotScore;
        this.avgImportanceScore = avgImportanceScore;
    }
}
