package com.hotpulse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitorKeywordRequest {
    private String keyword;
    private Boolean enabled;
    /** 是否立即触发一次搜索抓取，默认 false。*/
    private Boolean triggerNow;
    /** 定时爬取间隔（小时）。null 或 0 表示不定时。*/
    private Integer crawlIntervalHours;
}
