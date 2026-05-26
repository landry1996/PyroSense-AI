# PyroSense AI Platform

**Predictive Electrical Fire Prevention System** - A microservices platform that uses IoT sensors, statistical analysis, and AI to detect early signs of electrical faults before they cause fires.

> **Status:** MVP / Pedagogical project — No real hardware sensors. Uses simulated data.
> Not certified for production electrical monitoring (IEC 61439, NF C 15-100).

## Architecture

PyroSense follows a **microservices architecture** with each service implementing **hexagonal architecture** (ports & adapters) and **DDD tactical patterns**.

```
pyrosense-ai-platform/
├── pyrosense-shared-kernel/           # Shared domain primitives (Value Objects, IDs, Events)
├── pyrosense-api-gateway/             # Single entry point (port 8080)
├── pyrosense-identity-service/        # Auth & tenant management (port 8081)
├── pyrosense-device-service/          # Device lifecycle & provisioning (port 8082)
├── pyrosense-ingestion-service/       # IoT telemetry reception via MQTT (port 8083)
├── pyrosense-signal-analysis-service/ # Baseline learning & anomaly detection (port 8084)
├── pyrosense-risk-scoring-service/    # Composite risk score calculation (port 8085)
├── pyrosense-alerting-service/        # Alert lifecycle & deduplication (port 8086)
├── pyrosense-notification-service/    # Multi-channel notifications (port 8087)
├── pyrosense-reporting-service/       # Dashboards & compliance reports (port 8091)
├── pyrosense-maintenance-service/     # Predictive maintenance scheduling (port 8089)
├── scripts/                           # Local dev scripts (start, stop, reset)
├── infra/                             # Infrastructure configs
└── docs/                              # Full project documentation
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| API Gateway | Spring Cloud Gateway |
| Messaging | Apache Kafka (KRaft mode), MQTT (Mosquitto) |
| Database | PostgreSQL 16 + TimescaleDB |
| Cache | Redis 7 |
| Auth | Keycloak 24 (OAuth2/OIDC) |
| Observability | Prometheus + Grafana + Loki + OpenTelemetry |
| Containers | Docker + Docker Compose v2 |
| Build | Maven 3.9+ (multi-module) |
| Quality | JaCoCo, ArchUnit, Spotless, OWASP Dependency-Check |

## Prerequisites

- Java 21+ (Eclipse Temurin recommended)
- Maven 3.9+
- Docker + Docker Compose v2
- 8 GB RAM minimum (for full stack)

## Quick Start

### Option A: Using Scripts (Recommended)

```bash
# Start infrastructure only (PostgreSQL, Redis, Kafka, Mosquitto, Keycloak, monitoring)
./scripts/start-local.sh

# Or start everything including services
./scripts/start-local.sh --profile full
```

### Option B: Manual Docker Compose

```bash
# Infrastructure only
docker compose up -d

# Infrastructure + all services
docker compose --profile full up -d

# Infrastructure + services (without simulator)
docker compose --profile services up -d
```

### Build & Run from IDE

```bash
# Build all modules
mvn clean install -DskipTests

# Run a single service with local profile
cd pyrosense-api-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Stop & Reset

```bash
# Graceful stop
./scripts/stop-local.sh

# Full reset (removes all data volumes)
./scripts/reset-local.sh
```

## Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | JWT required |
| Keycloak Admin | http://localhost:8180 | admin / admin_local_dev |
| Grafana | http://localhost:3000 | admin / admin_local_dev |
| Prometheus | http://localhost:9090 | - |
| Kafka (external) | localhost:29092 | - |
| MQTT Broker | localhost:1884 | - |

## Docker Compose Profiles

| Profile | What it starts |
|---------|---------------|
| _(default)_ | Infrastructure only (DB, Redis, Kafka, MQTT, Keycloak, monitoring) |
| `services` | Infrastructure + all Spring Boot services |
| `simulator` | Infrastructure + IoT device simulator |
| `full` | Infrastructure + services + simulator |

## Maven Profiles

| Profile | Usage |
|---------|-------|
| `local` | Development (default, DEBUG logging) |
| `test` | Integration tests with Testcontainers |
| `docker` | Docker Compose deployment (Docker DNS) |
| `prod` | Production (OWASP check, optimized) |

```bash
# Unit tests only
mvn test -B

# Integration tests (requires Docker for Testcontainers)
mvn verify -Ptest

# Build Docker images
mvn spring-boot:build-image -Pdocker

# OWASP dependency vulnerability check
mvn verify -Pprod
```

## Quality Gates

| Tool | Threshold | Scope |
|------|-----------|-------|
| JaCoCo | 90% domain, 85% application | Per-layer coverage |
| Spotless | Palantir Java Format 2.47.0 | All sources |
| ArchUnit | 100% compliance | Hexagonal rules, naming, no cycles |
| OWASP Dependency-Check | CVSS < 7 | All dependencies |

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

## MVP 2 — Dashboard, Maintenance, Reporting, Notifications

Le MVP 2 ajoute les services orientes utilisateur : dashboard temps reel, interventions de maintenance, generation de rapports PDF, et notifications multi-canal.

### Demarrage rapide MVP 2

```bash
# Tout le MVP 2 (build + run)
./scripts/start-mvp2-local.sh

# Sans rebuild
./scripts/start-mvp2-local.sh --no-build

# Infrastructure seule (services dans IDE)
./scripts/start-mvp2-local.sh --infra-only
```

### Endpoints principaux

| Endpoint | URL | Description |
|----------|-----|-------------|
| Frontend Dashboard | http://localhost:4200 | Interface Angular |
| API Gateway | http://localhost:8080/api/v1/ | Point d'entree REST |
| Dashboard overview | GET /api/v1/dashboard/overview | KPI et stats |
| Alertes | GET /api/v1/alerts | Liste alertes (filtres severity/status) |
| Interventions | GET /api/v1/interventions | Liste interventions |
| Interventions kanban | GET /api/v1/interventions/kanban | Vue kanban 5 colonnes |
| Rapports | POST /api/v1/reports/monthly-health | Generer bilan mensuel |
| Notifications | GET /api/v1/notifications | Historique notifications |
| Preferences | GET /api/v1/notification-preferences/me | Preferences utilisateur |

### Comptes de test (Keycloak)

> Keycloak : http://localhost:8180 — admin / `admin_local_dev`

Aucun utilisateur pre-configure dans ce prototype. Pour tester les endpoints protege :
1. Creer un realm `pyrosense` dans Keycloak
2. Ajouter un client `pyrosense-frontend` (public, PKCE)
3. Creer un utilisateur et lui assigner un role (`TENANT_ADMIN`, `PROPERTY_MANAGER`, `ELECTRICIAN`)
4. Obtenir un JWT via le flow Authorization Code
5. Passer le JWT dans le header `Authorization: Bearer <token>`

Alternativement, en developpement local avec le profil `test`, les endpoints peuvent etre accedes sans JWT si la SecurityConfig est configuree en mode permissif.

### Profil Docker `mvp2`

| Service | Port | Role |
|---------|:----:|------|
| dashboard-frontend | 4200 | SPA Angular (nginx) |
| api-gateway | 8080 | Routage, JWT, CORS |
| dashboard-service | 8088 | Read models agreg |
| alerting-service | 8086 | Cycle de vie alertes |
| maintenance-service | 8089 | Interventions |
| reporting-service | 8091 | Rapports PDF |
| notification-service | 8087 | Notifications multi-canal |

### Documentation MVP 2

| Document | Contenu |
|----------|---------|
| [MVP 2 Overview](docs/mvp2-overview.md) | Objectifs, perimetre, hors perimetre, dependances |
| [Dashboard API](docs/dashboard-api.md) | Endpoints, roles, cache |
| [Maintenance](docs/maintenance-service.md) | Cycle de vie, SLA, workflow alerte → intervention |
| [Reporting](docs/reporting-service.md) | Types rapports, PDF, telechargement securise |
| [Notifications](docs/notification-service.md) | Canaux, templates, preferences, anti-spam |
| [Securite MVP 2](docs/mvp2-security.md) | RBAC, tenant isolation, audit |
| [Evenements Kafka](docs/mvp2-events.md) | Topics, events, versioning, idempotence |
| [Tests MVP 2](docs/mvp2-testing-strategy.md) | Strategie, commandes, couverture |
| [DevOps MVP 2](docs/mvp2-local-run.md) | Docker, variables, demarrage local |
| [Observabilite](docs/mvp2-observability.md) | Metriques, tracing, alertes Prometheus |
| [Production Readiness](docs/production-readiness-mvp2.md) | Checklist avant pilote reel |

## Remote Debugging (Docker)

All services expose JDWP debug ports (via `docker-compose.override.yml`):

| Service | Debug Port |
|---------|-----------|
| API Gateway | 5010 |
| Identity | 5011 |
| Device | 5012 |
| Ingestion | 5013 |
| Signal Analysis | 5014 |
| Risk Scoring | 5015 |
| Alerting | 5016 |
| Notification | 5017 |
| Maintenance | 5018 |
| Reporting | 5019 |

## Documentation

### Architecture & Design

| Document | Description |
|----------|-------------|
| [Architecture](docs/architecture.md) | C4 diagrams, hexagonal architecture, bounded contexts, ADRs |
| [Domain Model](docs/domain-model.md) | Aggregates, entities, value objects, domain events, business rules |
| [API Documentation](docs/api-documentation.md) | REST endpoints, request/response examples, error codes |
| [IoT Protocol](docs/iot-protocol.md) | MQTT topics, payloads, device authentication, heartbeat |
| [Security](docs/security.md) | STRIDE threat model, RBAC, JWT, tenant isolation, secrets |
| [ML Strategy](docs/ml-strategy.md) | Statistical MVP, ML roadmap, limitations, feedback loop |

### Operations & Quality

| Document | Description |
|----------|-------------|
| [DevOps](docs/devops.md) | Docker, CI/CD, Kubernetes-ready, observability, backup |
| [Testing Strategy](docs/testing-strategy.md) | Test pyramid, tools, coverage targets, how to run |
| [Local Development](docs/local-dev.md) | Docker setup, profiles, debugging, troubleshooting |
| [Observability](docs/observability.md) | Metrics, tracing, logging, Prometheus alerts, Grafana |
| [Production Readiness](docs/production-readiness-checklist.md) | Checklist with maturity levels (MVP/Pilot/Production) |

### MVP 2

| Document | Description |
|----------|-------------|
| [MVP 2 Overview](docs/mvp2-overview.md) | Objectives, scope, out-of-scope, MVP 1 dependencies |
| [Events (Kafka)](docs/mvp2-events.md) | Topics, event catalog, versioning, idempotence |
| [Security MVP 2](docs/mvp2-security.md) | RBAC per service, rate limiting, anti mass-assignment |
| [Testing MVP 2](docs/mvp2-testing-strategy.md) | Test strategy, 863+ backend tests, coverage targets |
| [DevOps MVP 2](docs/mvp2-local-run.md) | Docker Compose mvp2 profile, scripts, troubleshooting |
| [Observability MVP 2](docs/mvp2-observability.md) | Business metrics, Prometheus alerts, tracing |
| [Production Readiness MVP 2](docs/production-readiness-mvp2.md) | Pre-pilot checklist, risks, limitations |

### MVP 3 & Pilot

| Document | Description |
|----------|-------------|
| [MVP 3 Transition](docs/mvp3-transition.md) | Full transition plan: what's ready, what's missing, protocols, feedback loop, roadmap |
| [Pilot Transition Plan](docs/pilot-transition-plan.md) | Hardware, certification, cloud, 12-month roadmap |
| [Audit MVP 2](docs/audit-mvp2.md) | Security audit findings, corrections applied, verification results |

### Guides

| Document | Description |
|----------|-------------|
| [Pedagogical Guide](docs/pedagogical-guide.md) | Non-technical explanation, analogies, data journey, ROI |

## Project Constraints

- Pure Java domain (no Spring annotations in domain layer)
- No anemic domain model (rich entities with behavior)
- No business logic in controllers
- No secrets in repository
- Multi-tenant isolation at every layer
- All architectural decisions justified (see ADRs in docs/architecture.md)

## Limitations

- **No real hardware:** All sensor data is simulated via the IoT simulator
- **No electrical certification:** Not certified IEC 61439 or NF C 15-100
- **No production ML:** Currently uses statistical methods (Welford's algorithm); ML integration is planned
- **MVP scope:** Single-region, no HA, no disaster recovery yet
