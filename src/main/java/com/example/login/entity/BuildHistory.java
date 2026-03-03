package com.example.login.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "build_history")
@Getter
@Setter
@NoArgsConstructor
public class BuildHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "build_number", nullable = false)
    private Integer buildNumber;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public BuildHistory(String jobName, Integer buildNumber, String status, Long durationMs, LocalDateTime timestamp) {
        this.jobName = jobName;
        this.buildNumber = buildNumber;
        this.status = status;
        this.durationMs = durationMs;
        this.timestamp = timestamp;
    }
}
