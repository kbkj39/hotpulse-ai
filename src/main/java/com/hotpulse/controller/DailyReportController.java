package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.DailyReportResponse;
import com.hotpulse.service.hotspot.DailyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class DailyReportController {

    private final DailyReportService dailyReportService;

    @GetMapping("/daily")
    public Result<DailyReportResponse> getDailyReport(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Result.ok(dailyReportService.getByDate(localDate));
    }

    @GetMapping("/daily/latest")
    public Result<DailyReportResponse> getLatestDailyReport() {
        return Result.ok(dailyReportService.getLatest());
    }
}
