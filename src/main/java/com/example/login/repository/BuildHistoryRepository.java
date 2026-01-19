package com.example.login.repository;

import com.example.login.entity.BuildHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BuildHistoryRepository extends JpaRepository<BuildHistory, Long> {

    // Find all builds for a specific job, ordered by build number descending
    List<BuildHistory> findByJobNameOrderByBuildNumberDesc(String jobName);

    // Find the latest N builds across all jobs
    List<BuildHistory> findTop10ByOrderByTimestampDesc();

    // Find builds by status
    List<BuildHistory> findByStatus(String status);

    // Find builds within a date range
    List<BuildHistory> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Check if a build already exists
    Optional<BuildHistory> findByJobNameAndBuildNumber(String jobName, Integer buildNumber);

    // Get success rate statistics
    @Query("SELECT b.status, COUNT(b) FROM BuildHistory b GROUP BY b.status")
    List<Object[]> getStatusStatistics();

    // Get average build duration by job
    @Query("SELECT b.jobName, AVG(b.durationMs) FROM BuildHistory b GROUP BY b.jobName")
    List<Object[]> getAverageDurationByJob();

    // Get builds from last N days
    @Query("SELECT b FROM BuildHistory b WHERE b.timestamp >= :since ORDER BY b.timestamp DESC")
    List<BuildHistory> findRecentBuilds(LocalDateTime since);
}
