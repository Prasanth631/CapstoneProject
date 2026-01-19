package com.example.login.service;

import com.example.login.entity.SystemMetrics;
import com.example.login.repository.SystemMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemMetricsService {

    @Autowired
    private SystemMetricsRepository repository;

    @Transactional
    public SystemMetrics saveMetrics(Double cpuUsage, Double memoryUsage, Integer threadCount,
            Long httpRequestsTotal, Long jvmMemoryUsed, Long jvmMemoryMax) {
        SystemMetrics metrics = new SystemMetrics(cpuUsage, memoryUsage, threadCount,
                httpRequestsTotal, jvmMemoryUsed, jvmMemoryMax);
        return repository.save(metrics);
    }

    public List<SystemMetrics> getRecentMetrics(int limit) {
        return repository.findTop50ByOrderByRecordedAtDesc();
    }

    public List<SystemMetrics> getMetricsFromLastHours(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return repository.findMetricsSince(since);
    }

    public Map<String, Object> getMetricsStatistics(int hours) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        Double avgCpu = repository.getAverageCpuUsage(since);
        Double avgMemory = repository.getAverageMemoryUsage(since);

        stats.put("averageCpuUsage", avgCpu != null ? avgCpu : 0.0);
        stats.put("averageMemoryUsage", avgMemory != null ? avgMemory : 0.0);
        stats.put("periodHours", hours);

        return stats;
    }
}
