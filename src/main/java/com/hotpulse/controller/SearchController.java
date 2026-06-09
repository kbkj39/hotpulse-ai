package com.hotpulse.controller;

import com.hotpulse.common.Result;
import com.hotpulse.dto.SearchRequest;
import com.hotpulse.dto.SearchResponse;
import com.hotpulse.service.rag.RagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final ObjectProvider<RagService> ragServiceProvider;

    public SearchController(ObjectProvider<RagService> ragServiceProvider) {
        this.ragServiceProvider = ragServiceProvider;
    }

    @PostMapping
    public Result<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        RagService ragService = ragServiceProvider.getIfAvailable();
        if (ragService == null) {
            SearchResponse response = new SearchResponse();
            response.setAnswer(null);
            response.setEvidences(java.util.List.of());
            return Result.ok(response);
        }
        int topK = request.getTopK() != null ? request.getTopK() : 8;
        return Result.ok(ragService.query(request.getQuery(), topK));
    }
}
