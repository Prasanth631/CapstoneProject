package com.example.login;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.login.service.BuildHistoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/jenkins")
public class JenkinsProxyController {

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.user}")
    private String jenkinsUser;

    @Value("${jenkins.token}")
    private String jenkinsToken;

    @Autowired(required = false)
    private BuildHistoryService buildHistoryService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = jenkinsUser + ":" + jenkinsToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.set("Accept", "application/json");
        // Add CSRF protection header
        headers.set("Jenkins-Crumb", "no-csrf");
        return headers;
    }

    @GetMapping("/job/{jobName}/lastBuild")
    public ResponseEntity<String> getLastBuild(@PathVariable String jobName) {
        String url = jenkinsUrl + "/job/" + jobName + "/lastBuild/api/json";
        System.out.println("DEBUG: Attempting to access URL: " + url);

        try {
            HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("DEBUG: Response status: " + response.getStatusCode());

            // Save build data to database if service is available
            if (buildHistoryService != null && response.getBody() != null) {
                try {
                    JsonNode buildData = objectMapper.readTree(response.getBody());

                    Integer buildNumber = buildData.has("number") ? buildData.get("number").asInt() : null;
                    String status = buildData.has("result") && !buildData.get("result").isNull()
                            ? buildData.get("result").asText()
                            : "BUILDING";
                    Long duration = buildData.has("duration") ? buildData.get("duration").asLong() : null;
                    Long timestamp = buildData.has("timestamp") ? buildData.get("timestamp").asLong() : null;

                    if (buildNumber != null && timestamp != null) {
                        LocalDateTime buildTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(timestamp),
                                ZoneId.systemDefault());

                        buildHistoryService.saveBuild(jobName, buildNumber, status, duration, buildTime);
                        System.out.println("DEBUG: Saved build #" + buildNumber + " to database");
                    }
                } catch (Exception e) {
                    // Log error but don't fail the request
                    System.err.println("DEBUG: Failed to save build to database: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println(
                    "DEBUG: HTTP Error - Status: " + e.getStatusCode() + ", Body: " + e.getResponseBodyAsString());
            String errorMsg = String.format(
                    "{\"error\": \"Jenkins API Error\", \"status\": %d, \"message\": \"%s\", \"response\": \"%s\"}",
                    e.getStatusCode().value(), e.getMessage(),
                    e.getResponseBodyAsString().replace("\"", "\\\""));
            return ResponseEntity.status(e.getStatusCode()).body(errorMsg);
        } catch (Exception e) {
            System.err.println("DEBUG: General Error: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = String.format("{\"error\": \"Connection Error\", \"message\": \"%s\"}", e.getMessage());
            return ResponseEntity.status(500).body(errorMsg);
        }
    }

    @GetMapping("/job/{jobName}/lastBuild/consoleText")
    public ResponseEntity<String> getConsoleOutput(@PathVariable String jobName) {
        String url = jenkinsUrl + "/job/" + jobName + "/lastBuild/consoleText";
        System.out.println("DEBUG: Attempting to access URL: " + url);

        try {
            HttpHeaders headers = createAuthHeaders();
            headers.set("Accept", "text/plain");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("DEBUG: Response status: " + response.getStatusCode());

            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println(
                    "DEBUG: HTTP Error - Status: " + e.getStatusCode() + ", Body: " + e.getResponseBodyAsString());
            String errorMsg = String.format("Jenkins API Error: %s (Status: %d)\nResponse: %s",
                    e.getMessage(), e.getStatusCode().value(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(errorMsg);
        } catch (Exception e) {
            System.err.println("DEBUG: General Error: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = String.format("Connection Error: %s", e.getMessage());
            return ResponseEntity.status(500).body(errorMsg);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getJenkinsStatus() {
        String url = jenkinsUrl + "/api/json";
        System.out.println("DEBUG: Testing Jenkins connectivity at: " + url);

        try {
            HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("DEBUG: Jenkins connection successful, status: " + response.getStatusCode());

            return ResponseEntity.ok("{\"status\": \"connected\", \"jenkins_url\": \"" + jenkinsUrl + "\"}");
        } catch (HttpClientErrorException e) {
            System.err.println("DEBUG: Jenkins HTTP Error - Status: " + e.getStatusCode() + ", Body: "
                    + e.getResponseBodyAsString());
            String errorMsg = String.format("{\"status\": \"http_error\", \"error_code\": %d, \"error\": \"%s\"}",
                    e.getStatusCode().value(), e.getMessage().replace("\"", "\\\""));
            return ResponseEntity.status(e.getStatusCode()).body(errorMsg);
        } catch (Exception e) {
            System.err.println("DEBUG: Jenkins connection failed: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = String.format("{\"status\": \"disconnected\", \"error\": \"%s\"}",
                    e.getMessage().replace("\"", "\\\""));
            return ResponseEntity.status(500).body(errorMsg);
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<String> getAllJobs() {
        String url = jenkinsUrl + "/api/json?tree=jobs[name,url,lastBuild[number,result]]";
        System.out.println("DEBUG: Fetching all Jenkins jobs from: " + url);

        try {
            HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("DEBUG: Jobs fetched successfully");

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("DEBUG: Error fetching jobs: " + e.getMessage());
            String errorMsg = String.format("{\"error\": \"Failed to fetch jobs\", \"message\": \"%s\"}",
                    e.getMessage().replace("\"", "\\\""));
            return ResponseEntity.status(500).body(errorMsg);
        }
    }

    @GetMapping("/job/{jobName}")
    public ResponseEntity<String> getJobDetails(@PathVariable String jobName) {
        String url = jenkinsUrl + "/job/" + jobName
                + "/api/json?tree=builds[number,url],lastBuild[number,result,timestamp,duration]";
        System.out.println("DEBUG: Fetching job details from: " + url);

        try {
            HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("DEBUG: Job details fetched successfully");

            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println("DEBUG: HTTP Error - Status: " + e.getStatusCode());
            String errorMsg = String.format("{\"error\": \"Jenkins API Error\", \"status\": %d, \"message\": \"%s\"}",
                    e.getStatusCode().value(), e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(errorMsg);
        } catch (Exception e) {
            System.err.println("DEBUG: Error fetching job details: " + e.getMessage());
            String errorMsg = String.format("{\"error\": \"Connection Error\", \"message\": \"%s\"}", e.getMessage());
            return ResponseEntity.status(500).body(errorMsg);
        }
    }

    @GetMapping("/job/{jobName}/build/{buildNumber}")
    public ResponseEntity<String> getBuildInfo(@PathVariable String jobName, @PathVariable int buildNumber) {
        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/api/json";
        System.out.println("DEBUG: Fetching build info from: " + url);

        try {
            HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("DEBUG: Build info fetched successfully");

            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println("DEBUG: HTTP Error - Status: " + e.getStatusCode());
            String errorMsg = String.format("{\"error\": \"Jenkins API Error\", \"status\": %d, \"message\": \"%s\"}",
                    e.getStatusCode().value(), e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(errorMsg);
        } catch (Exception e) {
            System.err.println("DEBUG: Error fetching build info: " + e.getMessage());
            String errorMsg = String.format("{\"error\": \"Connection Error\", \"message\": \"%s\"}", e.getMessage());
            return ResponseEntity.status(500).body(errorMsg);
        }
    }
}