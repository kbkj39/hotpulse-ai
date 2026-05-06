package com.hotpulse.repository;

import com.hotpulse.entity.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    Optional<DailyReport> findByReportDate(LocalDate reportDate);
    Optional<DailyReport> findTopByOrderByReportDateDesc();
}
