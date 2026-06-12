package com.hotpulse.job;

import com.hotpulse.service.hotspot.DailyReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportJob {

    private final DailyReportGenerator dailyReportGenerator;

    @Scheduled(cron = "0 30 0 * * *") // 每天 00:30 触发
    public void generateDailyReport() {
        // 00:30 触发时生成「昨天」的日报，避免当天热点尚未积累完毕。
        LocalDate reportDate = LocalDate.now().minusDays(1);
        log.info("DailyReportJob scheduling async generation for {}", reportDate);
        dailyReportGenerator.generateAsync(reportDate);
    }
}
