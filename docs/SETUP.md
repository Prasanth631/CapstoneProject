# Setup & Deployment Guide

## Table of Contents
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Docker Setup](#docker-setup)
- [Kubernetes Setup](#kubernetes-setup)
- [Jenkins Configuration](#jenkins-configuration)
- [Environment Variables](#environment-variables)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java JDK** | 21+ | Application runtime |
| **Maven** | 3.9+ | Build tool |
| **Docker Desktop** | Latest | Container runtime |
| **Kubernetes** | 1.28+ | Orchestration (enabled in Docker Desktop) |
| **Jenkins** | Latest | CI/CD automation |
| **Git** | Latest | Version control |

### System Requirements

- **OS**: Windows 10/11, macOS, or Linux
- **RAM**: 8GB minimum, 16GB recommended
- **Disk**: 20GB free space
- **CPU**: 4 cores recommended

---

## Local Development Setup

### Step 1: Clone Repository
```bash
git clone https://github.com/Prasanth631/CapstoneProject.git
cd CapstoneProject
```

### Step 2: Configure Environment
```bash
# Copy environment template
cp .env.example .env

# Edit .env with your values
# Windows: notepad .env
# Mac/Linux: nano .env
```

**Required Values:**
```env
JENKINS_URL=http://localhost:8080
JENKINS_USER=your_username
JENKINS_TOKEN=your_api_token
```

### Step 3: Build Project
```bash
# Clean and build
mvn clean install

# Skip tests for faster build
mvn clean install -DskipTests
```

### Step 4: Run Locally
```bash
# Run with Maven
mvn spring-boot:run

# Or run JAR directly
java -jar target/CapstoneProject-0.0.1-SNAPSHOT.jar
```

### Step 5: Access Application
```
Dashboard: http://localhost:8082
Health:    http://localhost:8082/actuator/health
Metrics:   http://localhost:8082/actuator/prometheus
```

---

## Docker Setup

### Step 1: Build Docker Image
```bash
# Build image
docker build -t prasanth631/capstone_pro:latest .

# Verify image
docker images | grep capstone
```

### Step 2: Run Container
```bash
docker run -d \
  --name capstone-app \
  -p 8082:8082 \
  -e JENKINS_URL=http://host.docker.internal:8080 \
  -e JENKINS_USER=root \
  -e JENKINS_TOKEN=your_token \
  prasanth631/capstone_pro:latest
```

### Step 3: Verify Container
```bash
# Check container status
docker ps

# View logs
docker logs -f capstone-app

# Test health endpoint
curl http://localhost:8082/actuator/health
```

### Step 4: Stop Container
```bash
docker stop capstone-app
docker rm capstone-app
```

---

## Kubernetes Setup

### Step 1: Enable Kubernetes in Docker Desktop

**Windows/Mac:**
1. Open Docker Desktop
2. Settings → Kubernetes
3. Check "Enable Kubernetes"
4. Click "Apply & Restart"
5. Wait for Kubernetes to start (green indicator)

**Verify:**
```bash
kubectl version --client
kubectl cluster-info
```

### Step 2: Create Namespace
```bash
kubectl create namespace capstone-app
```

### Step 3: Deploy PostgreSQL
```bash
# Apply PostgreSQL resources
kubectl apply -f k8s/postgres-secret.yaml
kubectl apply -f k8s/postgres-service.yaml
kubectl apply -f k8s/postgres-statefulset.yaml

# Verify PostgreSQL pod
kubectl get pods -n capstone-app
# Should show: postgres-0   1/1   Running
```

### Step 4: Deploy Application
```bash
# Apply application resources
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/deployment.yaml

# Verify deployment
kubectl get deployments -n capstone-app
kubectl get pods -n capstone-app
```

### Step 5: Access Dashboard
```
Dashboard: http://localhost:30080
```

### Step 6: Monitor Deployment
```bash
# Watch pod status
kubectl get pods -n capstone-app -w

# View logs
kubectl logs -f deployment/capstone-deployment -n capstone-app

# Check service
kubectl get services -n capstone-app
```

---

## Jenkins Configuration

### Step 1: Install Jenkins

**Using Docker:**
```bash
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

**Access Jenkins:**
```
http://localhost:8080
```

### Step 2: Get Initial Admin Password
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Step 3: Install Required Plugins
1. Dashboard → Manage Jenkins → Plugins
2. Install:
   - Docker Pipeline
   - Kubernetes
   - Git
   - Maven Integration

### Step 4: Configure Credentials
1. Dashboard → Manage Jenkins → Credentials
2. Add credentials:
   - **Docker Hub**: Username + Password
   - **Kubernetes**: Kubeconfig file

### Step 5: Create Pipeline Job
1. New Item → Pipeline
2. Name: "Automated"
3. Pipeline → Definition: "Pipeline script from SCM"
4. SCM: Git
5. Repository URL: `https://github.com/Prasanth631/CapstoneProject.git`
6. Script Path: `Jenkinsfile`
7. Save

### Step 6: Get API Token
1. User → Configure
2. API Token → Add new Token
3. Copy token for `.env` file

---

## Environment Variables

### Application Configuration

**Required:**
```env
# Jenkins
JENKINS_URL=http://host.docker.internal:8080
JENKINS_USER=root
JENKINS_TOKEN=your_jenkins_api_token

# Database
DB_HOST=postgres.capstone-app.svc.cluster.local
DB_NAME=capstone
DB_USER=admin
DB_PASSWORD=your_secure_password
```

**Optional:**
```env
# Application
SERVER_PORT=8082
SPRING_PROFILES_ACTIVE=prod

# Kubernetes
K8S_NAMESPACE=capstone-app
```

### Kubernetes Secrets

**Update secrets.yaml:**
```bash
# Encode values
echo -n "your_username" | base64
echo -n "your_token" | base64

# Update k8s/secrets.yaml with encoded values
kubectl apply -f k8s/secrets.yaml
```

---

## Troubleshooting

### Application Won't Start

**Check logs:**
```bash
# Local
tail -f logs/application.log

# Docker
docker logs capstone-app

# Kubernetes
kubectl logs -f deployment/capstone-deployment -n capstone-app
```

**Common Issues:**
1. **Port already in use**
   ```bash
   # Windows
   netstat -ano | findstr :8082
   taskkill /PID <PID> /F
   
   # Mac/Linux
   lsof -i :8082
   kill -9 <PID>
   ```

2. **Database connection failed**
   - Verify PostgreSQL pod is running
   - Check database credentials in secrets
   - Test connection: `kubectl exec -it postgres-0 -n capstone-app -- psql -U admin -d capstone`

3. **Jenkins connection failed**
   - Verify Jenkins is running: `curl http://localhost:8080`
   - Check credentials in `.env` or secrets
   - Verify API token is valid

### Kubernetes Issues

**Pods not starting:**
```bash
# Describe pod for details
kubectl describe pod <pod-name> -n capstone-app

# Check events
kubectl get events -n capstone-app --sort-by='.lastTimestamp'
```

**Common Issues:**
1. **ImagePullBackOff**
   - Image doesn't exist in registry
   - Solution: Build and push image to Docker Hub

2. **CrashLoopBackOff**
   - Application crashes on startup
   - Check logs: `kubectl logs <pod-name> -n capstone-app`
   - Common cause: Database not ready
   - Solution: Increase `initialDelaySeconds` in probes

3. **Pending state**
   - Insufficient resources
   - Check: `kubectl describe pod <pod-name> -n capstone-app`

### Database Issues

**Cannot connect to PostgreSQL:**
```bash
# Check PostgreSQL pod
kubectl get pods -n capstone-app | grep postgres

# Check PostgreSQL logs
kubectl logs postgres-0 -n capstone-app

# Test connection
kubectl exec -it postgres-0 -n capstone-app -- psql -U admin -d capstone
```

**Reset database:**
```bash
# Delete StatefulSet (keeps PVC)
kubectl delete statefulset postgres -n capstone-app

# Delete PVC (deletes data)
kubectl delete pvc postgres-storage-postgres-0 -n capstone-app

# Redeploy
kubectl apply -f k8s/postgres-statefulset.yaml
```

### Jenkins Issues

**Pipeline fails:**
1. Check Jenkins logs
2. Verify Docker is accessible
3. Verify Kubernetes config
4. Check credentials

**Build fails:**
```bash
# Check Maven build locally
mvn clean install

# Check Docker build
docker build -t test .
```

### Dashboard Issues

**Dashboard not loading:**
1. Check service: `kubectl get svc -n capstone-app`
2. Verify NodePort: Should be 30080
3. Test health: `curl http://localhost:30080/actuator/health`

**No data showing:**
1. Check database connection
2. Run a Jenkins build to populate data
3. Check API endpoints: `curl http://localhost:30080/api/analytics/builds/statistics`

---

## Useful Commands Cheat Sheet

### Maven
```bash
mvn clean install              # Build project
mvn spring-boot:run            # Run locally
mvn test                       # Run tests
```

### Docker
```bash
docker build -t image:tag .    # Build image
docker run -p 8082:8082 image  # Run container
docker ps                      # List containers
docker logs container-name     # View logs
docker stop container-name     # Stop container
```

### Kubernetes
```bash
kubectl get pods -n namespace          # List pods
kubectl logs -f pod-name -n namespace  # View logs
kubectl describe pod pod-name          # Pod details
kubectl delete pod pod-name            # Delete pod
kubectl apply -f file.yaml             # Apply manifest
```

### Jenkins
```bash
# Restart Jenkins
docker restart jenkins

# View Jenkins logs
docker logs -f jenkins
```

---

## Production Deployment Checklist

- [ ] Use strong passwords for database
- [ ] Enable HTTPS/TLS
- [ ] Configure resource limits
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategy
- [ ] Implement network policies
- [ ] Enable authentication on dashboard
- [ ] Use private Docker registry
- [ ] Configure log aggregation
- [ ] Set up disaster recovery

---

**For architecture details, see [ARCHITECTURE.md](ARCHITECTURE.md)**
**For API documentation, see [API_DOCUMENTATION.md](API_DOCUMENTATION.md)**
