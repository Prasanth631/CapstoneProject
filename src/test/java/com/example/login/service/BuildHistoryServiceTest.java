package com.example.login.service;

import com.example.login.entity.BuildHistory;
import com.example.login.repository.BuildHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildHistoryServiceTest {

    @Mock
    private BuildHistoryRepository repository;

    @InjectMocks
    private BuildHistoryService buildHistoryService;

    private BuildHistory sampleBuild;

    @BeforeEach
    void setUp() {
        sampleBuild = new BuildHistory("test-job", 1, "SUCCESS", 5000L, LocalDateTime.now());
        sampleBuild.setId(1L);
    }

    @Test
    void saveBuild_newBuild_createsSuccessfully() {
        when(repository.findByJobNameAndBuildNumber("test-job", 1)).thenReturn(Optional.empty());
        when(repository.save(any(BuildHistory.class))).thenReturn(sampleBuild);

        BuildHistory result = buildHistoryService.saveBuild("test-job", 1, "SUCCESS", 5000L, LocalDateTime.now());

        assertNotNull(result);
        assertEquals("test-job", result.getJobName());
        assertEquals("SUCCESS", result.getStatus());
        verify(repository, times(1)).save(any(BuildHistory.class));
    }

    @Test
    void saveBuild_existingBuild_updatesSuccessfully() {
        when(repository.findByJobNameAndBuildNumber("test-job", 1)).thenReturn(Optional.of(sampleBuild));
        when(repository.save(any(BuildHistory.class))).thenReturn(sampleBuild);

        BuildHistory result = buildHistoryService.saveBuild("test-job", 1, "FAILURE", 6000L, LocalDateTime.now());

        assertNotNull(result);
        verify(repository, times(1)).save(sampleBuild);
    }

    @Test
    void getRecentBuilds_respectsLimit() {
        List<BuildHistory> builds = Arrays.asList(sampleBuild);
        when(repository.findRecentBuilds(any(PageRequest.class))).thenReturn(builds);

        List<BuildHistory> result = buildHistoryService.getRecentBuilds(5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findRecentBuilds(PageRequest.of(0, 5));
    }

    @Test
    void getStatistics_returnsCorrectData() {
        List<Object[]> statusStats = Arrays.<Object[]>asList(
                new Object[] { "SUCCESS", 8L },
                new Object[] { "FAILURE", 2L });
        List<Object[]> durationStats = Arrays.<Object[]>asList(
                new Object[] { "test-job", 5000.0 });
        when(repository.getStatusStatistics()).thenReturn(statusStats);
        when(repository.getAverageDurationByJob()).thenReturn(durationStats);

        Map<String, Object> stats = buildHistoryService.getStatistics();

        assertEquals(10L, stats.get("totalBuilds"));
        assertEquals(80.0, stats.get("successRate"));
        assertNotNull(stats.get("statusBreakdown"));
    }

    @Test
    void getBuildsFromLastDays_returnsBuilds() {
        List<BuildHistory> builds = Arrays.asList(sampleBuild);
        when(repository.findRecentBuilds(any(LocalDateTime.class))).thenReturn(builds);

        List<BuildHistory> result = buildHistoryService.getBuildsFromLastDays(7);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
