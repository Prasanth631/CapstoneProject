package com.example.login.service;

import com.example.login.entity.SystemMetrics;
import com.example.login.repository.SystemMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PrometheusMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsCollector.class);
    private static final String PROMETHEUS_URL = "http://localhost:30090/api/v1/query";

    @Autowired
    private SystemMetricsRepository systemMetricsRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Collect metrics from Prometheus every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void collectAndStoreMetrics() {
        try {
            logger.info("Collecting metrics from Prometheus...");

            SystemMetrics metrics = new SystemMetrics();
            metrics.setRecordedAt(LocalDateTime.now());

            // Collect JVM metrics
            metrics.setCpuUsage(queryPrometheusMetric("system_cpu_usage"));
            metrics.setMemoryUsage(queryPrometheusMetric("jvm_memory_used_bytes{area=\"heap\"}") /
                    queryPrometheusMetric("jvm_memory_max_bytes{area=\"heap\"}"));

            // Collect thread metrics
            metrics.setThreadCount((int) queryPrometheusMetric("jvm_threads_live_threads"));

            // Collect HTTP metrics
            metrics.setHttpRequestsTotal((long) queryPrometheusMetric("http_server_requests_seconds_count"));

            // Collect JVM memory details
            metrics.setJvmMemoryUsed((long) queryPrometheusMetric("jvm_memory_used_bytes{area=\"heap\"}"));
            metrics.setJvmMemoryMax((long) queryPrometheusMetric("jvm_memory_max_bytes{area=\"heap\"}"));

            // Save to database
            systemMetricsRepository.save(metrics);
            logger.info("Metrics saved successfully");

        } catch (Exception e) {
            logger.error("Error collecting metrics: {}", e.getMessage());
        }
    }

    /**
     * Query Prometheus for a specific metric
     */
    private double queryPrometheusMetric(String query) {
        try {
            String url = PROMETHEUS_URL + "?query=" + query;
            String response = restTemplate.getForObject(url, String.class);

            // Parse the response to extract the metric value
            Pattern pattern = Pattern.compile("\"value\":\\[\\d+,\"([\\d.]+)\"\\]");
            Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }

            return 0.0;
        } catch (Exception e) {
            logger.warn("Failed to query metric {}: {}", query, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get current metrics without saving to database
     */
    public SystemMetrics getCurrentMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        metrics.setRecordedAt(LocalDateTime.now());
        metrics.setCpuUsage(queryPrometheusMetric("system_cpu_usage"));
        metrics.setMemoryUsage(queryPrometheusMetric("jvm_memory_used_bytes{area=\"heap\"}") /
                queryPrometheusMetric("jvm_memory_max_bytes{area=\"heap\"}"));
        metrics.setThreadCount((int) queryPrometheusMetric("jvm_threads_live_threads"));
        metrics.setHttpRequestsTotal((long) queryPrometheusMetric("http_server_requests_seconds_count"));
        metrics.setJvmMemoryUsed((long) queryPrometheusMetric("jvm_memory_used_bytes{area=\"heap\"}"));
        metrics.setJvmMemoryMax((long) queryPrometheusMetric("jvm_memory_max_bytes{area=\"heap\"}"));

        return metrics;
    }
}
