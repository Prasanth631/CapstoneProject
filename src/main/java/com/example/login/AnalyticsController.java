package com.example.login;

import com.example.login.entity.BuildHistory;
import com.example.login.entity.SystemMetrics;
import com.example.login.service.BuildHistoryService;
import com.example.login.service.SystemMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private BuildHistoryService buildHistoryService;

    @Autowired
    private SystemMetricsService systemMetricsService;

    /**
     * Get build statistics and trends
     */
    @GetMapping("/builds/statistics")
    public ResponseEntity<Map<String, Object>> getBuildStatistics() {
        try {
            Map<String, Object> stats = buildHistoryService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch build statistics");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get recent build history
     */
    @GetMapping("/builds/recent")
    public ResponseEntity<List<BuildHistory>> getRecentBuilds(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<BuildHistory> builds = buildHistoryService.getRecentBuilds(limit);
            return ResponseEntity.ok(builds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
            return ResponseEntity.internalServerError().build();
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
            return ResponseEntity.internalServerError().build();
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
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get dashboard summary with all key metrics
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();

            // Build statistics
            Map<String, Object> buildStats = buildHistoryService.getStatistics();
            summary.put("buildStatistics", buildStats);

            // Recent builds
            List<BuildHistory> recentBuilds = buildHistoryService.getRecentBuilds(5);
            summary.put("recentBuilds", recentBuilds);

            // Metrics statistics
            Map<String, Object> metricsStats = systemMetricsService.getMetricsStatistics(24);
            summary.put("metricsStatistics", metricsStats);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch dashboard summary");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
