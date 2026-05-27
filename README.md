# PyroSense AI Platform

**Predictive Electrical Fire Prevention System** - A microservices platform that uses IoT sensors, statistical analysis, and AI to detect early signs of electrical faults before they cause fires.

> **Status:** MVP 3 complete — Pilot-ready (pending hardware 24h validation).
> Firmware prototype (ESP32-S3), secured IoT pipeline (HMAC + anti-replay), 900+ tests.
> **NOT CERTIFIED** for production electrical monitoring (IEC 61439, NF C 15-100).
> Any real installation requires a **qualified electrician (B2V minimum)**.

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

## MVP 3 — IoT Real Device Pilot (10 Capteurs)

> **AVERTISSEMENT** : Le MVP 3 prepare un **pilote terrain controle** avec capteurs reels.
> Ce n'est PAS un produit certifie. Toute installation physique sur tableau electrique
> doit etre realisee **exclusivement par un electricien qualifie habilite B2V minimum**.
> PyroSense ne remplace pas un systeme de securite incendie certifie.

### Demarrage rapide MVP 3

```bash
# 1. Lancer l'infrastructure + services backend
docker compose --profile full up -d

# 2. Lancer le broker MQTT (Mosquitto, port 1884)
# Deja inclus dans docker-compose.yml

# 3. Lancer le simulateur (simule capteurs reels via MQTT v1)
cd tools/pyrosense-iot-simulator
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Ou lancer un firmware mock (host build, sans ESP32 physique)
cd firmware/pyrosense-device/test
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
ctest --output-on-failure
```

### Enregistrer un device (provisioning)

```bash
# 1. Creer un claim token (necessite JWT admin)
curl -X POST http://localhost:8080/api/v1/devices/{deviceId}/claim-token \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json"
# Reponse: { "claimToken": "abc123..." } (affiche UNE SEULE fois)

# 2. Provisionner le device (appele par le firmware, pas de JWT requis)
curl -X POST http://localhost:8080/api/v1/devices/provision \
  -H "Content-Type: application/json" \
  -d '{
    "claimToken": "abc123...",
    "serialNumber": "PYRO-S3-001",
    "model": "PyroSense-S3",
    "firmwareVersion": "0.3.1"
  }'
# Reponse: credentials MQTT (affichees UNE SEULE fois, stockage hash uniquement)

# 3. Le device peut maintenant publier sur MQTT:
# Topic: pyrosense/v1/{tenantId}/{deviceId}/telemetry
```

### Verifier la telemetrie

```bash
# Verifier les metriques d'ingestion
curl http://localhost:8083/actuator/prometheus | grep pyrosense_mvp3

# Metriques cles:
# pyrosense_mvp3_real_device_telemetry_received_total  — messages recus
# pyrosense_mvp3_signature_invalid_total               — signatures invalides
# pyrosense_mvp3_replay_detected_total                 — tentatives replay
# pyrosense_mvp3_signal_quality_score                  — qualite signal (0-100)

# Qualite des donnees par device
curl http://localhost:8080/api/v1/devices/quality/devices/{deviceId} \
  -H "Authorization: Bearer $JWT"

# Dashboard technique device
curl http://localhost:8080/api/v1/devices/{deviceId}/technical/health \
  -H "Authorization: Bearer $JWT"
```

### Limites de securite MVP 3

| Protection | Mecanisme | Limite |
|-----------|-----------|--------|
| Authentification device | HMAC-SHA256 par message | Comparaison constant-time |
| Anti-replay | Nonce + messageId + sequence | 3 couches independantes, Redis 24h TTL |
| Rate limiting | Per-device sliding window | 120 msg / 60s (configurable) |
| Payload | Taille max + schema validation | 8 KB, schema v1.0 whitelist |
| Timestamp | Tolerance + drain mode | ±300s normal, 72h en drain |
| Credentials | Hash SHA-256, rotation, revocation | Jamais re-affichees apres creation |
| Provisioning | Claim token single-use, rate limit IP | 10 echecs / IP / 15 min |

**Ce qui n'est PAS encore en place :**

- mTLS X.509 (planifie Phase 4, necessite ATECC608B + EMQX)
- MQTT ACL par device (necessite broker EMQX)
- OTA firmware securise (necessite partitions A/B validees)
- Certification IEC 61439 / NF C 15-100 (processus long, post-pilote)

### Monitoring pilote

| URL | Description |
|-----|-------------|
| Grafana — Fleet Health | http://localhost:3000/d/mvp3-fleet-health | Sante flotte devices |
| Grafana — Pilot | http://localhost:3000/d/mvp3-pilot | Suivi programme pilote |
| Grafana — Security | http://localhost:3000/d/mvp3-security | Signatures, replay, rejets |
| Grafana — Data Quality | http://localhost:3000/d/mvp3-quality | Qualite donnees, drift |
| Prometheus Alerts | http://localhost:9090/alerts | 10 regles MVP3 actives |

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

### MVP 3 — IoT Real Device & Pilot

| Document | Description |
|----------|-------------|
| [MVP 3 Overview](docs/mvp3-overview.md) | Objectives, scope, prerequisites, architecture, safety warnings |
| [Edge-Cloud Architecture](docs/edge-cloud-architecture.md) | Sequences, responsibilities, offline 72h, compression, versioning |
| [Hardware Prototype](docs/hardware-prototype-strategy.md) | Options A/B/C comparison, BOM, progressive approach |
| [Firmware Architecture](docs/firmware-architecture.md) | ESP32-S3, ESP-IDF 5.x, 8 modules, state machine, tests |
| [MQTT Protocol v1](docs/mqtt-protocol-v1.md) | 6 topics, 7 payloads, HMAC, anti-replay, validation pipeline |
| [Device Provisioning](docs/device-provisioning.md) | Claim token, credential rotation, revocation, rate limiting |
| [Real Device Ingestion](docs/real-device-ingestion.md) | V1 topics, validation pipeline, rejection handling, quality |
| [Data Quality](docs/data-quality.md) | Scoring algorithm, 9 criteria, grades A-F, business rules |
| [Field Data Collection](docs/field-data-collection.md) | Dataset pipeline, pseudonymization, labels, RGPD |
| [Lab Test Protocol](docs/lab-test-protocol.md) | 14 scenarios, acceptance criteria, defect report templates |
| [Field Pilot 10 Devices](docs/field-pilot-10-devices.md) | Full pilot plan, 25 sections, KPIs, procedures, risks |
| [IoT Security](docs/iot-security.md) | STRIDE model, 13 controls, mTLS migration, pre-pilot checklist |
| [Observability MVP 3](docs/mvp3-observability.md) | 14 metrics, 10 alerts, tracing, 4 Grafana dashboards |
| [CI/CD MVP 3](docs/ci-cd-mvp3.md) | 7-job pipeline, versioning, firmware checklist, deploy-pilot |
| [Production Readiness](docs/mvp3-production-readiness-checklist.md) | 88-item checklist, GO/NO-GO criteria, residual risks |
| [MVP 3 Transition](docs/mvp3-transition.md) | Full transition plan: what's ready, what's missing, roadmap |
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

- **No electrical certification:** Not certified IEC 61439 or NF C 15-100 — prototype only
- **No production ML:** Currently uses statistical methods (Welford's algorithm); ML planned post-pilot (3+ months real data required)
- **No mTLS:** Device auth uses HMAC-SHA256; mTLS with secure element planned for MVP 4
- **No OTA firmware:** Updates require physical USB access during pilot
- **MVP scope:** Single-region, no HA, no disaster recovery — acceptable for 10-device pilot
- **Firmware not hardware-validated:** Host tests pass (43 tests); hardware 24h stability test pending
