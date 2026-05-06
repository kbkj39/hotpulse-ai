package com.hotpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SearchRequest {

    @NotBlank(message = "查询内容不能为空")
    private String query;

    private Integer topK = 8;

    private Map<String, Object> filters;
}
