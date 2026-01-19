# System Architecture Documentation

## Table of Contents
- [Overview](#overview)
- [System Components](#system-components)
- [Data Flow](#data-flow)
- [Request-Response Lifecycle](#request-response-lifecycle)
- [Component Communication](#component-communication)
- [Database Schema](#database-schema)
- [Security Architecture](#security-architecture)

---

## Overview

The CI/CD Analytics Dashboard follows a **microservices-inspired architecture** with clear separation of concerns:

- **Frontend**: Static HTML/JS served by Spring Boot
- **Backend**: Spring Boot REST API with JPA
- **Database**: PostgreSQL StatefulSet
- **Monitoring**: Prometheus metrics collection
- **Orchestration**: Kubernetes for deployment
- **CI/CD**: Jenkins for automation

---

## System Components

### 1. Frontend Layer
```
┌─────────────────────────────────────┐
│         Dashboard (Browser)         │
│  ┌──────────┐  ┌──────────┐        │
│  │ Chart.js │  │ Tailwind │        │
│  │ Charts   │  │   CSS    │        │
│  └──────────┘  └──────────┘        │
│         index.html + index.js       │
└─────────────────────────────────────┘
```

**Technologies:**
- HTML5 for structure
- Tailwind CSS for styling
- Chart.js for visualizations
- Vanilla JavaScript for logic

**Responsibilities:**
- Render UI components
- Fetch data from REST APIs
- Update charts and tables
- Handle user interactions

### 2. Backend Layer
```
┌─────────────────────────────────────┐
│       Spring Boot Application       │
│  ┌──────────────────────────────┐  │
│  │      Controllers             │  │
│  │  - JenkinsProxyController    │  │
│  │  - AnalyticsController       │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │      Services                │  │
│  │  - BuildHistoryService       │  │
│  │  - SystemMetricsService      │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │      Repositories            │  │
│  │  - BuildHistoryRepository    │  │
│  │  - SystemMetricsRepository   │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Technologies:**
- Spring Boot 3.4.1
- Spring Data JPA
- Spring Web
- Spring Actuator

**Responsibilities:**
- Expose REST APIs
- Business logic processing
- Database operations
- Metrics collection

### 3. Database Layer
```
┌─────────────────────────────────────┐
│    PostgreSQL (StatefulSet)         │
│  ┌──────────────────────────────┐  │
│  │  Tables:                     │  │
│  │  - build_history             │  │
│  │  - system_metrics            │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │  Persistent Volume (1GB)     │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Technologies:**
- PostgreSQL 15-alpine
- Kubernetes StatefulSet
- PersistentVolumeClaim

**Responsibilities:**
- Store build history
- Store system metrics
- Provide data persistence

### 4. Monitoring Layer
```
┌─────────────────────────────────────┐
│         Prometheus                  │
│  - Scrapes /actuator/prometheus     │
│  - Stores time-series metrics       │
│  - Provides query interface         │
└─────────────────────────────────────┘
```

### 5. CI/CD Layer
```
┌─────────────────────────────────────┐
│           Jenkins                   │
│  - Checkout code                    │
│  - Run Maven build                  │
│  - Execute tests                    │
│  - Build Docker image               │
│  - Deploy to Kubernetes             │
└─────────────────────────────────────┘
```

---

## Data Flow

### 1. Build Data Flow
```
Jenkins Build
    │
    ├─► Triggers webhook/polling
    │
    ▼
JenkinsProxyController
    │
    ├─► Fetches build data via Jenkins API
    │
    ▼
BuildHistoryService
    │
    ├─► Saves to database
    │
    ▼
PostgreSQL
    │
    ├─► Persists build_history record
    │
    ▼
AnalyticsController
    │
    ├─► Queries historical data
    │
    ▼
Dashboard
    │
    └─► Renders charts and tables
```

### 2. Metrics Data Flow
```
Spring Boot App
    │
    ├─► Collects JVM metrics
    │
    ▼
Micrometer
    │
    ├─► Formats as Prometheus metrics
    │
    ▼
/actuator/prometheus
    │
    ├─► Exposes metrics endpoint
    │
    ▼
Prometheus (scrapes)
    │
    └─► Stores time-series data

Dashboard
    │
    ├─► Fetches current metrics
    │
    └─► Displays in KPI cards
```

---

## Request-Response Lifecycle

### Example: Fetching Build Statistics

**1. User Action**
```
User opens dashboard → http://localhost:30080
```

**2. Frontend Request**
```javascript
fetch('/api/analytics/builds/statistics')
  .then(response => response.json())
  .then(data => renderCharts(data))
```

**3. Backend Processing**
```java
@GetMapping("/builds/statistics")
public ResponseEntity<Map<String, Object>> getBuildStatistics() {
    // 1. Service layer call
    Map<String, Object> stats = buildHistoryService.getStatistics();
    
    // 2. Repository queries
    // - SELECT status, COUNT(*) FROM build_history GROUP BY status
    // - SELECT AVG(duration_ms) FROM build_history GROUP BY job_name
    
    // 3. Return response
    return ResponseEntity.ok(stats);
}
```

**4. Database Query**
```sql
-- Status breakdown
SELECT status, COUNT(*) as count
FROM build_history
GROUP BY status;

-- Average duration
SELECT job_name, AVG(duration_ms) as avg_duration
FROM build_history
GROUP BY job_name;
```

**5. Response Format**
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

**6. Frontend Rendering**
```javascript
// Update KPI cards
document.getElementById('kpi-total-builds').textContent = data.totalBuilds;
document.getElementById('kpi-success-rate').textContent = data.successRate + '%';

// Render charts
new Chart(ctx, {
  type: 'doughnut',
  data: {
    labels: Object.keys(data.statusBreakdown),
    datasets: [{
      data: Object.values(data.statusBreakdown)
    }]
  }
});
```

---

## Component Communication

### 1. Frontend ↔ Backend
```
Dashboard (Browser)
    │
    ├─► HTTP GET /api/analytics/builds/statistics
    │   └─► Response: JSON with build stats
    │
    ├─► HTTP GET /api/analytics/builds/history?days=7
    │   └─► Response: Array of build objects
    │
    └─► HTTP GET /actuator/prometheus
        └─► Response: Prometheus metrics text
```

**Protocol**: HTTP/REST
**Format**: JSON
**Authentication**: None (internal network)

### 2. Backend ↔ Database
```
Spring Boot App
    │
    ├─► JDBC Connection
    │   Host: postgres.capstone-app.svc.cluster.local
    │   Port: 5432
    │   Database: capstone
    │
    └─► JPA/Hibernate
        ├─► SELECT queries for reads
        ├─► INSERT for new records
        └─► UPDATE for modifications
```

**Protocol**: PostgreSQL wire protocol
**Connection Pool**: HikariCP (default)
**ORM**: Hibernate

### 3. Backend ↔ Jenkins
```
Spring Boot App
    │
    ├─► HTTP GET {JENKINS_URL}/api/json
    │   Headers: Authorization: Basic {base64(user:token)}
    │   └─► Response: Jenkins job list
    │
    └─► HTTP GET {JENKINS_URL}/job/{name}/lastBuild/api/json
        └─► Response: Build details
```

**Protocol**: HTTP/REST
**Authentication**: Basic Auth (username + API token)
**Format**: JSON

### 4. Kubernetes Service Discovery
```
Application Pod
    │
    ├─► DNS Lookup: postgres.capstone-app.svc.cluster.local
    │   └─► Resolves to: PostgreSQL Service ClusterIP
    │
    └─► Connection established to PostgreSQL pod
```

**DNS Format**: `{service-name}.{namespace}.svc.cluster.local`
**Service Type**: ClusterIP (internal only)

---

## Database Schema

### Table: build_history
```sql
CREATE TABLE build_history (
    id SERIAL PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,
    build_number INTEGER NOT NULL,
    status VARCHAR(50),              -- SUCCESS, FAILURE, BUILDING
    duration_ms BIGINT,
    timestamp TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(job_name, build_number)
);

-- Indexes for performance
CREATE INDEX idx_build_history_job_name ON build_history(job_name);
CREATE INDEX idx_build_history_timestamp ON build_history(timestamp);
CREATE INDEX idx_build_history_status ON build_history(status);
```

### Table: system_metrics
```sql
CREATE TABLE system_metrics (
    id SERIAL PRIMARY KEY,
    cpu_usage DECIMAL(5,2),
    memory_usage DECIMAL(5,2),
    thread_count INTEGER,
    http_requests_total BIGINT,
    jvm_memory_used BIGINT,
    jvm_memory_max BIGINT,
    recorded_at TIMESTAMP DEFAULT NOW()
);

-- Index for time-based queries
CREATE INDEX idx_system_metrics_recorded_at ON system_metrics(recorded_at);
```

---

## Security Architecture

### 1. Secrets Management
```
Kubernetes Secrets (Base64 encoded)
    │
    ├─► jenkins-credentials
    │   ├─► username
    │   └─► token
    │
    └─► postgres-secret
        ├─► database
        ├─► username
        └─► password
```

**Best Practices:**
- Never commit secrets to Git
- Use Kubernetes Secrets for sensitive data
- Rotate credentials regularly
- Use RBAC for secret access control

### 2. Network Security
```
┌─────────────────────────────────────┐
│  External Access (NodePort 30080)   │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│    Application Service (ClusterIP)  │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  PostgreSQL Service (ClusterIP)     │
│  (Internal only - no external access)│
└─────────────────────────────────────┘
```

**Security Layers:**
- PostgreSQL not exposed externally
- Application accessible via NodePort
- Network policies (can be added)

### 3. Authentication
- **Jenkins API**: Basic Auth with API token
- **Database**: Username/password from secrets
- **Dashboard**: No auth (internal tool)

---

## Scalability Considerations

### Horizontal Scaling
```yaml
# Increase replicas
spec:
  replicas: 3  # Scale to 3 pods
```

### Database Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

### Caching Strategy
- Add Redis for frequently accessed data
- Cache build statistics for 30 seconds
- Cache metrics for 10 seconds

---

## Monitoring & Observability

### Health Checks
```
Liveness Probe:  /actuator/health/liveness
Readiness Probe: /actuator/health/readiness
```

### Metrics
```
JVM Metrics:     /actuator/prometheus
Custom Metrics:  Build success rate, avg duration
```

### Logging
```
Application Logs: stdout (captured by Kubernetes)
Log Level:        INFO (configurable via env)
```

---

**For deployment details, see [KUBERNETES.md](KUBERNETES.md)**
**For API details, see [API_DOCUMENTATION.md](API_DOCUMENTATION.md)**
