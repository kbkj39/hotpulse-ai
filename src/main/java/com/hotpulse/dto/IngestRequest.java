package com.hotpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IngestRequest {

    @NotBlank(message = "URL 不能为空")
    private String url;

    private Long sourceId;
}
