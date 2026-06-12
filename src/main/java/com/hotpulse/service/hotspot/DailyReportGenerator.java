package com.hotpulse.service.hotspot;

import com.hotpulse.dto.DailyReportRequest;
import com.hotpulse.entity.DailyReport;
import com.hotpulse.entity.DailyReportStatus;
import com.hotpulse.entity.Hotspot;
import com.hotpulse.repository.DailyReportRepository;
import com.hotpulse.repository.HotspotRepository;
import com.hotpulse.skill.GenerateDailyReportSkill;
import com.hotpulse.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
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
public class DailyReportGenerator {

    private final HotspotRepository hotspotRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GenerateDailyReportSkill generateDailyReportSkill;

    private static final Set<DailyReportStatus> SKIP_STATUSES = EnumSet.of(
            DailyReportStatus.READY,
            DailyReportStatus.EMPTY,
            DailyReportStatus.GENERATING);

    @Async
    public void generateAsync(LocalDate reportDate) {
        try {
            doGenerate(reportDate);
        } catch (Exception e) {
            log.error("DailyReportGenerator crashed for {}", reportDate, e);
            markFailed(reportDate, e);
        }
    }

    private void doGenerate(LocalDate reportDate) {
        ZoneId zone = ZoneId.systemDefault();
        Instant startOfDay = reportDate.atStartOfDay(zone).toInstant();
        Instant endOfDay = reportDate.plusDays(1).atStartOfDay(zone).toInstant();

        DailyReport report = dailyReportRepository.findByReportDate(reportDate)
                .orElseGet(() -> {
                    DailyReport r = new DailyReport();
                    r.setReportDate(reportDate);
                    r.setStatus(DailyReportStatus.PENDING);
                    r.setContent("");
                    return dailyReportRepository.save(r);
                });

        if (SKIP_STATUSES.contains(report.getStatus())) {
            log.info("DailyReport for {} in status {}, skipping", reportDate, report.getStatus());
            return;
        }

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
            log.error("DailyReport generation failed: {}", result.error());
            return;
        }

        String content = result.data();
        if (content == null || content.isBlank()) {
            report.setStatus(DailyReportStatus.FAILED);
            report.setErrorMessage("Daily report content is empty");
            dailyReportRepository.save(report);
            log.error("DailyReport generation returned empty content for {}", reportDate);
            return;
        }

        report.setContent(content);
        report.setHotspotCount(hotspots.size());
        report.setGeneratedAt(Instant.now());
        report.setStatus(DailyReportStatus.READY);
        dailyReportRepository.save(report);
        log.info("DailyReport generated for {} with {} hotspots", reportDate, hotspots.size());
    }

    private void markFailed(LocalDate reportDate, Exception e) {
        try {
            dailyReportRepository.findByReportDate(reportDate)
                    .filter(report -> report.getStatus() == DailyReportStatus.PENDING
                            || report.getStatus() == DailyReportStatus.GENERATING)
                    .ifPresent(report -> {
                        report.setStatus(DailyReportStatus.FAILED);
                        report.setErrorMessage(e.getMessage());
                        if (report.getContent() == null) {
                            report.setContent("");
                        }
                        dailyReportRepository.save(report);
                    });
        } catch (Exception saveError) {
            log.error("Failed to mark DailyReport as FAILED for {}", reportDate, saveError);
        }
    }
}
