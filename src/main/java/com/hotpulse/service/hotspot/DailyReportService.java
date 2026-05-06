package com.hotpulse.service.hotspot;

import com.hotpulse.dto.DailyReportResponse;
import com.hotpulse.entity.DailyReport;
import com.hotpulse.repository.DailyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;

    public DailyReportResponse getByDate(LocalDate date) {
        DailyReport report = dailyReportRepository.findByReportDate(date)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("日报不存在: " + date));
        return toResponse(report);
    }

    public DailyReportResponse getLatest() {
        DailyReport report = dailyReportRepository.findTopByOrderByReportDateDesc()
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("暂无日报"));
        return toResponse(report);
    }

    private DailyReportResponse toResponse(DailyReport report) {
        DailyReportResponse resp = new DailyReportResponse();
        resp.setReportDate(report.getReportDate().toString());
        resp.setContent(report.getContent());
        resp.setHotspotCount(report.getHotspotCount());
        resp.setGeneratedAt(report.getGeneratedAt());
        return resp;
    }
}
