package com.hotpulse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AgentQueryResponse {
    private String executionId;
    private String conversationId;
    private String status;
}
