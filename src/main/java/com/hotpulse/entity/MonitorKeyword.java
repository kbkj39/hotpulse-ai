package com.hotpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "monitor_keywords")
public class MonitorKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keyword;

    @Column(nullable = false)
    private Boolean enabled = true;

    /** 定时爬取间隔（小时）。null 或 0 表示不定时，仅手动/立即触发。*/
    @Column(name = "crawl_interval_hours")
    private Integer crawlIntervalHours;

    /** 上次触发爬取的时间，用于计算下次执行时机。*/
    @Column(name = "last_crawled_at")
    private Instant lastCrawledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
