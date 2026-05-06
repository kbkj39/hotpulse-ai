package com.hotpulse.repository;

import com.hotpulse.entity.AgentExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentExecutionRepository extends JpaRepository<AgentExecution, Long> {
    Page<AgentExecution> findAllByOrderByStartedAtDesc(Pageable pageable);
}
