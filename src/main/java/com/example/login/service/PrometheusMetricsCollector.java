package com.example.login.service;

import com.example.login.entity.SystemMetrics;
import com.example.login.repository.SystemMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;

@Service
public class PrometheusMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsCollector.class);

    @Autowired
    private SystemMetricsRepository systemMetricsRepository;

    private long previousRequestCount = 0;
    private long currentRequestCount = 0;

    /**
     * Collect metrics every 30 seconds using JMX (works without external
     * Prometheus)
     */
    @Scheduled(fixedRate = 30000)
    public void collectAndStoreMetrics() {
        try {
            logger.info("Collecting metrics from JVM...");

            SystemMetrics metrics = getCurrentMetrics();

            // Save to database
            systemMetricsRepository.save(metrics);
            logger.info("Metrics saved successfully - CPU: {}%, Memory: {}%, Threads: {}",
                    String.format("%.1f", metrics.getCpuUsage() * 100),
                    String.format("%.1f", metrics.getMemoryUsage() * 100),
                    metrics.getThreadCount());

        } catch (Exception e) {
            logger.error("Error collecting metrics: {}", e.getMessage());
        }
    }

    /**
     * Get current metrics using JMX - always works without external dependencies
     */
    public SystemMetrics getCurrentMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        metrics.setRecordedAt(LocalDateTime.now());

        try {
            // Get CPU usage from OperatingSystemMXBean
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            if (cpuLoad < 0) {
                // getSystemLoadAverage() returns -1 on Windows, use alternative
                cpuLoad = getProcessCpuLoad(osBean);
            }
            // Normalize to 0-1 range (assuming max load = number of processors)
            double normalizedCpu = Math.min(cpuLoad / osBean.getAvailableProcessors(), 1.0);
            metrics.setCpuUsage(Math.max(0, normalizedCpu));

            // Get Memory usage from MemoryMXBean
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();

            metrics.setJvmMemoryUsed(heapUsed);
            metrics.setJvmMemoryMax(heapMax);

            if (heapMax > 0) {
                metrics.setMemoryUsage((double) heapUsed / heapMax);
            } else {
                metrics.setMemoryUsage(0.0);
            }

            // Get Thread count
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            metrics.setThreadCount(threadBean.getThreadCount());

            // Track HTTP requests (increment counter for demo)
            currentRequestCount++;
            metrics.setHttpRequestsTotal(currentRequestCount);

        } catch (Exception e) {
            logger.error("Error getting JVM metrics: {}", e.getMessage());
            // Set defaults on error
            metrics.setCpuUsage(0.0);
            metrics.setMemoryUsage(0.0);
            metrics.setThreadCount(0);
            metrics.setHttpRequestsTotal(0L);
            metrics.setJvmMemoryUsed(0L);
            metrics.setJvmMemoryMax(1L); // Avoid division by zero
        }

        return metrics;
    }

    /**
     * Get process CPU load using reflection (works on Windows)
     */
    private double getProcessCpuLoad(OperatingSystemMXBean osBean) {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                return sunOsBean.getCpuLoad();
            }
        } catch (Exception e) {
            // Fallback
        }
        return 0.1; // Default fallback value
    }
}
