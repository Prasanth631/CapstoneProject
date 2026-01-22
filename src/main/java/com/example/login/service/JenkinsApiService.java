package com.example.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class JenkinsApiService {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsApiService.class);

    // Try multiple URLs for Jenkins connectivity
    private static final String[] JENKINS_URLS = {
            "http://host.docker.internal:8080", // Docker Desktop (Windows/Mac)
            "http://172.17.0.1:8080", // Docker bridge network
            "http://localhost:8080", // Local fallback
            "http://host.minikube.internal:8080" // Minikube
    };

    @Value("${jenkins.url:}")
    private String configuredJenkinsUrl;

    @Value("${jenkins.user:root}")
    private String jenkinsUser;

    @Value("${jenkins.token:11372fa14838785c20bdd8ef361a974e9d}")
    private String jenkinsToken;

    private final RestTemplate restTemplate;
    private String workingJenkinsUrl = null;

    // Cached data for when Jenkins is unreachable
    private Map<String, Object> cachedStats = new HashMap<>();
    private List<Map<String, Object>> cachedBuilds = new ArrayList<>();
    private long lastSuccessfulFetch = 0;

    public JenkinsApiService() {
        this.restTemplate = new RestTemplate();
        // Set shorter timeouts
        // Note: For production, use RestTemplateBuilder with proper timeout config
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing JenkinsApiService...");
        findWorkingJenkinsUrl();
    }

    /**
     * Find a working Jenkins URL by testing each one
     */
    private void findWorkingJenkinsUrl() {
        // First try configured URL
        if (configuredJenkinsUrl != null && !configuredJenkinsUrl.isEmpty()) {
            if (testJenkinsConnection(configuredJenkinsUrl)) {
                workingJenkinsUrl = configuredJenkinsUrl;
                logger.info("Using configured Jenkins URL: {}", workingJenkinsUrl);
                return;
            }
        }

        // Try each URL
        for (String url : JENKINS_URLS) {
            if (testJenkinsConnection(url)) {
                workingJenkinsUrl = url;
                logger.info("Found working Jenkins URL: {}", workingJenkinsUrl);
                return;
            }
        }

        logger.warn("No working Jenkins URL found. Dashboard will show simulated data.");
    }

    private boolean testJenkinsConnection(String url) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url + "/api/json",
                    HttpMethod.GET,
                    entity,
                    String.class);

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.debug("Jenkins URL {} not accessible: {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * Get build statistics - with robust fallback
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBuildStatistics() {
        // Try to fetch from Jenkins
        if (workingJenkinsUrl != null) {
            try {
                Map<String, Object> stats = fetchBuildStatsFromJenkins();
                if (stats != null && !stats.isEmpty() && (Integer) stats.getOrDefault("totalBuilds", 0) > 0) {
                    cachedStats = stats;
                    lastSuccessfulFetch = System.currentTimeMillis();
                    return stats;
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch stats from Jenkins: {}", e.getMessage());
                // Try to reconnect
                findWorkingJenkinsUrl();
            }
        }

        // Return cached data if recent
        if (!cachedStats.isEmpty() && System.currentTimeMillis() - lastSuccessfulFetch < 300000) {
            return cachedStats;
        }

        // Return demo data for presentation
        return generateDemoStats();
    }

    /**
     * Fetch real stats from Jenkins
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchBuildStatsFromJenkins() {
        String url = workingJenkinsUrl + "/api/json?tree=jobs[name,builds[number,result,duration,timestamp]{0,50}]";

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        Map<String, Object> data = response.getBody();

        if (data == null || !data.containsKey("jobs")) {
            return Collections.emptyMap();
        }

        List<Map<String, Object>> jobs = (List<Map<String, Object>>) data.get("jobs");

        int totalBuilds = 0;
        int successBuilds = 0;
        int failureBuilds = 0;
        int unstableBuilds = 0;
        List<Map<String, Object>> allBuilds = new ArrayList<>();

        for (Map<String, Object> job : jobs) {
            String jobName = (String) job.get("name");
            List<Map<String, Object>> builds = (List<Map<String, Object>>) job.get("builds");

            if (builds != null) {
                for (Map<String, Object> build : builds) {
                    totalBuilds++;
                    String result = (String) build.get("result");

                    if ("SUCCESS".equals(result))
                        successBuilds++;
                    else if ("FAILURE".equals(result))
                        failureBuilds++;
                    else if ("UNSTABLE".equals(result))
                        unstableBuilds++;

                    Map<String, Object> buildInfo = new HashMap<>();
                    buildInfo.put("jobName", jobName);
                    buildInfo.put("number", build.get("number"));
                    buildInfo.put("result", result != null ? result : "BUILDING");
                    buildInfo.put("duration", build.get("duration"));
                    buildInfo.put("timestamp", build.get("timestamp"));
                    allBuilds.add(buildInfo);
                }
            }
        }

        // Sort by timestamp
        allBuilds.sort((a, b) -> {
            Long tsA = a.get("timestamp") instanceof Long ? (Long) a.get("timestamp") : 0L;
            Long tsB = b.get("timestamp") instanceof Long ? (Long) b.get("timestamp") : 0L;
            return tsB.compareTo(tsA);
        });

        // Cache builds
        cachedBuilds = allBuilds;

        double successRate = totalBuilds > 0 ? (successBuilds * 100.0 / totalBuilds) : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBuilds", totalBuilds);
        stats.put("successBuilds", successBuilds);
        stats.put("failureBuilds", failureBuilds);
        stats.put("unstableBuilds", unstableBuilds);
        stats.put("successRate", successRate);
        stats.put("recentBuilds", allBuilds.subList(0, Math.min(20, allBuilds.size())));

        Map<String, Integer> breakdown = new HashMap<>();
        if (successBuilds > 0)
            breakdown.put("SUCCESS", successBuilds);
        if (failureBuilds > 0)
            breakdown.put("FAILURE", failureBuilds);
        if (unstableBuilds > 0)
            breakdown.put("UNSTABLE", unstableBuilds);
        stats.put("statusBreakdown", breakdown);

        logger.info("Fetched {} builds from Jenkins, success rate: {}%", totalBuilds,
                String.format("%.1f", successRate));

        return stats;
    }

    /**
     * Generate demo stats for presentation when Jenkins is unreachable
     */
    private Map<String, Object> generateDemoStats() {
        Map<String, Object> stats = new HashMap<>();

        // Realistic demo data based on typical Jenkins usage
        int totalBuilds = 176;
        int successBuilds = 142;
        int failureBuilds = 28;
        int unstableBuilds = 6;

        double successRate = (successBuilds * 100.0 / totalBuilds);

        stats.put("totalBuilds", totalBuilds);
        stats.put("successBuilds", successBuilds);
        stats.put("failureBuilds", failureBuilds);
        stats.put("unstableBuilds", unstableBuilds);
        stats.put("successRate", successRate);

        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("SUCCESS", successBuilds);
        breakdown.put("FAILURE", failureBuilds);
        breakdown.put("UNSTABLE", unstableBuilds);
        stats.put("statusBreakdown", breakdown);

        // Generate recent builds
        List<Map<String, Object>> recentBuilds = new ArrayList<>();
        String[] statuses = { "SUCCESS", "SUCCESS", "SUCCESS", "FAILURE", "SUCCESS",
                "SUCCESS", "UNSTABLE", "SUCCESS", "SUCCESS", "FAILURE" };

        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> build = new HashMap<>();
            build.put("jobName", "Automated");
            build.put("number", 176 - i);
            build.put("result", statuses[i]);
            build.put("duration", 60000 + (int) (Math.random() * 120000)); // 1-3 min
            build.put("timestamp", baseTime - (i * 3600000L)); // 1 hour apart
            recentBuilds.add(build);
        }
        stats.put("recentBuilds", recentBuilds);

        logger.info("Using demo data: {} builds, {}% success rate", totalBuilds, String.format("%.1f", successRate));

        return stats;
    }

    /**
     * Get recent builds
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentBuilds(int limit) {
        Map<String, Object> stats = getBuildStatistics();
        List<Map<String, Object>> builds = (List<Map<String, Object>>) stats.get("recentBuilds");

        if (builds != null && !builds.isEmpty()) {
            return builds.subList(0, Math.min(limit, builds.size()));
        }

        return new ArrayList<>();
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = jenkinsUser + ":" + jenkinsToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Check if Jenkins is connected
     */
    public boolean isConnected() {
        return workingJenkinsUrl != null;
    }

    /**
     * Get connection status info
     */
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", isConnected());
        status.put("url", workingJenkinsUrl != null ? workingJenkinsUrl : "Not connected");
        status.put("lastFetch", lastSuccessfulFetch > 0 ? new Date(lastSuccessfulFetch).toString() : "Never");
        status.put("usingDemoData", !isConnected() || cachedStats.isEmpty());
        return status;
    }
}
