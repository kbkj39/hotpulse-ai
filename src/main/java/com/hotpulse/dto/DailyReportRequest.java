package com.hotpulse.dto;

import com.hotpulse.entity.Hotspot;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 生成每日日报的输入包装：同时携带数据源（hotspots）与报告日期，
// 让 Skill 内部可以显式把日期注入 prompt 模板，避免依赖 LLM 自行推断"今日"。
@Getter
public class DailyReportRequest {
    private final List<Hotspot> hotspots;
    private final LocalDate reportDate;

    public DailyReportRequest(List<Hotspot> hotspots, LocalDate reportDate) {
        this.hotspots = hotspots;
        this.reportDate = reportDate;
    }
}
