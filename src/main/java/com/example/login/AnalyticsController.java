package com.example.login;

import com.example.login.entity.BuildHistory;
import com.example.login.entity.SystemMetrics;
import com.example.login.service.BuildHistoryService;
import com.example.login.service.JenkinsApiService;
import com.example.login.service.SystemMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private BuildHistoryService buildHistoryService;

    @Autowired
    private SystemMetricsService systemMetricsService;

    @Autowired
    private JenkinsApiService jenkinsApiService;

    /**
     * Get build statistics from Jenkins API (REAL DATA)
     */
    @GetMapping("/builds/statistics")
    public ResponseEntity<Map<String, Object>> getBuildStatistics() {
        try {
            // Fetch REAL data from Jenkins API
            Map<String, Object> jenkinsStats = jenkinsApiService.getBuildStatistics();

            if (jenkinsStats != null && !jenkinsStats.isEmpty()) {
                return ResponseEntity.ok(jenkinsStats);
            }

            // Fallback to database if Jenkins is not available
            Map<String, Object> stats = buildHistoryService.getStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch build statistics");
            error.put("message", e.getMessage());
            error.put("totalBuilds", 0);
            error.put("successRate", 0.0);
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Get recent builds from Jenkins API (REAL DATA)
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/builds/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentBuilds(@RequestParam(defaultValue = "20") int limit) {
        try {
            // Fetch REAL data from Jenkins API
            List<Map<String, Object>> jenkinsBuilds = jenkinsApiService.getRecentBuilds(limit);

            if (jenkinsBuilds != null && !jenkinsBuilds.isEmpty()) {
                // Format for frontend
                List<Map<String, Object>> formattedBuilds = new ArrayList<>();

                for (Map<String, Object> build : jenkinsBuilds) {
                    Map<String, Object> formatted = new HashMap<>();
                    formatted.put("jobName", build.get("jobName"));
                    formatted.put("buildNumber", build.get("number"));
                    formatted.put("status", build.get("result") != null ? build.get("result") : "BUILDING");
                    formatted.put("durationMs", build.get("duration"));

                    // Convert timestamp to ISO string
                    Object tsObj = build.get("timestamp");
                    if (tsObj instanceof Long) {
                        LocalDateTime ldt = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli((Long) tsObj),
                                ZoneId.systemDefault());
                        formatted.put("timestamp", ldt.toString());
                    } else {
                        formatted.put("timestamp", null);
                    }

                    formattedBuilds.add(formatted);
                }

                return ResponseEntity.ok(formattedBuilds);
            }

            // Fallback to database
            List<BuildHistory> dbBuilds = buildHistoryService.getRecentBuilds(limit);
            List<Map<String, Object>> fallback = new ArrayList<>();
            for (BuildHistory bh : dbBuilds) {
                Map<String, Object> m = new HashMap<>();
                m.put("jobName", bh.getJobName());
                m.put("buildNumber", bh.getBuildNumber());
                m.put("status", bh.getStatus());
                m.put("durationMs", bh.getDurationMs());
                m.put("timestamp", bh.getTimestamp() != null ? bh.getTimestamp().toString() : null);
                fallback.add(m);
            }

            return ResponseEntity.ok(fallback);

        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get builds from last N days
     */
    @GetMapping("/builds/history")
    public ResponseEntity<List<BuildHistory>> getBuildHistory(@RequestParam(defaultValue = "7") int days) {
        try {
            List<BuildHistory> builds = buildHistoryService.getBuildsFromLastDays(days);
            return ResponseEntity.ok(builds);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get system metrics from last N hours
     */
    @GetMapping("/metrics/history")
    public ResponseEntity<List<SystemMetrics>> getMetricsHistory(@RequestParam(defaultValue = "24") int hours) {
        try {
            List<SystemMetrics> metrics = systemMetricsService.getMetricsFromLastHours(hours);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get metrics statistics
     */
    @GetMapping("/metrics/statistics")
    public ResponseEntity<Map<String, Object>> getMetricsStatistics(@RequestParam(defaultValue = "24") int hours) {
        try {
            Map<String, Object> stats = systemMetricsService.getMetricsStatistics(hours);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch metrics statistics");
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Get dashboard summary with all key metrics - USING JENKINS API
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();

            // Build statistics from Jenkins
            Map<String, Object> buildStats = jenkinsApiService.getBuildStatistics();
            summary.put("buildStatistics", buildStats);

            // Recent builds from Jenkins
            List<Map<String, Object>> recentBuilds = jenkinsApiService.getRecentBuilds(10);
            summary.put("recentBuilds", recentBuilds);

            // Metrics statistics from database
            Map<String, Object> metricsStats = systemMetricsService.getMetricsStatistics(24);
            summary.put("metricsStatistics", metricsStats);

            // Recent metrics for charts
            List<SystemMetrics> recentMetrics = systemMetricsService.getMetricsFromLastHours(1);
            summary.put("recentMetrics", recentMetrics);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch dashboard summary");
            error.put("message", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Get build trends for visualization
     */
    @GetMapping("/builds/trends")
    public ResponseEntity<Map<String, Object>> getBuildTrends(@RequestParam(defaultValue = "30") int days) {
        try {
            List<BuildHistory> builds = buildHistoryService.getBuildsFromLastDays(days);

            Map<String, Object> trends = new HashMap<>();
            trends.put("totalBuilds", builds.size());
            trends.put("builds", builds);

            // Calculate daily build counts
            Map<String, Long> dailyCounts = new HashMap<>();
            for (BuildHistory build : builds) {
                if (build.getTimestamp() != null) {
                    String date = build.getTimestamp().toLocalDate().toString();
                    dailyCounts.put(date, dailyCounts.getOrDefault(date, 0L) + 1);
                }
            }
            trends.put("dailyCounts", dailyCounts);

            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * Get performance metrics
     */
    @GetMapping("/performance/metrics")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> performance = new HashMap<>();

            // Get recent metrics
            List<SystemMetrics> recent = systemMetricsService.getMetricsFromLastHours(1);
            if (!recent.isEmpty()) {
                SystemMetrics latest = recent.get(0);
                performance.put("currentCpu", latest.getCpuUsage() * 100);
                performance.put("currentMemory", latest.getMemoryUsage() * 100);
                performance.put("currentThreads", latest.getThreadCount());
            }

            // Get statistics
            Map<String, Object> stats = systemMetricsService.getMetricsStatistics(24);
            performance.put("averageCpu", stats.get("averageCpuUsage"));
            performance.put("averageMemory", stats.get("averageMemoryUsage"));

            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<>());
        }
    }
}
