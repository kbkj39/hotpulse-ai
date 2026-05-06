package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.entity.Source;
import com.hotpulse.service.hotspot.SourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceService sourceService;

    @GetMapping
    public Result<List<Source>> getSources() {
        return Result.ok(sourceService.getSources());
    }

    @PostMapping
    public Result<Source> createSource(@RequestBody Source source) {
        return Result.ok(sourceService.createSource(source));
    }

    @PutMapping("/{id}")
    public Result<Source> updateSource(@PathVariable Long id, @RequestBody Source source) {
        return Result.ok(sourceService.updateSource(id, source));
    }

    @PatchMapping("/{id}/enabled")
    public Result<Source> setEnabled(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("Missing 'enabled' boolean in request body");
        }
        return Result.ok(sourceService.setEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    public Result<java.util.Map<String, Boolean>> deleteSource(@PathVariable Long id) {
        boolean deleted = sourceService.deleteSource(id);
        return Result.ok(java.util.Map.of("deleted", deleted));
    }
}
