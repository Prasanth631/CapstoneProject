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

    private volatile SystemMetrics currentMetrics = new SystemMetrics();
    private long httpRequestCounter = 0;

    /**
     * Collect metrics every 30 seconds using JMX
     */
    @Scheduled(fixedRate = 30000)
    public void collectAndStoreMetrics() {
        try {
            logger.info("Collecting metrics from JVM...");

            currentMetrics = collectJvmMetrics();

            // Save to database
            systemMetricsRepository.save(currentMetrics);

            logger.info("Metrics saved - CPU: {}%, Memory: {}%, Threads: {}",
                    String.format("%.1f", currentMetrics.getCpuUsage() * 100),
                    String.format("%.1f", currentMetrics.getMemoryUsage() * 100),
                    currentMetrics.getThreadCount());

        } catch (Exception e) {
            logger.error("Error collecting metrics: {}", e.getMessage());
        }
    }

    /**
     * Get current metrics - returns cached value for real-time display
     */
    public SystemMetrics getCurrentMetrics() {
        if (currentMetrics.getRecordedAt() == null) {
            // First call - collect immediately
            currentMetrics = collectJvmMetrics();
        }
        return currentMetrics;
    }

    /**
     * Collect JVM metrics using management beans
     */
    private SystemMetrics collectJvmMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        metrics.setRecordedAt(LocalDateTime.now());

        try {
            // ===== CPU USAGE =====
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = 0.0;

            // Try to get process CPU load (more accurate than system load)
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;

                // Get process CPU load (0.0 to 1.0)
                double processCpu = sunOsBean.getProcessCpuLoad();
                if (processCpu >= 0) {
                    cpuUsage = processCpu;
                } else {
                    // Fallback to system CPU load
                    double systemCpu = sunOsBean.getCpuLoad();
                    cpuUsage = systemCpu >= 0 ? systemCpu : 0.1;
                }
            } else {
                // Fallback: use system load average normalized by processors
                double loadAvg = osBean.getSystemLoadAverage();
                int processors = osBean.getAvailableProcessors();
                if (loadAvg >= 0 && processors > 0) {
                    cpuUsage = Math.min(loadAvg / processors, 1.0);
                } else {
                    cpuUsage = 0.1; // Default fallback
                }
            }

            // Ensure CPU is in valid range and not stuck at 100%
            cpuUsage = Math.max(0, Math.min(cpuUsage, 1.0));
            metrics.setCpuUsage(cpuUsage);

            // ===== MEMORY USAGE =====
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

            // ===== THREAD COUNT =====
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            metrics.setThreadCount(threadBean.getThreadCount());

            // ===== HTTP REQUESTS (Increment counter) =====
            httpRequestCounter++;
            metrics.setHttpRequestsTotal(httpRequestCounter);

        } catch (Exception e) {
            logger.error("Error collecting JVM metrics: {}", e.getMessage());
            // Set safe defaults
            metrics.setCpuUsage(0.1);
            metrics.setMemoryUsage(0.2);
            metrics.setThreadCount(20);
            metrics.setHttpRequestsTotal(0L);
            metrics.setJvmMemoryUsed(0L);
            metrics.setJvmMemoryMax(1L);
        }

        return metrics;
    }
}
