package com.example.login.service;

import com.example.login.entity.SystemMetrics;
import com.example.login.repository.SystemMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemMetricsServiceTest {

    @Mock
    private SystemMetricsRepository repository;

    @InjectMocks
    private SystemMetricsService systemMetricsService;

    private SystemMetrics sampleMetrics;

    @BeforeEach
    void setUp() {
        sampleMetrics = new SystemMetrics(0.45, 0.65, 25, 1000L, 512000000L, 1024000000L);
        sampleMetrics.setId(1L);
        sampleMetrics.setRecordedAt(LocalDateTime.now());
    }

    @Test
    void saveMetrics_savesSuccessfully() {
        when(repository.save(any(SystemMetrics.class))).thenReturn(sampleMetrics);

        SystemMetrics result = systemMetricsService.saveMetrics(0.45, 0.65, 25, 1000L, 512000000L, 1024000000L);

        assertNotNull(result);
        assertEquals(0.45, result.getCpuUsage());
        assertEquals(0.65, result.getMemoryUsage());
        verify(repository, times(1)).save(any(SystemMetrics.class));
    }

    @Test
    void getMetricsFromLastHours_returnsMetrics() {
        List<SystemMetrics> metrics = Arrays.asList(sampleMetrics);
        when(repository.findByRecordedAtAfterOrderByRecordedAtDesc(any(LocalDateTime.class))).thenReturn(metrics);

        List<SystemMetrics> result = systemMetricsService.getMetricsFromLastHours(24);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getMetricsStatistics_calculatesCorrectly() {
        when(repository.getAverageCpuUsage(any(LocalDateTime.class))).thenReturn(0.45);
        when(repository.getAverageMemoryUsage(any(LocalDateTime.class))).thenReturn(0.65);

        Map<String, Object> stats = systemMetricsService.getMetricsStatistics(24);

        assertNotNull(stats);
        assertEquals(45.0, stats.get("averageCpuUsage"));
        assertEquals(65.0, stats.get("averageMemoryUsage"));
    }

    @Test
    void getMetricsStatistics_handlesNullAverages() {
        when(repository.getAverageCpuUsage(any(LocalDateTime.class))).thenReturn(null);
        when(repository.getAverageMemoryUsage(any(LocalDateTime.class))).thenReturn(null);

        Map<String, Object> stats = systemMetricsService.getMetricsStatistics(24);

        assertNotNull(stats);
        assertEquals(0.0, stats.get("averageCpuUsage"));
        assertEquals(0.0, stats.get("averageMemoryUsage"));
    }
}
