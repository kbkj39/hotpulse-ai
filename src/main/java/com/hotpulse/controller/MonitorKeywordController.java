package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.MonitorKeywordCreateResponse;
import com.hotpulse.dto.MonitorKeywordRequest;
import com.hotpulse.entity.MonitorKeyword;
import com.hotpulse.service.hotspot.MonitorKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitor-keywords")
@RequiredArgsConstructor
public class MonitorKeywordController {

    private final MonitorKeywordService monitorKeywordService;

    @GetMapping
    public Result<List<MonitorKeyword>> list() {
        return Result.ok(monitorKeywordService.list());
    }

    @PostMapping
    public Result<MonitorKeywordCreateResponse> create(@RequestBody MonitorKeywordRequest request) {
        return Result.ok(monitorKeywordService.create(request));
    }

    @PutMapping("/{id}")
    public Result<MonitorKeyword> update(@PathVariable Long id, @RequestBody MonitorKeywordRequest request) {
        return Result.ok(monitorKeywordService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> delete(@PathVariable Long id) {
        monitorKeywordService.delete(id);
        return Result.ok(Map.of("deleted", true));
    }

    @PostMapping("/{id}/trigger")
    public Result<Map<String, Object>> trigger(@PathVariable Long id) {
        Long executionId = monitorKeywordService.triggerById(id);
        return Result.ok(Map.of("executionId", executionId));
    }

    @PostMapping("/trigger-all")
    public Result<java.util.List<Map<String, Object>>> triggerAll() {
        return Result.ok(monitorKeywordService.triggerAll());
    }
}
