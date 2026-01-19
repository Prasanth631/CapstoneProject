# Kubernetes Documentation

## Table of Contents
- [Why Kubernetes?](#why-kubernetes)
- [Core Concepts](#core-concepts)
- [Our Kubernetes Resources](#our-kubernetes-resources)
- [How Containers Communicate](#how-containers-communicate)
- [Deployment Strategy](#deployment-strategy)
- [Scaling & High Availability](#scaling--high-availability)

---

## Why Kubernetes?

### Problems Kubernetes Solves

**1. Manual Container Management**
```
Without Kubernetes:
- Manually start containers on servers
- Manually restart if they crash
- Manually distribute load
- Manually update versions
```

**With Kubernetes:**
```
- Automatically schedules containers
- Automatically restarts failed containers
- Automatically load balances
- Automatically rolls out updates
```

**2. Scaling Challenges**
```
Traffic increases → Need more containers
How to:
- Distribute them across servers?
- Balance load between them?
- Update them without downtime?
```

**Kubernetes Solution:** Automatic scaling and load balancing

### Benefits for This Project

| Feature | Benefit |
|---------|---------|
| **Self-Healing** | Restarts crashed pods automatically |
| **Load Balancing** | Distributes traffic across pods |
| **Rolling Updates** | Zero-downtime deployments |
| **Service Discovery** | Pods find each other via DNS |
| **Persistent Storage** | Data survives pod restarts |
| **Secrets Management** | Secure credential handling |

---

## Core Concepts

### 1. Pod
```
┌─────────────────────────────┐
│          Pod                │
│  ┌─────────────────────┐   │
│  │   Container 1       │   │
│  │  (Spring Boot App)  │   │
│  └─────────────────────┘   │
│                             │
│  Shared:                    │
│  - Network namespace        │
│  - Storage volumes          │
│  - IP address               │
└─────────────────────────────┘
```

**What**: Smallest deployable unit in Kubernetes
**Contains**: One or more containers
**Lifecycle**: Created, runs, terminates
**IP Address**: Each pod gets unique IP

### 2. Service
```
┌─────────────────────────────┐
│        Service              │
│  (Stable IP & DNS)          │
└─────────────────────────────┘
            │
            ├──► Pod 1
            ├──► Pod 2
            └──► Pod 3
```

**What**: Stable network endpoint for pods
**Why**: Pod IPs change, Service IP doesn't
**Types**:
- **ClusterIP**: Internal only (default)
- **NodePort**: External access via node port
- **LoadBalancer**: Cloud load balancer

### 3. Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: capstone-deployment
spec:
  replicas: 2  # Number of pods
  template:
    # Pod definition
```

**What**: Manages pod replicas
**Features**:
- Ensures desired number of pods running
- Handles rolling updates
- Enables rollback

### 4. StatefulSet
```
┌─────────────────────────────┐
│    StatefulSet              │
│  (For stateful apps)        │
└─────────────────────────────┘
            │
            ├──► postgres-0 (stable identity)
            ├──► postgres-1
            └──► postgres-2
```

**What**: Like Deployment, but for stateful apps
**Use Case**: Databases (PostgreSQL)
**Features**:
- Stable network identities
- Ordered deployment/scaling
- Persistent storage per pod

### 5. ConfigMap & Secret
```
ConfigMap (non-sensitive)     Secret (sensitive)
├── APPLICATION_NAME          ├── DB_PASSWORD
├── SERVER_PORT               ├── JENKINS_TOKEN
└── LOG_LEVEL                 └── API_KEYS
```

**ConfigMap**: Non-sensitive configuration
**Secret**: Sensitive data (base64 encoded)

### 6. PersistentVolume (PV) & PersistentVolumeClaim (PVC)
```
PersistentVolume              PersistentVolumeClaim
(Storage resource)            (Storage request)
┌──────────────┐              ┌──────────────┐
│   1GB SSD    │ ◄────────────│ Need 1GB     │
└──────────────┘              └──────────────┘
```

**PV**: Actual storage (disk)
**PVC**: Request for storage
**Binding**: K8s matches PVC to PV

---

## Our Kubernetes Resources

### Application Resources

**1. Deployment** (`k8s/deployment.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: capstone-deployment
  namespace: capstone-app
spec:
  replicas: 2  # 2 pods for high availability
  selector:
    matchLabels:
      app: capstone
  template:
    metadata:
      labels:
        app: capstone
    spec:
      containers:
      - name: capstone-container
        image: prasanth631/capstone_pro:latest
        ports:
        - containerPort: 8082
        env:
          - name: JENKINS_URL
            value: "http://host.docker.internal:8080"
          - name: DB_HOST
            value: "postgres.capstone-app.svc.cluster.local"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 20
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8082
          initialDelaySeconds: 60
```

**Key Features:**
- **Replicas**: 2 pods for redundancy
- **Resource Limits**: Prevents resource hogging
- **Health Probes**: Auto-restart if unhealthy
- **Environment Variables**: Configuration injection

**2. Service** (`k8s/service.yaml`)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: capstone-service
  namespace: capstone-app
spec:
  type: NodePort
  selector:
    app: capstone
  ports:
  - port: 8082        # Service port
    targetPort: 8082  # Container port
    nodePort: 30080   # External access port
```

**Service Types:**
- **NodePort**: Accessible at `http://localhost:30080`
- **Port Mapping**: 30080 (external) → 8082 (container)

### Database Resources

**3. StatefulSet** (`k8s/postgres-statefulset.yaml`)
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: capstone-app
spec:
  serviceName: postgres
  replicas: 1
  template:
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        env:
          - name: POSTGRES_DB
            valueFrom:
              secretKeyRef:
                name: postgres-secret
                key: database
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 1Gi
```

**Why StatefulSet?**
- **Stable Identity**: Always `postgres-0`
- **Persistent Storage**: Data survives restarts
- **Ordered Operations**: Predictable behavior

**4. Headless Service** (`k8s/postgres-service.yaml`)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: capstone-app
spec:
  clusterIP: None  # Headless service
  selector:
    app: postgres
  ports:
  - port: 5432
```

**Why Headless?**
- StatefulSets need stable DNS
- DNS: `postgres-0.postgres.capstone-app.svc.cluster.local`

### Configuration Resources

**5. ConfigMap** (`k8s/configmap.yaml`)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: capstone-config
  namespace: capstone-app
data:
  APPLICATION_NAME: "CapstoneProject"
  SERVER_PORT: "8082"
  SPRING_PROFILES_ACTIVE: "prod"
```

**6. Secrets** (`k8s/secrets.yaml`, `k8s/postgres-secret.yaml`)
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jenkins-credentials
  namespace: capstone-app
type: Opaque
stringData:
  username: root
  token: your_token_here
```

---

## How Containers Communicate

### 1. Pod-to-Pod Communication
```
Application Pod                PostgreSQL Pod
┌──────────────┐              ┌──────────────┐
│  10.1.0.5    │              │  10.1.0.8    │
└──────────────┘              └──────────────┘
       │                             │
       └─────────────────────────────┘
         Direct IP communication
```

**Method**: Direct IP (all pods in same network)
**No NAT**: Pods see each other's real IPs

### 2. Service Discovery via DNS
```
Application needs PostgreSQL
    │
    ├─► DNS Lookup: postgres.capstone-app.svc.cluster.local
    │
    ├─► DNS Server returns: Service ClusterIP (10.96.0.10)
    │
    ├─► Service forwards to: Pod IP (10.1.0.8)
    │
    └─► Connection established
```

**DNS Format**: `{service}.{namespace}.svc.cluster.local`

**Example in Code:**
```java
// application.properties
spring.datasource.url=jdbc:postgresql://postgres.capstone-app.svc.cluster.local:5432/capstone
```

### 3. External Access
```
User Browser
    │
    ├─► http://localhost:30080
    │
    ├─► NodePort Service (30080)
    │
    ├─► Forwards to Pod (8082)
    │
    └─► Application responds
```

**Port Mapping**: 30080 (NodePort) → 8082 (Container)

### 4. Network Policies (Optional)
```yaml
# Allow only app pods to access database
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: postgres-network-policy
spec:
  podSelector:
    matchLabels:
      app: postgres
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: capstone
```

---

## Deployment Strategy

### Rolling Update
```
Initial State:        Update Triggered:      Final State:
┌────┐ ┌────┐        ┌────┐ ┌────┐         ┌────┐ ┌────┐
│ v1 │ │ v1 │   →    │ v1 │ │ v2 │    →    │ v2 │ │ v2 │
└────┘ └────┘        └────┘ └────┘         └────┘ └────┘
```

**Process:**
1. Create new pod with v2
2. Wait for readiness probe
3. Terminate old pod with v1
4. Repeat for all pods

**Zero Downtime**: Always have running pods

### Rollback
```bash
# View rollout history
kubectl rollout history deployment/capstone-deployment -n capstone-app

# Rollback to previous version
kubectl rollout undo deployment/capstone-deployment -n capstone-app
```

---

## Scaling & High Availability

### Manual Scaling
```bash
# Scale to 3 replicas
kubectl scale deployment/capstone-deployment --replicas=3 -n capstone-app
```

### Horizontal Pod Autoscaler (HPA)
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: capstone-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: capstone-deployment
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**Auto-scales based on CPU usage**

### High Availability
```
Multiple Pods + Load Balancing
┌────────────────────────────┐
│      Service (LB)          │
└────────────────────────────┘
       │       │       │
       ▼       ▼       ▼
    ┌────┐ ┌────┐ ┌────┐
    │Pod1│ │Pod2│ │Pod3│
    └────┘ └────┘ └────┘
```

**Benefits:**
- If one pod crashes, others handle traffic
- Load distributed across pods
- Rolling updates without downtime

---

## Useful Commands

### Deployment
```bash
# Apply all manifests
kubectl apply -f k8s/ -n capstone-app

# Check deployment status
kubectl get deployments -n capstone-app

# View rollout status
kubectl rollout status deployment/capstone-deployment -n capstone-app
```

### Pods
```bash
# List pods
kubectl get pods -n capstone-app

# View pod logs
kubectl logs -f pod-name -n capstone-app

# Execute command in pod
kubectl exec -it pod-name -n capstone-app -- /bin/bash

# Describe pod (detailed info)
kubectl describe pod pod-name -n capstone-app
```

### Services
```bash
# List services
kubectl get services -n capstone-app

# Test service connectivity
kubectl run test --rm -it --image=busybox -n capstone-app -- sh
# Inside pod: wget -O- http://capstone-service:8082/actuator/health
```

### Debugging
```bash
# View events
kubectl get events -n capstone-app --sort-by='.lastTimestamp'

# Check resource usage
kubectl top pods -n capstone-app

# Port forward for local access
kubectl port-forward svc/capstone-service 8082:8082 -n capstone-app
```

---

**For Docker details, see [DOCKER.md](DOCKER.md)**
**For architecture, see [ARCHITECTURE.md](ARCHITECTURE.md)**
