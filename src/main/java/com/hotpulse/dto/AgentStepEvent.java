package com.hotpulse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AgentStepEvent {
    private String agentName;
    private String status;
    private String message;
    private Instant timestamp;
    private String answer;
    private List<HotspotResponse> hotspots;

    public AgentStepEvent(String agentName, String status, String message, Instant timestamp) {
        this.agentName = agentName;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
}
