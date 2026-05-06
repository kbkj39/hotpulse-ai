package com.hotpulse.service.agent;

import com.hotpulse.entity.AgentExecution;
import com.hotpulse.repository.AgentExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private final AgentExecutionRepository agentExecutionRepository;

    public AgentExecution create(String query, Long conversationId) {
        AgentExecution execution = new AgentExecution();
        execution.setQuery(query);
        execution.setStatus("RUNNING");
        execution.setConversationId(conversationId);
        execution.setStartedAt(Instant.now());
        return agentExecutionRepository.save(execution);
    }

    public AgentExecution markDone(Long id) {
        return updateStatus(id, "DONE");
    }

    public AgentExecution markFailed(Long id) {
        return updateStatus(id, "FAILED");
    }

    private AgentExecution updateStatus(Long id, String status) {
        AgentExecution execution = agentExecutionRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("执行记录不存在: " + id));
        execution.setStatus(status);
        execution.setCompletedAt(Instant.now());
        return agentExecutionRepository.save(execution);
    }

    public Page<AgentExecution> listHistory(int page, int limit) {
        return agentExecutionRepository.findAllByOrderByStartedAtDesc(PageRequest.of(page - 1, limit));
    }

    public void saveTaskPlan(Long id, String taskPlanJson) {
        AgentExecution execution = agentExecutionRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("执行记录不存在: " + id));
        execution.setTaskPlanJson(taskPlanJson);
        agentExecutionRepository.save(execution);
    }
}
