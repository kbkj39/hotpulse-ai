package com.hotpulse.job;

import com.hotpulse.entity.DailyReport;
import com.hotpulse.entity.DailyReportStatus;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.dto.DailyReportRequest;
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
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportJob {

    private final HotspotRepository hotspotRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GenerateDailyReportSkill generateDailyReportSkill;

    // 终态或进行中：直接跳过本次 cron。
    //   READY/EMPTY - 已有结论，无需重跑
    //   GENERATING  - 另一实例正在跑（避免并发）或上次崩溃遗留，避免重入
    private static final Set<DailyReportStatus> SKIP_STATUSES = EnumSet.of(
            DailyReportStatus.READY,
            DailyReportStatus.EMPTY,
            DailyReportStatus.GENERATING);

    @Scheduled(cron = "0 30 0 * * *") // 每天 00:30 触发
    public void generateDailyReport() {
        // 00:30 触发时把"昨天"作为报告日。例如 2026-06-12 00:30 跑，生成 2026-06-11 的日报，
        // 避免把当天尚未积累的热点混进报告里。
        LocalDate reportDate = LocalDate.now().minusDays(1);
        ZoneId zone = ZoneId.systemDefault();
        Instant startOfDay = reportDate.atStartOfDay(zone).toInstant();
        Instant endOfDay = reportDate.plusDays(1).atStartOfDay(zone).toInstant();

        DailyReport report = dailyReportRepository.findByReportDate(reportDate)
                .orElseGet(() -> {
                    DailyReport r = new DailyReport();
                    r.setReportDate(reportDate);
                    r.setStatus(DailyReportStatus.PENDING);
                    return dailyReportRepository.save(r);
                });

        if (SKIP_STATUSES.contains(report.getStatus())) {
            log.info("DailyReport for {} in status {}, skipping", reportDate, report.getStatus());
            return;
        }

        // 进入生成态
        report.setStatus(DailyReportStatus.GENERATING);
        report.setErrorMessage(null);
        dailyReportRepository.save(report);

        List<Hotspot> hotspots = hotspotRepository
                .findByCreatedAtBetweenOrderByImportanceScoreDesc(
                        startOfDay, endOfDay, PageRequest.of(0, 50))
                .getContent();
        if (hotspots.isEmpty()) {
            report.setStatus(DailyReportStatus.EMPTY);
            dailyReportRepository.save(report);
            log.warn("No hotspots found for daily report on {}", reportDate);
            return;
        }

        SkillResult<String> result = generateDailyReportSkill.execute(
                new DailyReportRequest(hotspots, reportDate));
        if (!result.isOk()) {
            report.setStatus(DailyReportStatus.FAILED);
            report.setErrorMessage(result.error());
            dailyReportRepository.save(report);
            log.error("DailyReportJob failed: {}", result.error());
            return;
        }

        report.setContent(result.data());
        report.setHotspotCount(hotspots.size());
        report.setGeneratedAt(Instant.now());
        report.setStatus(DailyReportStatus.READY);
        dailyReportRepository.save(report);
        log.info("DailyReport generated for {} with {} hotspots", reportDate, hotspots.size());
    }
}
