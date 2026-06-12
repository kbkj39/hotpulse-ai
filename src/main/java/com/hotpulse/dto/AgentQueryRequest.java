package com.hotpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentQueryRequest {

    @NotBlank(message = "查询内容不能为空")
    private String query;

    private String conversationId;

    /** 可选：固定讨论的热点 ID，走热点上下文对话管线 */
    private Long hotspotId;
}
