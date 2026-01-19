# API Documentation

## Table of Contents
- [Base URL](#base-url)
- [Authentication](#authentication)
- [Jenkins Proxy APIs](#jenkins-proxy-apis)
- [Analytics APIs](#analytics-apis)
- [Actuator Endpoints](#actuator-endpoints)
- [Error Handling](#error-handling)

---

## Base URL

**Local Development:**
```
http://localhost:8082
```

**Kubernetes Deployment:**
```
http://localhost:30080
```

---

## Authentication

Currently, the API does not require authentication as it's designed for internal use within a Kubernetes cluster.

**Future Enhancement:** Add JWT-based authentication for production deployments.

---

## Jenkins Proxy APIs

### 1. List All Jobs
```http
GET /api/jenkins/jobs
```

**Description:** Retrieves list of all Jenkins jobs

**Response:**
```json
{
  "jobs": [
    {
      "name": "Automated",
      "url": "http://jenkins:8080/job/Automated/",
      "color": "blue"
    }
  ]
}
```

### 2. Get Last Build
```http
GET /api/jenkins/job/{jobName}/lastBuild
```

**Parameters:**
- `jobName` (path) - Name of the Jenkins job

**Example:**
```bash
curl http://localhost:30080/api/jenkins/job/Automated/lastBuild
```

**Response:**
```json
{
  "number": 42,
  "result": "SUCCESS",
  "duration": 125500,
  "timestamp": 1705670400000,
  "url": "http://jenkins:8080/job/Automated/42/",
  "building": false
}
```

### 3. Get Console Output
```http
GET /api/jenkins/job/{jobName}/lastBuild/consoleText
```

**Parameters:**
- `jobName` (path) - Name of the Jenkins job

**Example:**
```bash
curl http://localhost:30080/api/jenkins/job/Automated/lastBuild/consoleText
```

**Response:**
```
Started by user admin
Building in workspace /var/jenkins_home/workspace/Automated
...
Finished: SUCCESS
```

---

## Analytics APIs

### 1. Build Statistics
```http
GET /api/analytics/builds/statistics
```

**Description:** Get comprehensive build statistics including success rate and averages

**Example:**
```bash
curl http://localhost:30080/api/analytics/builds/statistics
```

**Response:**
```json
{
  "totalBuilds": 42,
  "successRate": 85.7,
  "statusBreakdown": {
    "SUCCESS": 36,
    "FAILURE": 6
  },
  "averageDurationByJob": {
    "Automated": 125.5
  }
}
```

**Fields:**
- `totalBuilds` - Total number of builds in database
- `successRate` - Percentage of successful builds
- `statusBreakdown` - Count by status (SUCCESS, FAILURE, BUILDING)
- `averageDurationByJob` - Average duration in seconds per job

### 2. Recent Builds
```http
GET /api/analytics/builds/recent?limit={limit}
```

**Parameters:**
- `limit` (query, optional) - Number of builds to return (default: 10)

**Example:**
```bash
curl http://localhost:30080/api/analytics/builds/recent?limit=5
```

**Response:**
```json
[
  {
    "id": 42,
    "jobName": "Automated",
    "buildNumber": 42,
    "status": "SUCCESS",
    "durationMs": 125500,
    "timestamp": "2026-01-19T12:00:00",
    "createdAt": "2026-01-19T12:02:05"
  },
  {
    "id": 41,
    "jobName": "Automated",
    "buildNumber": 41,
    "status": "FAILURE",
    "durationMs": 45200,
    "timestamp": "2026-01-19T11:30:00",
    "createdAt": "2026-01-19T11:30:45"
  }
]
```

### 3. Build History
```http
GET /api/analytics/builds/history?days={days}
```

**Parameters:**
- `days` (query, optional) - Number of days to look back (default: 7)

**Example:**
```bash
curl http://localhost:30080/api/analytics/builds/history?days=30
```

**Response:**
```json
[
  {
    "id": 42,
    "jobName": "Automated",
    "buildNumber": 42,
    "status": "SUCCESS",
    "durationMs": 125500,
    "timestamp": "2026-01-19T12:00:00"
  }
]
```

### 4. Metrics History
```http
GET /api/analytics/metrics/history?hours={hours}
```

**Parameters:**
- `hours` (query, optional) - Number of hours to look back (default: 24)

**Example:**
```bash
curl http://localhost:30080/api/analytics/metrics/history?hours=12
```

**Response:**
```json
[
  {
    "id": 1,
    "cpuUsage": 0.45,
    "memoryUsage": 0.62,
    "threadCount": 42,
    "httpRequestsTotal": 1250,
    "jvmMemoryUsed": 524288000,
    "jvmMemoryMax": 1073741824,
    "recordedAt": "2026-01-19T12:00:00"
  }
]
```

**Fields:**
- `cpuUsage` - CPU usage (0.0 to 1.0 = 0% to 100%)
- `memoryUsage` - Memory usage (0.0 to 1.0 = 0% to 100%)
- `threadCount` - Number of active threads
- `httpRequestsTotal` - Total HTTP requests processed
- `jvmMemoryUsed` - JVM memory used in bytes
- `jvmMemoryMax` - JVM max memory in bytes

### 5. Metrics Statistics
```http
GET /api/analytics/metrics/statistics?hours={hours}
```

**Parameters:**
- `hours` (query, optional) - Period for averages (default: 24)

**Example:**
```bash
curl http://localhost:30080/api/analytics/metrics/statistics?hours=6
```

**Response:**
```json
{
  "averageCpuUsage": 0.42,
  "averageMemoryUsage": 0.58,
  "periodHours": 6
}
```

### 6. Dashboard Summary
```http
GET /api/analytics/dashboard/summary
```

**Description:** Get all dashboard data in one request

**Example:**
```bash
curl http://localhost:30080/api/analytics/dashboard/summary
```

**Response:**
```json
{
  "buildStatistics": {
    "totalBuilds": 42,
    "successRate": 85.7,
    "statusBreakdown": {
      "SUCCESS": 36,
      "FAILURE": 6
    }
  },
  "recentBuilds": [
    {
      "id": 42,
      "jobName": "Automated",
      "buildNumber": 42,
      "status": "SUCCESS"
    }
  ],
  "metricsStatistics": {
    "averageCpuUsage": 0.42,
    "averageMemoryUsage": 0.58
  }
}
```

---

## Actuator Endpoints

### 1. Health Check
```http
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"],
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1081101176832,
        "free": 998803468288,
        "threshold": 10485760,
        "path": "/app/.",
        "exists": true
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

### 2. Liveness Probe
```http
GET /actuator/health/liveness
```

**Used by:** Kubernetes liveness probe

**Response:**
```json
{
  "status": "UP"
}
```

### 3. Readiness Probe
```http
GET /actuator/health/readiness
```

**Used by:** Kubernetes readiness probe

**Response:**
```json
{
  "status": "UP"
}
```

### 4. Prometheus Metrics
```http
GET /actuator/prometheus
```

**Response:** (Prometheus text format)
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 5.24288E7

# HELP system_cpu_usage The "recent cpu usage" for the whole system
# TYPE system_cpu_usage gauge
system_cpu_usage 0.42

# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/api/analytics/builds/statistics",} 125.0
```

### 5. Application Info
```http
GET /actuator/info
```

**Response:**
```json
{
  "app": {
    "name": "CapstoneProject",
    "version": "0.0.1-SNAPSHOT"
  }
}
```

### 6. Metrics List
```http
GET /actuator/metrics
```

**Response:**
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "system.cpu.usage",
    "process.cpu.usage",
    "http.server.requests"
  ]
}
```

### 7. Specific Metric
```http
GET /actuator/metrics/{metricName}
```

**Example:**
```bash
curl http://localhost:30080/actuator/metrics/system.cpu.usage
```

**Response:**
```json
{
  "name": "system.cpu.usage",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 0.42
    }
  ]
}
```

---

## Error Handling

### Error Response Format
```json
{
  "error": "Error Type",
  "message": "Detailed error message",
  "status": 500
}
```

### Common HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | Success | Request completed successfully |
| 404 | Not Found | Job or build not found |
| 500 | Server Error | Database connection failed |
| 503 | Service Unavailable | Jenkins is down |

### Example Error Responses

**404 Not Found:**
```json
{
  "error": "Not Found",
  "message": "Job 'NonExistent' not found",
  "status": 404
}
```

**500 Server Error:**
```json
{
  "error": "Database Error",
  "message": "Failed to connect to PostgreSQL",
  "status": 500
}
```

---

## Rate Limiting

Currently, there is no rate limiting implemented.

**Future Enhancement:** Add rate limiting using Spring Cloud Gateway or custom interceptor.

---

## CORS Configuration

CORS is enabled for local development:

```properties
spring.web.cors.allowed-origins=http://localhost:30080,http://localhost:8082
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
```

---

## Testing APIs

### Using cURL
```bash
# Get build statistics
curl -X GET http://localhost:30080/api/analytics/builds/statistics

# Get recent builds
curl -X GET "http://localhost:30080/api/analytics/builds/recent?limit=5"

# Get health status
curl -X GET http://localhost:30080/actuator/health
```

### Using Postman
1. Import collection from `postman_collection.json` (if provided)
2. Set base URL to `http://localhost:30080`
3. Execute requests

### Using Browser
Simply navigate to:
```
http://localhost:30080/api/analytics/builds/statistics
http://localhost:30080/actuator/health
```

---

**For architecture details, see [ARCHITECTURE.md](ARCHITECTURE.md)**
**For setup instructions, see [SETUP.md](SETUP.md)**
