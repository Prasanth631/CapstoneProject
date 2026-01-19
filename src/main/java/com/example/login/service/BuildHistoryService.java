package com.example.login.service;

import com.example.login.entity.BuildHistory;
import com.example.login.repository.BuildHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BuildHistoryService {

    @Autowired
    private BuildHistoryRepository repository;

    @Transactional
    public BuildHistory saveBuild(String jobName, Integer buildNumber, String status, Long durationMs,
            LocalDateTime timestamp) {
        // Check if build already exists
        Optional<BuildHistory> existing = repository.findByJobNameAndBuildNumber(jobName, buildNumber);
        if (existing.isPresent()) {
            // Update existing build
            BuildHistory build = existing.get();
            build.setStatus(status);
            build.setDurationMs(durationMs);
            build.setTimestamp(timestamp);
            return repository.save(build);
        } else {
            // Create new build
            BuildHistory build = new BuildHistory(jobName, buildNumber, status, durationMs, timestamp);
            return repository.save(build);
        }
    }

    public List<BuildHistory> getRecentBuilds(int limit) {
        return repository.findTop10ByOrderByTimestampDesc();
    }

    public List<BuildHistory> getBuildsByJob(String jobName) {
        return repository.findByJobNameOrderByBuildNumberDesc(jobName);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Get status statistics
        List<Object[]> statusStats = repository.getStatusStatistics();
        Map<String, Long> statusMap = new HashMap<>();
        long totalBuilds = 0;
        long successBuilds = 0;

        for (Object[] stat : statusStats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            statusMap.put(status, count);
            totalBuilds += count;
            if ("SUCCESS".equals(status)) {
                successBuilds = count;
            }
        }

        stats.put("statusBreakdown", statusMap);
        stats.put("totalBuilds", totalBuilds);
        stats.put("successRate", totalBuilds > 0 ? (double) successBuilds / totalBuilds * 100 : 0);

        // Get average duration by job
        List<Object[]> durationStats = repository.getAverageDurationByJob();
        Map<String, Double> durationMap = new HashMap<>();
        for (Object[] stat : durationStats) {
            String jobName = (String) stat[0];
            Double avgDuration = (Double) stat[1];
            durationMap.put(jobName, avgDuration / 1000); // Convert to seconds
        }
        stats.put("averageDurationByJob", durationMap);

        return stats;
    }

    public List<BuildHistory> getBuildsFromLastDays(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return repository.findRecentBuilds(since);
    }
}
