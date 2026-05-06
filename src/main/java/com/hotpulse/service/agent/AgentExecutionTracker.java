package com.hotpulse.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotpulse.common.AgentConstants;
import com.hotpulse.dto.AgentStepEvent;
import com.hotpulse.entity.AgentExecutionStep;
import com.hotpulse.repository.AgentExecutionStepRepository;
import com.hotpulse.sse.AgentSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutionTracker {

    private final AgentExecutionStepRepository stepRepository;
    private final AgentSseService agentSseService;
    private final ObjectMapper objectMapper;

    public AgentExecutionStep recordStep(Long executionId, String agentName, String status, String message, Object detail) {
        AgentExecutionStep step = new AgentExecutionStep();
        step.setExecutionId(executionId);
        step.setAgentName(agentName);
        step.setStatus(status);

        if (detail != null) {
            try {
                step.setDetailJson(objectMapper.writeValueAsString(detail));
            } catch (Exception e) {
                step.setDetailJson("{}");
            }
        }

        if (AgentConstants.STATUS_RUNNING.equals(status)) {
            step.setStartedAt(Instant.now());
        } else if (AgentConstants.STATUS_DONE.equals(status) || AgentConstants.STATUS_FAILED.equals(status)) {
            step.setStartedAt(Instant.now());
            step.setCompletedAt(Instant.now());
        }

        AgentExecutionStep saved = stepRepository.save(step);

        // 广播 SSE 事件
        AgentStepEvent event = new AgentStepEvent(agentName, status, message, Instant.now());
        agentSseService.broadcast(executionId.toString(), event);

        return saved;
    }
}
