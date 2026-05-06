package com.hotpulse.job;

import com.hotpulse.entity.DailyReport;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.repository.DailyReportRepository;
import com.hotpulse.repository.HotspotRepository;
import com.hotpulse.skill.GenerateDailyReportSkill;
import com.hotpulse.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportJob {

    private final HotspotRepository hotspotRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GenerateDailyReportSkill generateDailyReportSkill;

    @Scheduled(cron = "0 30 0 * * *") // 每天 00:30 触发
    public void generateDailyReport() {
        LocalDate today = LocalDate.now();
        if (dailyReportRepository.findByReportDate(today).isPresent()) {
            log.info("DailyReport for {} already exists, skipping", today);
            return;
        }

        List<Hotspot> hotspots = hotspotRepository.findAll(PageRequest.of(0, 50)).getContent();
        if (hotspots.isEmpty()) {
            log.warn("No hotspots found for daily report on {}", today);
            return;
        }

        SkillResult<String> result = generateDailyReportSkill.execute(hotspots);
        if (!result.isOk()) {
            log.error("DailyReportJob failed: {}", result.error());
            return;
        }

        DailyReport report = new DailyReport();
        report.setReportDate(today);
        report.setContent(result.data());
        report.setHotspotCount(hotspots.size());
        report.setGeneratedAt(Instant.now());
        dailyReportRepository.save(report);
        log.info("DailyReport generated for {} with {} hotspots", today, hotspots.size());
    }
}
