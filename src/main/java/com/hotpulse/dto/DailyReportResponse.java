package com.hotpulse.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class DailyReportResponse {
    private String reportDate;
    private String content;
    private Integer hotspotCount;
    private Instant generatedAt;
}
