package com.example.login.controller;

import com.example.login.service.PrometheusMetricsCollector;
import com.example.login.service.SystemMetricsService;
import com.example.login.entity.SystemMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {

    @Autowired
    private PrometheusMetricsCollector metricsCollector;

    @Autowired
    private SystemMetricsService systemMetricsService;

    /**
     * Get real-time metrics from Prometheus
     */
    @GetMapping("/realtime")
    public ResponseEntity<SystemMetrics> getRealtimeMetrics() {
        SystemMetrics metrics = metricsCollector.getCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get JVM-specific metrics
     */
    @GetMapping("/jvm")
    public ResponseEntity<Map<String, Object>> getJvmMetrics() {
        SystemMetrics current = metricsCollector.getCurrentMetrics();

        Map<String, Object> jvmMetrics = new HashMap<>();
        jvmMetrics.put("heapUsed", current.getJvmMemoryUsed());
        jvmMetrics.put("heapMax", current.getJvmMemoryMax());
        jvmMetrics.put("heapUsagePercent", (current.getJvmMemoryUsed() * 100.0) / current.getJvmMemoryMax());
        jvmMetrics.put("threadCount", current.getThreadCount());

        return ResponseEntity.ok(jvmMetrics);
    }

    /**
     * Get HTTP metrics
     */
    @GetMapping("/http")
    public ResponseEntity<Map<String, Object>> getHttpMetrics() {
        SystemMetrics current = metricsCollector.getCurrentMetrics();

        Map<String, Object> httpMetrics = new HashMap<>();
        httpMetrics.put("totalRequests", current.getHttpRequestsTotal());
        httpMetrics.put("requestRate", calculateRequestRate());

        return ResponseEntity.ok(httpMetrics);
    }

    /**
     * Get metrics history for charts
     */
    @GetMapping("/history")
    public ResponseEntity<List<SystemMetrics>> getMetricsHistory(
            @RequestParam(defaultValue = "24") int hours) {
        List<SystemMetrics> history = systemMetricsService.getMetricsHistory(hours);
        return ResponseEntity.ok(history);
    }

    /**
     * Get system health summary
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        SystemMetrics current = metricsCollector.getCurrentMetrics();

        Map<String, Object> health = new HashMap<>();
        health.put("status", determineHealthStatus(current));
        health.put("cpuUsage", current.getCpuUsage());
        health.put("memoryUsage", current.getMemoryUsage());
        health.put("threadCount", current.getThreadCount());
        health.put("timestamp", current.getRecordedAt());

        return ResponseEntity.ok(health);
    }

    /**
     * Get metrics statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMetricsStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> stats = systemMetricsService.getMetricsStatistics(hours);
        return ResponseEntity.ok(stats);
    }

    // Helper methods

    private double calculateRequestRate() {
        // Calculate requests per second based on recent data
        List<SystemMetrics> recent = systemMetricsService.getMetricsHistory(1);
        if (recent.size() < 2)
            return 0.0;

        SystemMetrics latest = recent.get(0);
        SystemMetrics previous = recent.get(1);

        long requestDiff = latest.getHttpRequestsTotal() - previous.getHttpRequestsTotal();
        long timeDiff = 30; // 30 seconds between collections

        return (double) requestDiff / timeDiff;
    }

    private String determineHealthStatus(SystemMetrics metrics) {
        if (metrics.getCpuUsage() > 0.9 || metrics.getMemoryUsage() > 0.9) {
            return "CRITICAL";
        } else if (metrics.getCpuUsage() > 0.7 || metrics.getMemoryUsage() > 0.7) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
}
