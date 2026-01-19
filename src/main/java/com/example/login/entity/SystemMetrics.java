package com.example.login.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_metrics")
public class SystemMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Double memoryUsage;

    @Column(name = "thread_count")
    private Integer threadCount;

    @Column(name = "http_requests_total")
    private Long httpRequestsTotal;

    @Column(name = "jvm_memory_used")
    private Long jvmMemoryUsed;

    @Column(name = "jvm_memory_max")
    private Long jvmMemoryMax;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }

    // Constructors
    public SystemMetrics() {
    }

    public SystemMetrics(Double cpuUsage, Double memoryUsage, Integer threadCount,
            Long httpRequestsTotal, Long jvmMemoryUsed, Long jvmMemoryMax) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.threadCount = threadCount;
        this.httpRequestsTotal = httpRequestsTotal;
        this.jvmMemoryUsed = jvmMemoryUsed;
        this.jvmMemoryMax = jvmMemoryMax;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Integer getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    public Long getHttpRequestsTotal() {
        return httpRequestsTotal;
    }

    public void setHttpRequestsTotal(Long httpRequestsTotal) {
        this.httpRequestsTotal = httpRequestsTotal;
    }

    public Long getJvmMemoryUsed() {
        return jvmMemoryUsed;
    }

    public void setJvmMemoryUsed(Long jvmMemoryUsed) {
        this.jvmMemoryUsed = jvmMemoryUsed;
    }

    public Long getJvmMemoryMax() {
        return jvmMemoryMax;
    }

    public void setJvmMemoryMax(Long jvmMemoryMax) {
        this.jvmMemoryMax = jvmMemoryMax;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
