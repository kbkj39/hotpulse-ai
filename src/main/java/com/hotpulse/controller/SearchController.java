package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.SearchRequest;
import com.hotpulse.dto.SearchResponse;
import com.hotpulse.service.rag.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final RagService ragService;

    @PostMapping
    public Result<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 8;
        return Result.ok(ragService.query(request.getQuery(), topK));
    }
}
