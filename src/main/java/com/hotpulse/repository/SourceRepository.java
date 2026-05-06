package com.hotpulse.repository;

import com.hotpulse.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRepository extends JpaRepository<Source, Long> {
    List<Source> findByEnabledTrue();
}
