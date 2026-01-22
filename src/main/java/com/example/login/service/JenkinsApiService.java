package com.example.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class JenkinsApiService {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsApiService.class);

    @Value("${jenkins.url:http://host.docker.internal:8080}")
    private String jenkinsUrl;

    @Value("${jenkins.user:root}")
    private String jenkinsUser;

    @Value("${jenkins.token:11372fa14838785c20bdd8ef361a974e9d}")
    private String jenkinsToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Get all Jenkins jobs with build info
     */
    public Map<String, Object> getAllJobs() {
        try {
            String url = jenkinsUrl + "/api/json?tree=jobs[name,url,color,lastBuild[number,result,duration,timestamp]]";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            logger.info("Jenkins jobs fetched successfully");
            return response.getBody();

        } catch (Exception e) {
            logger.error("Error fetching Jenkins jobs: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get recent builds for a specific job
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJobBuilds(String jobName, int limit) {
        try {
            String url = jenkinsUrl + "/job/" + jobName +
                    "/api/json?tree=builds[number,result,duration,timestamp,displayName]{0," + limit + "}";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("builds")) {
                return (List<Map<String, Object>>) body.get("builds");
            }

            return Collections.emptyList();

        } catch (Exception e) {
            logger.error("Error fetching builds for {}: {}", jobName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get build statistics across all jobs
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBuildStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get all jobs first
            Map<String, Object> jobsData = getAllJobs();
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) jobsData.get("jobs");

            if (jobs == null || jobs.isEmpty()) {
                logger.warn("No Jenkins jobs found");
                return getDefaultStats();
            }

            int totalBuilds = 0;
            int successBuilds = 0;
            int failureBuilds = 0;
            int unstableBuilds = 0;
            List<Map<String, Object>> allRecentBuilds = new ArrayList<>();

            for (Map<String, Object> job : jobs) {
                String jobName = (String) job.get("name");

                // Get builds for each job (last 50)
                List<Map<String, Object>> builds = getJobBuilds(jobName, 50);

                for (Map<String, Object> build : builds) {
                    totalBuilds++;
                    String result = (String) build.get("result");

                    if ("SUCCESS".equals(result)) {
                        successBuilds++;
                    } else if ("FAILURE".equals(result)) {
                        failureBuilds++;
                    } else if ("UNSTABLE".equals(result)) {
                        unstableBuilds++;
                    }

                    // Add job name to build for recent builds list
                    build.put("jobName", jobName);
                    allRecentBuilds.add(build);
                }
            }

            // Sort by timestamp descending
            allRecentBuilds.sort((a, b) -> {
                Long tsA = (Long) a.get("timestamp");
                Long tsB = (Long) b.get("timestamp");
                return tsB.compareTo(tsA);
            });

            // Calculate success rate
            double successRate = totalBuilds > 0 ? (successBuilds * 100.0 / totalBuilds) : 0.0;

            stats.put("totalBuilds", totalBuilds);
            stats.put("successBuilds", successBuilds);
            stats.put("failureBuilds", failureBuilds);
            stats.put("unstableBuilds", unstableBuilds);
            stats.put("successRate", successRate);
            stats.put("recentBuilds", allRecentBuilds.subList(0, Math.min(20, allRecentBuilds.size())));

            Map<String, Integer> statusBreakdown = new HashMap<>();
            if (successBuilds > 0)
                statusBreakdown.put("SUCCESS", successBuilds);
            if (failureBuilds > 0)
                statusBreakdown.put("FAILURE", failureBuilds);
            if (unstableBuilds > 0)
                statusBreakdown.put("UNSTABLE", unstableBuilds);
            stats.put("statusBreakdown", statusBreakdown);

            logger.info("Build statistics: {} total, {}% success rate", totalBuilds,
                    String.format("%.1f", successRate));
            return stats;

        } catch (Exception e) {
            logger.error("Error calculating build statistics: {}", e.getMessage());
            return getDefaultStats();
        }
    }

    /**
     * Get recent builds from all jobs combined
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentBuilds(int limit) {
        try {
            Map<String, Object> stats = getBuildStatistics();
            List<Map<String, Object>> recentBuilds = (List<Map<String, Object>>) stats.get("recentBuilds");

            if (recentBuilds != null) {
                return recentBuilds.subList(0, Math.min(limit, recentBuilds.size()));
            }

            return Collections.emptyList();

        } catch (Exception e) {
            logger.error("Error fetching recent builds: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = jenkinsUser + ":" + jenkinsToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> getDefaultStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBuilds", 0);
        stats.put("successBuilds", 0);
        stats.put("failureBuilds", 0);
        stats.put("successRate", 0.0);
        stats.put("statusBreakdown", new HashMap<>());
        stats.put("recentBuilds", new ArrayList<>());
        return stats;
    }
}
