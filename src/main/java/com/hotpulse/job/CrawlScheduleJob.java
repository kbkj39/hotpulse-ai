package com.hotpulse.job;

import com.hotpulse.entity.MonitorKeyword;
import com.hotpulse.repository.MonitorKeywordRepository;
import com.hotpulse.service.hotspot.MonitorKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlScheduleJob {

    private final MonitorKeywordRepository monitorKeywordRepository;
    private final MonitorKeywordService monitorKeywordService;

    /**
     * 每 30 分钟扫描一次，对每个已启用且设置了定时间隔的关键词独立判断是否到期。
     * 到期判断：lastCrawledAt 为 null，或 lastCrawledAt + crawlIntervalHours <= now。
     */
    @Scheduled(fixedDelay = 1_800_000) // 30 分钟
    public void scheduleCrawl() {
        List<MonitorKeyword> keywords = monitorKeywordRepository.findByEnabledTrue();
        if (keywords.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (MonitorKeyword kw : keywords) {
            Integer intervalHours = kw.getCrawlIntervalHours();
            if (intervalHours == null || intervalHours <= 0) {
                // 该关键词未设置定时，跳过
                continue;
            }
            if (!isDue(kw, now)) {
                continue;
            }
            try {
                monitorKeywordService.triggerCrawlForKeyword(kw);
                log.info("CrawlScheduleJob: 触发关键词「{}」的定时抓取（间隔 {}h）", kw.getKeyword(), intervalHours);
            } catch (Exception e) {
                log.error("CrawlScheduleJob: 触发关键词「{}」失败", kw.getKeyword(), e);
            }
        }
    }

    private boolean isDue(MonitorKeyword kw, Instant now) {
        if (kw.getLastCrawledAt() == null) return true;
        long intervalSeconds = (long) kw.getCrawlIntervalHours() * 3600;
        return kw.getLastCrawledAt().plusSeconds(intervalSeconds).isBefore(now);
    }
}
