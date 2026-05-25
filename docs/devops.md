# PyroSense AI Platform - DevOps & Deployment

## 1. Docker

### Multi-Stage Dockerfiles

All services use multi-stage builds for optimized image size:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Non-root user
RUN groupadd -r pyrosense && useradd -r -g pyrosense pyrosense
USER pyrosense:pyrosense

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Container Security

- **Non-root user**: All containers run as `pyrosense:pyrosense` (no root access)
- **Health checks**: Every service container has a `wget` health check against `/actuator/health`
- **Minimal image**: JRE-only runtime (no compiler, no build tools in final image)
- **No shell access needed**: Production images can use distroless base (future optimization)

### .dockerignore

Optimized to exclude build artifacts, IDE files, and documentation:
```
target/
.git/
.idea/
*.iml
.env*
docs/
node_modules/
```

### Docker Compose

**docker-compose.yml** with profiles for flexible local development:

| Profile | Services Included | Use Case |
|---------|-------------------|----------|
| `services` | All infrastructure (PostgreSQL, Kafka, Redis, Keycloak, etc.) | IDE development - run services locally |
| `simulator` | Infrastructure + IoT simulator | Testing data pipeline with simulated devices |
| `full` | All infrastructure + all application services | Full stack integration testing |

```bash
# Infrastructure only (for IDE development)
docker compose --profile services up -d

# Full stack with simulator
docker compose --profile full --profile simulator up -d
```

**docker-compose.override.yml** enables JDWP debug ports for all services:

| Service | Application Port | Debug Port (JDWP) |
|---------|-----------------|-------------------|
| API Gateway | 8080 | 5005 |
| Identity Service | 8081 | 5006 |
| Device Service | 8082 | 5007 |
| Ingestion Service | 8083 | 5008 |
| Signal Analysis | 8084 | 5009 |
| Risk Scoring | 8085 | 5010 |
| Alerting Service | 8086 | 5011 |
| Notification Service | 8087 | 5012 |
| Reporting Service | 8088 | 5013 |
| Maintenance Service | 8089 | 5014 |

---

## 2. CI/CD (GitHub Actions)

### Pipeline Jobs

The CI/CD pipeline consists of 6 jobs:

```
┌─────────────────┐     ┌─────────────────────┐
│ build-and-test  │────▶│ integration-tests   │
└─────────────────┘     └─────────────────────┘
        │                         │
        ▼                         ▼
┌─────────────────┐     ┌─────────────────────┐
│    coverage     │     │   security-scan     │
└─────────────────┘     └─────────────────────┘
        │                         │
        ▼                         ▼
┌─────────────────┐     ┌─────────────────────┐
│  docker-build   │     │   code-quality      │
└─────────────────┘     └─────────────────────┘
```

| Job | Description | Tools |
|-----|-------------|-------|
| `build-and-test` | Compile, unit tests, architecture tests | Maven, Surefire, ArchUnit |
| `integration-tests` | Full integration tests with real dependencies | Testcontainers (PostgreSQL, Kafka, Redis) |
| `coverage` | Coverage report generation and enforcement | JaCoCo (>= 80% threshold) |
| `security-scan` | Dependency vulnerability scanning | OWASP Dependency Check (CVSS < 7) |
| `docker-build` | Build and tag Docker images | Multi-stage Dockerfiles |
| `code-quality` | Format checking and static analysis | Spotless, PMD |

### Pipeline Configuration

- **Concurrency**: `cancel-in-progress: true` for same branch (prevents stale builds)
- **Maven cache**: GitHub Actions cache for `~/.m2/repository` (faster builds)
- **JDK**: Eclipse Temurin 21 (LTS)
- **Coverage comment**: JaCoCo report posted as PR comment on pull requests
- **OWASP report**: Published as build artifact for review

### Quality Gates

| Gate | Tool | Threshold | Blocks Merge |
|------|------|-----------|:---:|
| Code Format | Spotless | Must pass | Yes |
| Unit Tests | Surefire | Must pass | Yes |
| Integration Tests | Failsafe | Must pass | Yes |
| Code Coverage | JaCoCo | >= 80% | Yes |
| Vulnerabilities | OWASP Dependency Check | CVSS < 7 | Yes |
| Architecture Rules | ArchUnit | 100% pass | Yes |

---

## 3. Local Development

> Full details in [docs/local-dev.md](local-dev.md)

### Infrastructure Only (Recommended for IDE Development)

```bash
docker compose --profile services up -d
```

Run services from IDE with `--spring.profiles.active=local` for:
- Hot reload
- Debugger attachment
- Faster iteration

### Full Stack (Integration Testing)

```bash
docker compose --profile full up -d
```

All services running in Docker with debug ports exposed via `docker-compose.override.yml`.

### Debug Ports

Attach your IDE debugger to the JDWP port for any service:

| Service | Debug Port |
|---------|-----------|
| API Gateway | 5005 |
| Identity Service | 5006 |
| Device Service | 5007 |
| Ingestion Service | 5008 |
| Signal Analysis | 5009 |
| Risk Scoring | 5010 |
| Alerting Service | 5011 |
| Notification Service | 5012 |
| Reporting Service | 5013 |
| Maintenance Service | 5014 |

---

## 4. Kubernetes-Ready (Future)

### Health Probes

All services expose Spring Boot Actuator health endpoints suitable for Kubernetes probes:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 3
```

### Resource Requests/Limits Guidance

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit |
|---------|-------------|-----------|----------------|--------------|
| API Gateway | 250m | 500m | 256Mi | 512Mi |
| Identity Service | 250m | 500m | 256Mi | 512Mi |
| Ingestion Service | 500m | 1000m | 512Mi | 1Gi |
| Signal Analysis | 500m | 1000m | 512Mi | 1Gi |
| Risk Scoring | 250m | 500m | 256Mi | 512Mi |
| Alerting Service | 250m | 500m | 256Mi | 512Mi |
| Other services | 200m | 400m | 256Mi | 512Mi |

### Horizontal Pod Autoscaling (HPA)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: External
      external:
        metric:
          name: kafka_consumer_lag
        target:
          type: AverageValue
          averageValue: "1000"
```

- Scale based on CPU utilization (70% threshold)
- Scale based on Kafka consumer lag (for ingestion and analysis services)
- Minimum 2 replicas for high availability

### Secrets Management (Kubernetes)

- **Option A**: Kubernetes Secrets (encrypted at rest with KMS)
- **Option B**: External Secrets Operator (syncs from Vault/AWS Secrets Manager/GCP Secret Manager)

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
  target:
    name: pyrosense-secrets
  data:
    - secretKey: DB_PASSWORD
      remoteRef:
        key: secret/pyrosense/database
        property: password
```

### ConfigMaps

Application configuration per environment:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: pyrosense-config
data:
  application-k8s.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres-service:5432/pyrosense
      kafka:
        bootstrap-servers: kafka-service:9092
    management:
      endpoints:
        web:
          exposure:
            include: health,info,prometheus
```

### Service Mesh (Future)

- **Istio** for mTLS between all services (zero-trust networking)
- Automatic certificate rotation
- Traffic management (canary deployments, circuit breaking)
- Observability (distributed tracing injection)

---

## 5. Observability

### Prometheus

- **Scrape interval**: 15 seconds
- **Alert rules**: 14 rules defined across 4 severity levels
- **Targets**: All application services + infrastructure (PostgreSQL, Kafka, Redis)
- **Storage**: Local TSDB with 15-day retention (configurable)

Metrics exposed by each service:
- JVM metrics (heap, GC, threads, classloading)
- HTTP request metrics (rate, latency percentiles, error rate)
- Kafka consumer lag and throughput
- Custom business metrics (signals ingested, anomalies detected, alerts created, risk scores calculated)

### Grafana

3 dashboards provisioned automatically from configuration files:

| Dashboard | Contents |
|-----------|----------|
| **Service Health** | JVM metrics, HTTP request rate/latency/errors, uptime, circuit breaker state |
| **Signal Pipeline** | Ingestion rate, processing latency, anomaly detection rate, Kafka consumer lag |
| **Business Metrics** | Risk score distribution, alert frequency, device connectivity, tenant activity |

Dashboards are provisioned from JSON files (no manual configuration required, no backup needed).

### Loki

- **Log format**: Structured JSON from all services (logstash-logback-encoder)
- **Labels**: service, environment, level
- **Retention**: 7 days (local development)
- **Integration**: Grafana data source for log correlation with metrics

### OpenTelemetry

- **Protocol**: OTLP HTTP export
- **Distributed tracing**: Full request trace across all services
- **Auto-instrumentation**: Spring Boot, HTTP clients, Kafka, JDBC
- **Trace propagation**: W3C TraceContext headers

### MDC (Mapped Diagnostic Context)

Every log line includes:

| Field | Source | Purpose |
|-------|--------|---------|
| `traceId` | OpenTelemetry | Distributed trace correlation |
| `spanId` | OpenTelemetry | Individual operation within trace |
| `correlationId` | `X-Request-Id` header | Request-level correlation |
| `tenantId` | JWT claim | Tenant-scoped log filtering |

---

## 6. Monitoring & Alerting

### Critical Alerts (Immediate Action Required)

| Alert | Condition | Duration | Severity |
|-------|-----------|----------|----------|
| `ServiceDown` | Service health endpoint unreachable | 1 minute | Critical |
| `KafkaLagCritical` | Consumer lag > 10,000 messages | 5 minutes | Critical |
| `DeviceOfflineCritical` | > 50 devices offline simultaneously | 5 minutes | Critical |
| `CriticalAlertsActive` | > 10 critical fire risk alerts active | Immediate | Critical |

### Warning Alerts (Investigation Required)

| Alert | Condition | Duration | Severity |
|-------|-----------|----------|----------|
| `HighMemoryUsage` | JVM heap > 85% | 5 minutes | Warning |
| `HighErrorRate` | HTTP 5xx rate > 5% | 5 minutes | Warning |
| `HighLatency` | P95 response time > 2 seconds | 5 minutes | Warning |
| `IngestionRejection` | Rejected telemetry > 10% of total | 5 minutes | Warning |
| `NoSignalsIngested` | Zero signals ingested | 10 minutes | Warning |
| `HighRiskScore` | Risk score > 80 sustained | 2 minutes | Warning |
| `CertificateExpiry` | TLS certificate expires within 7 days | N/A | Warning |
| `DiskSpaceWarning` | Disk usage > 80% | 5 minutes | Warning |
| `DatabaseConnectionPoolExhaustion` | Available connections < 2 | 1 minute | Warning |
| `KafkaLagWarning` | Consumer lag > 1,000 messages | 5 minutes | Warning |

---

## 7. Backup Strategy (Planned)

### PostgreSQL

| Strategy | Frequency | Retention | Purpose |
|----------|-----------|-----------|---------|
| `pg_dump` | Daily (02:00 UTC) | 30 days | Full database backup |
| WAL archiving | Continuous | 7 days | Point-in-Time Recovery (PITR) |
| Streaming replication | Real-time | N/A | High availability (read replica) |

### Kafka

| Environment | Topic Retention | Purpose |
|-------------|----------------|---------|
| Local development | 24 hours | Sufficient for development/testing |
| Production | 7 days | Event replay and recovery |

Topic replication factor: RF=3 in production (survives 2 broker failures).

### Redis

- **Strategy**: RDB snapshots (periodic point-in-time snapshots)
- **Data classification**: Cache only (no critical persistent data)
- **Recovery**: If Redis lost, services rebuild cache from database on startup
- **No backup required**: All Redis data is derivable from primary data stores

### Grafana

- **Dashboards**: Provisioned from JSON files in repository (source-controlled)
- **No backup needed**: Re-provisioned automatically on restart
- **Alerting rules**: Defined in Prometheus config files (source-controlled)

---

## 8. Scaling Considerations

### Service Scaling Characteristics

| Service | Scaling Type | Strategy | Bottleneck |
|---------|-------------|----------|------------|
| API Gateway | Horizontal | Stateless, scale freely behind load balancer | Network I/O |
| Ingestion Service | Horizontal | Kafka partitions = parallelism. Add partitions + instances | Kafka throughput |
| Signal Analysis | Horizontal | Scale with Kafka consumer group. State in Redis. | CPU (computation) |
| Risk Scoring | Horizontal | Scale with Kafka consumer group. Cache in Redis. | CPU + Database reads |
| Alerting Service | Horizontal | Event-driven, scale with Kafka consumer group | Database writes |
| Identity Service | Horizontal | Stateless (Keycloak handles sessions) | Database reads |
| All other services | Horizontal | Stateless REST services | Database I/O |

### Database Scaling

- **Write scaling**: Single primary (PostgreSQL does not natively support multi-master)
- **Read scaling**: Read replicas for query-heavy services (Reporting, Risk Scoring)
- **Connection pooling**: HikariCP per service (max 10 connections per instance)
- **TimescaleDB**: Automatic partitioning for time-series data (hypertables)

### Kafka Scaling

- **Partition count**: Determines maximum consumer parallelism
- **Consumer groups**: Each service type is a consumer group (independent scaling)
- **Rebalancing**: Tested with cooperative sticky assignor for minimal disruption
- **Throughput target**: 1,000 messages/second ingestion (MVP milestone)

### Redis Scaling

- **Current**: Single instance sufficient for MVP
- **Cache patterns**: Write-through for risk scores, TTL-based for device status
- **Future**: Redis Cluster if cache size exceeds single node capacity
- **Eviction policy**: `allkeys-lru` (least recently used)

### MVP Target Performance

| Metric | Target |
|--------|--------|
| Ingestion throughput | 1,000 messages/second |
| API response time (P95) | < 200ms |
| Alert generation latency | < 5 seconds (end-to-end) |
| Concurrent devices | 10,000 |
| Concurrent users | 1,000 |
