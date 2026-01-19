package com.example.login.repository;

import com.example.login.entity.SystemMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemMetricsRepository extends JpaRepository<SystemMetrics, Long> {

    // Find latest N metrics
    List<SystemMetrics> findTop50ByOrderByRecordedAtDesc();

    // Find metrics within a date range
    List<SystemMetrics> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end);

    // Get average CPU usage over time
    @Query("SELECT AVG(m.cpuUsage) FROM SystemMetrics m WHERE m.recordedAt >= :since")
    Double getAverageCpuUsage(LocalDateTime since);

    // Get average memory usage over time
    @Query("SELECT AVG(m.memoryUsage) FROM SystemMetrics m WHERE m.recordedAt >= :since")
    Double getAverageMemoryUsage(LocalDateTime since);

    // Get metrics from last N hours
    @Query("SELECT m FROM SystemMetrics m WHERE m.recordedAt >= :since ORDER BY m.recordedAt ASC")
    List<SystemMetrics> findMetricsSince(LocalDateTime since);
}
