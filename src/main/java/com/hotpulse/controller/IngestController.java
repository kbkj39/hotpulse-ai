package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.IngestRequest;
import com.hotpulse.entity.Document;
import com.hotpulse.service.ingest.IngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final IngestService ingestService;

    @PostMapping
    public Result<Map<String, Object>> ingest(@Valid @RequestBody IngestRequest request) {
        // 基础 URL 合法性校验（防止 SSRF）
        String url = request.getUrl();
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            return Result.error(400, "URL 格式不合法");
        }

        Document doc = ingestService.ingest(url, request.getSourceId());
        if (doc == null) {
            return Result.ok(Map.of("status", "skipped", "reason", "duplicate or empty content"));
        }
        return Result.ok(Map.of("documentId", doc.getId(), "status", "queued"));
    }
}
