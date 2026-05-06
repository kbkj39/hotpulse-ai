package com.hotpulse.repository;

import com.hotpulse.entity.AgentExecutionStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentExecutionStepRepository extends JpaRepository<AgentExecutionStep, Long> {
    List<AgentExecutionStep> findByExecutionIdOrderByStartedAtAsc(Long executionId);
}
