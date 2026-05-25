# PyroSense AI Platform - DevOps & Deployment

## 1. Container Strategy

### Docker Images
Each service produces a Docker image via Spring Boot Buildpacks:

```bash
mvn spring-boot:build-image -Pdocker -pl pyrosense-device-service
```

Image naming convention: `pyrosense/{service-name}:{version}`

### Base Image
- Spring Boot Buildpacks (Paketo)
- JDK 21 (Eclipse Temurin)
- Non-root user
- ZGC garbage collector

## 2. Environment Profiles

| Profile | Spring Profile | Usage |
|---------|---------------|-------|
| Local | `local` | Developer machine, Docker infra |
| Test | `test` | CI pipeline, Testcontainers |
| Docker | `docker` | Docker Compose full stack |
| Production | `prod` | Production deployment |

## 3. Docker Compose (Development)

```bash
# Start all infrastructure
docker compose up -d

# Start specific services
docker compose up -d postgres kafka redis mosquitto

# View logs
docker compose logs -f pyrosense-kafka
```

### Services Provided
| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL + TimescaleDB | 5432 | Persistence (9 databases) |
| Redis | 6379 | Caching |
| Kafka (KRaft) | 9092 | Event bus |
| Mosquitto | 1883, 9001 | MQTT broker |
| Keycloak | 8180 | Identity provider |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Dashboards |
| Loki | 3100 | Log aggregation |

## 4. CI/CD Pipeline (GitHub Actions)

```yaml
# Stages:
# 1. Build & Unit Test
# 2. Integration Test (Testcontainers)
# 3. Code Quality (Spotless, JaCoCo, OWASP)
# 4. Docker Image Build
# 5. Push to Registry
# 6. Deploy (staging/production)
```

### Quality Gates
| Gate | Tool | Threshold |
|------|------|-----------|
| Code Format | Spotless | Must pass |
| Unit Tests | Surefire | Must pass |
| Integration Tests | Failsafe | Must pass |
| Code Coverage | JaCoCo | >= 80% |
| Vulnerabilities | OWASP | CVSS < 7 |
| Architecture | ArchUnit | 100% |

## 5. Observability Stack

### Metrics (Prometheus + Micrometer)
- JVM metrics (heap, GC, threads)
- HTTP request metrics (rate, latency, errors)
- Kafka consumer lag
- Custom business metrics (signals ingested, anomalies detected, alerts created)

### Logging (Loki + Logback)
- JSON format in production
- Correlation via X-Request-Id (MDC)
- Structured fields: service, traceId, spanId

### Dashboards (Grafana)
- Service health overview
- Signal ingestion rate
- Risk score distribution
- Alert frequency
- Kafka consumer lag

### Alerting (Prometheus Alertmanager)
- ServiceDown (any service unreachable > 1min)
- HighMemoryUsage (heap > 85% for 5min)
- NoSignalsIngested (0 signals for 10min)
- HighRiskScore (score > 80 for 2min)

## 6. Database Migration Strategy

- **Tool**: Flyway
- **Convention**: `V{NNN}__{description}.sql`
- **Per-service**: Each service manages its own migrations
- **TimescaleDB**: Extension enabled via init script

## 7. Scaling Strategy

| Service | Stateless | Scalable | Notes |
|---------|-----------|----------|-------|
| API Gateway | Yes | Horizontal | Load balancer in front |
| Identity | Yes | Horizontal | Keycloak handles sessions |
| Device | Yes | Horizontal | |
| Ingestion | Yes | Horizontal | Kafka partitioning |
| Signal Analysis | Stateful* | Vertical | Baseline state in Redis |
| Risk Scoring | Yes | Horizontal | Cache in Redis |
| Alerting | Yes | Horizontal | |
| Notification | Yes | Horizontal | |
| Reporting | Yes | Horizontal | |
| Maintenance | Yes | Horizontal | |

## 8. Disaster Recovery

| Component | Strategy | RPO | RTO |
|-----------|----------|-----|-----|
| PostgreSQL | Streaming replication + WAL archiving | 0 | < 5min |
| Kafka | Topic replication (RF=3 in prod) | 0 | < 1min |
| Redis | AOF persistence + replica | < 1min | < 1min |
| Services | Stateless, restart policy: always | N/A | < 30s |
