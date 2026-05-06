package com.hotpulse.repository;

import com.hotpulse.entity.MonitorKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorKeywordRepository extends JpaRepository<MonitorKeyword, Long> {

    Optional<MonitorKeyword> findByKeywordIgnoreCase(String keyword);

    List<MonitorKeyword> findByEnabledTrue();
}
