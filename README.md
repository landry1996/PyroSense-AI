# PyroSense AI Platform

**Predictive Electrical Fire Prevention System** - A microservices platform that uses IoT sensors, statistical analysis, and AI to detect early signs of electrical faults before they cause fires.

## Architecture

PyroSense follows a **microservices architecture** with each service implementing **hexagonal architecture** (ports & adapters) and **DDD tactical patterns**.

```
pyrosense-ai-platform/
├── pyrosense-shared-kernel/          # Shared domain primitives
├── pyrosense-api-gateway/            # Single entry point (port 8080)
├── pyrosense-identity-service/       # Auth & user management (port 8081)
├── pyrosense-device-service/         # Installation & sensor lifecycle (port 8082)
├── pyrosense-ingestion-service/      # IoT signal reception via MQTT (port 8083)
├── pyrosense-signal-analysis-service/# Baseline learning & anomaly detection (port 8084)
├── pyrosense-risk-scoring-service/   # Risk score calculation (port 8085)
├── pyrosense-alerting-service/       # Alert lifecycle management (port 8086)
├── pyrosense-notification-service/   # Multi-channel notifications (port 8087)
├── pyrosense-reporting-service/      # Compliance & reports (port 8088)
├── pyrosense-maintenance-service/    # Predictive maintenance (port 8089)
├── infra/                            # Infrastructure configs
└── docs/                             # Architecture documentation
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.x |
| API Gateway | Spring Cloud Gateway |
| Messaging | Apache Kafka (KRaft), MQTT 5 (Mosquitto) |
| Database | PostgreSQL 16 + TimescaleDB |
| Cache | Redis 7 |
| Auth | Keycloak (OAuth2/OIDC) |
| Observability | Prometheus + Grafana + Loki |
| Containers | Docker / Podman |
| Build | Maven (multi-module) |

## Prerequisites

- Java 21+ (GraalVM or Eclipse Temurin)
- Maven 3.9+
- Docker or Podman
- Docker Compose v2

## Quick Start

### 1. Start Infrastructure

```bash
cp .env.example .env
docker compose up -d
```

This starts: PostgreSQL+TimescaleDB, Redis, Kafka, Mosquitto, Keycloak, Prometheus, Grafana, Loki.

### 2. Build All Services

```bash
mvn clean install -DskipTests
```

### 3. Run Services

```bash
# Run each service (in separate terminals)
cd pyrosense-api-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-identity-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-device-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-ingestion-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-signal-analysis-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-risk-scoring-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-alerting-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-notification-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-reporting-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd pyrosense-maintenance-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Access

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Keycloak Admin | http://localhost:8180 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

## Maven Profiles

| Profile | Usage |
|---------|-------|
| `local` | Development (default, DEBUG logging) |
| `test` | Integration tests with Testcontainers |
| `docker` | Docker image build (Spring Boot Buildpacks) |
| `prod` | Production (OWASP check, optimized) |

```bash
# Run with specific profile
mvn clean install -Ptest

# Build Docker images
mvn spring-boot:build-image -Pdocker

# Run OWASP dependency check
mvn verify -Pprod
```

## Quality Gates

| Tool | Threshold |
|------|-----------|
| JaCoCo | 80% line coverage |
| Spotless | Palantir Java Format |
| OWASP Dependency-Check | CVSS < 7 |
| ArchUnit | 100% architectural compliance |

## Event Flow

```
[IoT Sensors] --MQTT--> [Ingestion Service] --Kafka--> [Signal Analysis]
                                                              |
                                                    Kafka: anomaly.detected
                                                              |
                                                              v
[API Gateway] <--REST-- [Alerting Service] <--Kafka-- [Risk Scoring]
      |                        |
      v                        v (Kafka)
[Frontend SPA]         [Notification Service] --> Email/SMS/Webhook
```

## Documentation

- [Architecture](docs/architecture.md) - Complete architectural framing (C4, DDD, ADRs)
- [Security](docs/security.md) - Authentication, authorization, secrets management
- [DevOps](docs/devops.md) - Deployment, CI/CD, infrastructure
- [Testing Strategy](docs/testing-strategy.md) - Unit, integration, contract tests

## Project Constraints

- Pure Java domain (no Spring annotations in domain layer)
- No anemic domain model (rich entities with behavior)
- No business logic in controllers
- No secrets in repository
- All architectural decisions justified (see ADRs in docs/architecture.md)
