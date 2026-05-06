package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.HotspotResponse;
import com.hotpulse.service.hotspot.HotspotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            @RequestParam(defaultValue = "1") int page) {
        return Result.ok(hotspotService.getHotspots(sort, page, limit, tag, keyword));
    }

    @GetMapping("/{id}")
    public Result<HotspotResponse> getHotspotDetail(@PathVariable Long id) {
        return Result.ok(hotspotService.getHotspotDetail(id));
    }
}
