package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.HotspotResponse;
import com.hotpulse.dto.TrendPoint;
import com.hotpulse.service.hotspot.HotspotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hotspots")
@RequiredArgsConstructor
public class HotspotController {

    private final HotspotService hotspotService;

    @GetMapping
    public Result<Map<String, Object>> getHotspots(
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String monitorKeyword,
            @RequestParam(required = false) Long executionId,
            @RequestParam(defaultValue = "1") int page) {
        return Result.ok(hotspotService.getHotspots(sort, page, limit, monitorKeyword, executionId, tag, keyword));
    }

    @GetMapping("/trends")
    public Result<List<TrendPoint>> getTrends(
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(required = false) String monitorKeyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        return Result.ok(hotspotService.getTrends(interval, monitorKeyword, tag, keyword));
    }

    @GetMapping("/{id}")
    public Result<HotspotResponse> getHotspotDetail(@PathVariable Long id) {
        return Result.ok(hotspotService.getHotspotDetail(id));
    }
}
