package com.example.login.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_metrics")
@Getter
@Setter
@NoArgsConstructor
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

    public SystemMetrics(Double cpuUsage, Double memoryUsage, Integer threadCount,
            Long httpRequestsTotal, Long jvmMemoryUsed, Long jvmMemoryMax) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.threadCount = threadCount;
        this.httpRequestsTotal = httpRequestsTotal;
        this.jvmMemoryUsed = jvmMemoryUsed;
        this.jvmMemoryMax = jvmMemoryMax;
    }
}
