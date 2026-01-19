# Docker Documentation

## Table of Contents
- [Why Docker?](#why-docker)
- [How Docker Works](#how-docker-works)
- [Dockerfile Explanation](#dockerfile-explanation)
- [Building Images](#building-images)
- [Running Containers](#running-containers)
- [Best Practices](#best-practices)

---

## Why Docker?

### Problems Docker Solves

**1. "It Works on My Machine" Problem**
```
Developer's Machine  ≠  Test Server  ≠  Production Server
   (Windows)            (Linux)          (Linux)
```

**Solution**: Docker ensures **identical environments** everywhere.

**2. Dependency Hell**
- Different Java versions
- Different library versions
- OS-specific dependencies

**Solution**: Everything packaged in one container.

**3. Resource Efficiency**
```
Virtual Machines          vs          Docker Containers
┌──────────────┐                    ┌──────────────┐
│     App      │                    │     App      │
│  Libraries   │                    │  Libraries   │
├──────────────┤                    ├──────────────┤
│  Guest OS    │                    │ Docker Engine│
├──────────────┤                    ├──────────────┤
│  Hypervisor  │                    │   Host OS    │
├──────────────┤                    ├──────────────┤
│   Host OS    │                    │   Hardware   │
└──────────────┘                    └──────────────┘
  ~GB of RAM                          ~MB of RAM
  Slow startup                        Fast startup
```

### Benefits for This Project

| Benefit | Impact |
|---------|--------|
| **Consistency** | Same environment in dev, test, prod |
| **Portability** | Run anywhere Docker runs |
| **Isolation** | Dependencies don't conflict |
| **Scalability** | Easy to scale with Kubernetes |
| **Speed** | Fast startup (seconds vs minutes) |

---

## How Docker Works

### Core Concepts

**1. Image**
- Read-only template
- Contains app code + dependencies
- Built from Dockerfile
- Stored in registry (Docker Hub)

**2. Container**
- Running instance of an image
- Isolated process
- Has its own filesystem
- Can be started/stopped/deleted

**3. Dockerfile**
- Text file with instructions
- Defines how to build an image
- Layer-based (cached for speed)

### Docker Architecture
```
┌─────────────────────────────────────┐
│         Docker Client               │
│  (docker build, docker run)         │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│       Docker Daemon                 │
│  - Builds images                    │
│  - Runs containers                  │
│  - Manages volumes                  │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│       Docker Registry               │
│  (Docker Hub, private registry)     │
└─────────────────────────────────────┘
```

---

## Dockerfile Explanation

### Our Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
```

### Line-by-Line Breakdown

**Line 1: `FROM openjdk:21-jdk-slim`**
```
What: Base image with Java 21
Why:  Our app needs Java to run
Slim: Smaller image size (~200MB vs ~400MB)
```

**Line 2: `WORKDIR /app`**
```
What: Sets working directory to /app
Why:  All subsequent commands run in /app
Creates /app if it doesn't exist
```

**Line 3: `COPY target/*.jar app.jar`**
```
What: Copies JAR file from host to container
From: target/ directory (Maven build output)
To:   /app/app.jar in container
```

**Line 4: `EXPOSE 8082`**
```
What: Documents that container listens on port 8082
Note: Doesn't actually publish the port
      (use -p flag when running)
```

**Line 5: `CMD ["java", "-jar", "app.jar"]`**
```
What: Command to run when container starts
Runs: java -jar app.jar
Can be overridden at runtime
```

### Image Layers
```
Layer 5: CMD ["java", "-jar", "app.jar"]     ← ~0 bytes
Layer 4: EXPOSE 8082                          ← ~0 bytes
Layer 3: COPY target/*.jar app.jar            ← ~50 MB
Layer 2: WORKDIR /app                         ← ~0 bytes
Layer 1: FROM openjdk:21-jdk-slim             ← ~200 MB
─────────────────────────────────────────────────────────
Total:                                        ~250 MB
```

**Layer Caching**: If base image doesn't change, Docker reuses it!

---

## Building Images

### Build Command
```bash
docker build -t prasanth631/capstone_pro:latest .
```

**Breakdown:**
- `docker build` - Build an image
- `-t prasanth631/capstone_pro:latest` - Tag (name) the image
- `.` - Build context (current directory)

### Build Process
```
Step 1/5 : FROM openjdk:21-jdk-slim
 ---> Pulling from library/openjdk
 ---> Downloaded (200 MB)

Step 2/5 : WORKDIR /app
 ---> Running in abc123...
 ---> Created directory

Step 3/5 : COPY target/*.jar app.jar
 ---> Copying file (50 MB)

Step 4/5 : EXPOSE 8082
 ---> Documented port

Step 5/5 : CMD ["java", "-jar", "app.jar"]
 ---> Set entrypoint

Successfully built def456
Successfully tagged prasanth631/capstone_pro:latest
```

### Push to Docker Hub
```bash
# Login
docker login

# Push image
docker push prasanth631/capstone_pro:latest
```

---

## Running Containers

### Basic Run
```bash
docker run -p 8082:8082 prasanth631/capstone_pro:latest
```

**Flags:**
- `-p 8082:8082` - Port mapping (host:container)
- Image name at the end

### Run with Environment Variables
```bash
docker run -p 8082:8082 \
  -e JENKINS_URL=http://host.docker.internal:8080 \
  -e JENKINS_USER=root \
  -e JENKINS_TOKEN=your_token \
  -e DB_HOST=postgres \
  -e DB_NAME=capstone \
  -e DB_USER=admin \
  -e DB_PASSWORD=password \
  prasanth631/capstone_pro:latest
```

### Run in Background (Detached)
```bash
docker run -d -p 8082:8082 --name capstone-app \
  prasanth631/capstone_pro:latest
```

**Flags:**
- `-d` - Detached mode (background)
- `--name capstone-app` - Container name

### Useful Commands
```bash
# List running containers
docker ps

# View logs
docker logs capstone-app

# Follow logs (real-time)
docker logs -f capstone-app

# Stop container
docker stop capstone-app

# Remove container
docker rm capstone-app

# Execute command in running container
docker exec -it capstone-app /bin/bash
```

---

## Best Practices

### 1. Use Specific Base Image Tags
```dockerfile
# ❌ Bad - version can change
FROM openjdk:latest

# ✅ Good - specific version
FROM openjdk:21-jdk-slim
```

### 2. Minimize Layers
```dockerfile
# ❌ Bad - multiple RUN commands
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get clean

# ✅ Good - combine commands
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean
```

### 3. Use .dockerignore
```
# .dockerignore
target/
.git/
.idea/
*.md
```

### 4. Multi-Stage Builds (Advanced)
```dockerfile
# Build stage
FROM maven:3.9-openjdk-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
```

**Benefits:**
- Smaller final image (no Maven)
- Faster builds (cached layers)
- More secure (fewer tools in production)

### 5. Health Checks
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8082/actuator/health || exit 1
```

### 6. Non-Root User
```dockerfile
# Create user
RUN addgroup --system appgroup && \
    adduser --system --ingroup appgroup appuser

# Switch to user
USER appuser
```

---

## Docker in This Project

### Build Process
```
1. Maven builds JAR file
   └─► target/CapstoneProject-0.0.1-SNAPSHOT.jar

2. Docker builds image
   └─► Copies JAR into image
   └─► Tags as prasanth631/capstone_pro:latest

3. Push to Docker Hub
   └─► Available for Kubernetes to pull
```

### Integration with Kubernetes
```
Jenkinsfile
    │
    ├─► Builds Docker image
    │
    ├─► Pushes to Docker Hub
    │
    └─► Kubernetes pulls image
        └─► Creates pods from image
```

### Environment Variables
```yaml
# In Kubernetes deployment.yaml
env:
  - name: JENKINS_URL
    value: "http://host.docker.internal:8080"
  - name: DB_HOST
    value: "postgres.capstone-app.svc.cluster.local"
```

---

## Troubleshooting

### Image Won't Build
```bash
# Check Dockerfile syntax
docker build --no-cache -t test .

# View build logs
docker build -t test . 2>&1 | tee build.log
```

### Container Won't Start
```bash
# Check logs
docker logs container-name

# Run interactively
docker run -it prasanth631/capstone_pro:latest /bin/bash
```

### Port Already in Use
```bash
# Find process using port
netstat -ano | findstr :8082

# Kill process (Windows)
taskkill /PID <PID> /F
```

---

**For Kubernetes deployment, see [KUBERNETES.md](KUBERNETES.md)**
**For architecture details, see [ARCHITECTURE.md](ARCHITECTURE.md)**
