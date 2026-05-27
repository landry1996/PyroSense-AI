# PyroSense AI Platform — Documentation Technique

> **Version** : MVP 3 (2026-05-28)
> **Statut** : Pilote conditionnel — en attente validation hardware 24h
> **Licence** : Proprietary — usage interne uniquement
> **AVERTISSEMENT** : Ce produit n'est PAS CERTIFIE (IEC 61439, NF C 15-100).
> Toute installation reelle requiert un electricien qualifie habilite B2V minimum.

---

## Table des matieres

1. [Vue d'ensemble](#1-vue-densemble)
2. [Stack technique](#2-stack-technique)
3. [Architecture microservices](#3-architecture-microservices)
4. [Architecture hexagonale](#4-architecture-hexagonale)
5. [Services et bounded contexts](#5-services-et-bounded-contexts)
6. [Communication inter-services](#6-communication-inter-services)
7. [Modele de donnees](#7-modele-de-donnees)
8. [Securite](#8-securite)
9. [IoT et firmware](#9-iot-et-firmware)
10. [Protocole MQTT v1](#10-protocole-mqtt-v1)
11. [Pipeline d'ingestion](#11-pipeline-dingestion)
12. [Analyse de signal](#12-analyse-de-signal)
13. [Scoring de risque](#13-scoring-de-risque)
14. [Observabilite](#14-observabilite)
15. [CI/CD](#15-cicd)
16. [Base de donnees](#16-base-de-donnees)
17. [Infrastructure locale](#17-infrastructure-locale)
18. [Tests](#18-tests)
19. [Performance](#19-performance)
20. [Deploiement](#20-deploiement)
21. [Decisions architecturales (ADR)](#21-decisions-architecturales)

---

## 1. Vue d'ensemble

PyroSense AI est une plateforme de prevention predictive des incendies electriques. Elle combine capteurs IoT, analyse statistique en temps reel et scoring de risque pour detecter les signes precurseurs de defauts electriques avant qu'ils ne causent un incendie.

### Principes fondamentaux

- **Edge-first** : calcul des features sur le capteur, pas de donnees brutes au cloud
- **Explicable** : chaque score de risque est decompose en facteurs comprehensibles
- **Multi-tenant** : isolation stricte des donnees par tenant
- **Zero-trust IoT** : HMAC-SHA256, anti-replay, revocation instantanee
- **Resilience offline** : buffer 72h sur capteur en cas de perte de connectivite

---

## 2. Stack technique

| Couche | Technologie | Version |
|--------|------------|---------|
| Langage backend | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.1 |
| API Gateway | Spring Cloud Gateway | 2023.0.x |
| Messaging async | Apache Kafka (KRaft) | 3.6+ |
| Messaging IoT | MQTT (Mosquitto) | 2.0 |
| Base relationnelle | PostgreSQL | 16 |
| Time-series | TimescaleDB | 2.x (extension PG) |
| Cache / Rate limiting | Redis | 7.x |
| Authentification | Keycloak | 24 (OAuth2/OIDC) |
| Metriques | Prometheus | 2.x |
| Dashboards | Grafana | 10.x |
| Logs | Loki | 2.9 |
| Tracing | OpenTelemetry (OTLP) | 0.96 |
| Conteneurs | Docker + Compose v2 | 24+ |
| Build | Maven | 3.9+ |
| Qualite | JaCoCo, ArchUnit, Spotless, OWASP | - |
| Frontend | Angular | 18 (standalone components, signals) |
| Firmware | ESP-IDF | 5.x (C++17, FreeRTOS) |
| MCU | ESP32-S3 | N16R8 |

### Versions Java

- Java 21 avec : virtual threads ready, pattern matching, records, sealed classes
- Compilation : `--release 21`, `--enable-preview` non utilise

---

## 3. Architecture microservices

### Topologie

```
                            ┌─────────────────────┐
                            │   Angular Frontend   │
                            │   (port 4200)        │
                            └──────────┬──────────┘
                                       │ HTTPS
                            ┌──────────▼──────────┐
                            │    API Gateway       │
                            │    (port 8080)       │
                            │  JWT + Rate Limit    │
                            └──────────┬──────────┘
                                       │
           ┌───────────────────────────┼────────────────────────────┐
           │                           │                            │
    ┌──────▼──────┐           ┌────────▼───────┐          ┌────────▼───────┐
    │  Identity   │           │    Device      │          │   Ingestion    │
    │  Service    │           │    Service     │          │   Service      │
    │  (8081)     │           │    (8082)      │          │   (8083)       │
    └─────────────┘           └────────────────┘          └────────┬───────┘
                                                                   │ MQTT
                                                          ┌────────▼───────┐
                                                          │   Mosquitto    │
                                                          │   MQTT Broker  │
                                                          └────────────────┘
           │                           │                            │
    ┌──────▼──────┐           ┌────────▼───────┐          ┌────────▼───────┐
    │  Signal     │           │  Risk Scoring  │          │   Alerting     │
    │  Analysis   │           │  Service       │          │   Service      │
    │  (8084)     │           │  (8085)        │          │   (8086)       │
    └─────────────┘           └────────────────┘          └────────────────┘
           │                           │                            │
    ┌──────▼──────┐           ┌────────▼───────┐          ┌────────▼───────┐
    │Notification │           │  Maintenance   │          │   Reporting    │
    │  Service    │           │  Service       │          │   Service      │
    │  (8087)     │           │  (8089)        │          │   (8091)       │
    └─────────────┘           └────────────────┘          └────────────────┘
                                       │
                              ┌────────▼───────┐
                              │  Dashboard     │
                              │  Service       │
                              │  (8088)        │
                              └────────────────┘
```

### Principes

- **Database-per-service** : chaque service possede sa propre base PostgreSQL
- **Communication asynchrone** : Kafka pour les evenements de domaine
- **Communication synchrone** : HTTP/REST uniquement pour les queries urgentes
- **Eventual consistency** : les services sont coherents a terme via les evenements
- **Pas de transactions distribuees** : compensation events si besoin

---

## 4. Architecture hexagonale

Chaque service respecte strictement le pattern ports & adapters :

```
┌──────────────────────────────────────────────────────────────────┐
│                          SERVICE                                   │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                     DOMAIN (centre)                          │  │
│  │  - Aggregates, Entities, Value Objects                       │  │
│  │  - Domain Events                                             │  │
│  │  - Business rules (pure Java, ZERO framework dependency)     │  │
│  └───────────────────────────┬─────────────────────────────────┘  │
│                              │                                     │
│  ┌───────────────────────────▼─────────────────────────────────┐  │
│  │                   APPLICATION (use cases)                    │  │
│  │  - Ports IN (interfaces: *UseCase, *Query)                  │  │
│  │  - Ports OUT (interfaces: *RepositoryPort, *EventPort)      │  │
│  │  - Service implementations (orchestration)                  │  │
│  └───────────────────────────┬─────────────────────────────────┘  │
│                              │                                     │
│  ┌───────────────────────────▼─────────────────────────────────┐  │
│  │                    ADAPTER (infrastructure)                  │  │
│  │  IN:  REST controllers, Kafka listeners, MQTT listeners     │  │
│  │  OUT: JDBC repositories, Kafka producers, HTTP clients      │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                    CONFIG (Spring wiring)                    │  │
│  │  - SecurityConfig, KafkaConfig, UseCaseConfig               │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
```

### Package layout

```
com.pyrosense.{service}/
├── domain/
│   ├── model/          # Aggregates, entities, value objects
│   ├── event/          # Domain events
│   └── validation/     # Domain validators
├── application/
│   ├── port/
│   │   ├── in/         # Use case interfaces (driven by)
│   │   └── out/        # Repository/publisher interfaces (driving)
│   └── usecase/        # Service implementations
├── adapter/
│   ├── in/
│   │   ├── rest/       # Controllers + DTOs
│   │   └── messaging/  # Kafka/MQTT listeners
│   └── out/
│       ├── persistence/  # JDBC repositories
│       ├── messaging/    # Kafka producers
│       └── http/         # REST clients
└── config/             # Spring configuration
```

### Regles ArchUnit (10+ par service)

- Le domaine n'importe AUCUNE classe Spring/JPA/Kafka
- Les ports sont des interfaces
- Pas d'injection par champ (uniquement constructeur)
- Les adapters n'accedent pas directement au domaine (passent par application)
- Les controllers ne contiennent pas de logique metier

---

## 5. Services et bounded contexts

### 5.1 Shared Kernel (`pyrosense-shared-kernel`)

Bibliotheque partagee contenant :
- Strongly-typed IDs : `TenantId`, `DeviceId`, `BuildingId`, `AlertId`, etc.
- Base classes : `AggregateRoot`, `DomainEvent`, `ValueObject`
- Value objects : `RiskScore`, `Percentage`, `Money`
- Enums : `AlertSeverity`, `DefectType`, `PlatformRole`, `Permission`
- Pagination : `Page<T>`, `PageRequest`
- Erreurs : `BusinessException`, `NotFoundException`, `ValidationException`
- Utilitaires : `ClockProvider`, `IdGenerator`, `PayloadIntegrity`
- **77 tests unitaires**

### 5.2 Identity Service (port 8081)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Gestion des tenants, users, roles, credentials device |
| Aggregates | User (state machine), Tenant, DeviceCredential |
| Roles | 8 roles → 34 permissions |
| Securite | OAuth2 JWT via Keycloak, TenantContext ThreadLocal |
| Tests | 30 tests |

### 5.3 Device Service (port 8082)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Lifecycle device, provisioning securise, revocation |
| Aggregates | Device (6 etats), ClaimToken, DeviceCredential, ProvisioningSession |
| Provisioning | Claim token single-use → credential HMAC → hash stocke |
| Events | DeviceRegistered, Provisioned, Activated, Revoked, CredentialRotated |
| Tests | 32 tests + 26 tests provisioning |

### 5.4 Ingestion Service (port 8083)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Reception MQTT, validation protocol, persistence time-series |
| Pipeline | Rate limit → Schema → Device status → HMAC → Anti-replay → Firmware track |
| Persistence | JDBC batch write (TimescaleDB), pas de JPA |
| Idempotency | Redis (deviceId + timestamp + payloadHash, TTL 24h) |
| Data Quality | Scoring 0-100, grades A-F, 9 criteres |
| Dataset | DatasetCandidate, labels terrain, export Parquet/CSV |
| Tests | 38 tests + protocol tests + quality tests |

### 5.5 Signal Analysis Service (port 8084)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Detection anomalies, baseline learning, drift |
| Algorithmes | Z-Score, MicroArc pattern, Temperature trend, THD drift, EMA |
| Baseline | Welford's online algorithm (O(1) memoire) |
| ML Port | `MachineLearningInferencePort` (NoOp pour MVP) |
| Events | SignalAnomalyDetected, BaselineBuilt, BaselineDriftDetected |
| Tests | 66 tests |

### 5.6 Risk Scoring Service (port 8085)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Score de risque composite, prediction incident |
| Formule | 6 facteurs ponderes × modifiers (recency, repetition, trend) |
| Feedback | FeedbackConfidenceEngine (boost/reduction par retour terrain) |
| ML Port | `RiskModelPort` (NoOp pour MVP) |
| Events | RiskScoreUpdated, CriticalRiskDetected, RiskLevelChanged |
| Tests | 58 tests |

### 5.7 Alerting Service (port 8086)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Creation alertes, deduplication, escalation, SLA |
| States | OPEN → ACKNOWLEDGED → IN_PROGRESS → RESOLVED / FALSE_POSITIVE |
| Escalation | NONE → FIRST (1h) → SECOND (4h) → EMERGENCY (8h) |
| Deduplication | deviceId + anomalyType (empêche alertes doublons) |
| Tests | 85 tests |

### 5.8 Notification Service (port 8087)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Notifications multi-canaux, preferences, anti-spam |
| Canaux | EMAIL, SMS, PUSH, WEBHOOK, DASHBOARD |
| Routing | INFO→Dashboard, WARNING→Email+Dashboard, CRITICAL→SMS+Push+Email+Dashboard |
| Retry | Exponential backoff : 30s → 2min → 10min (max 3) |
| Templates | 9 templates (4 canaux chacun), regles de confidentialite |
| Preferences | Par user, quiet hours, critical override, verif. technique |
| Tests | 206 tests |

### 5.9 Maintenance Service (port 8089)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Cycle de vie interventions, feedback loop |
| States | CREATED → PLANNED → ASSIGNED → IN_PROGRESS → COMPLETED / CANCELLED |
| Workflow | CRITICAL alert → auto-intervention, WARNING → recommendation manager |
| Feedback | DefectConfirmed → boost scoring, FalsePositive → reduction |
| SLA | Par priorite : URGENT 4h/24h, HIGH 24h/72h, MEDIUM 72h/7j |
| Tests | 84 tests |

### 5.10 Reporting Service (port 8091)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Generation PDF, certificats, download securise |
| Types | Bilan mensuel, Attestation surveillance, Alerte critique, Intervention |
| PDF | OpenPDF, A4, format professionnel, signature SHA-256 |
| Securite | Download token single-use (SecureRandom 32B, TTL 15min) |
| Tests | 107 tests |

### 5.11 Dashboard Service (port 8088)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Read-model optimise pour le frontend, cache |
| Pattern | CQRS read-side, cache-aside (Redis, TTL configurable) |
| Endpoints | Overview, risky-buildings, risk-trend, recent-alerts, priority-interventions, device-health, pilot |
| Invalidation | Kafka consumer (5 topics) invalide le cache |
| Tests | 23 tests |

### 5.12 API Gateway (port 8080)

| Aspect | Detail |
|--------|--------|
| Responsabilite | Routing, JWT validation, rate limiting, headers securite |
| Routes | 11 routes vers 9 services downstream |
| Securite | HSTS, CSP, X-Frame-Options DENY, header sanitization |
| Rate Limiting | Redis-based, 60/10/120 req/min par type endpoint |
| Filtres | 6 filtres ordonnes (sanitization → payload → tracing → propagation → headers → rate-limit) |
| Tests | 55 tests |

---

## 6. Communication inter-services

### Topics Kafka

| Topic | Producteur | Consommateur | Contenu |
|-------|-----------|-------------|---------|
| `telemetry-events` | Ingestion | Signal Analysis | TelemetryReceivedEvent |
| `analysis-events` | Signal Analysis | Risk Scoring, Alerting | SignalAnomalyDetectedEvent |
| `scoring-events` | Risk Scoring | Alerting, Notification | RiskScoreUpdatedEvent, CriticalRiskDetected |
| `alerting-events` | Alerting | Notification, Maintenance | AlertCreated, Escalated |
| `maintenance-events` | Maintenance | Risk Scoring, Notification | InterventionCompleted, DefectConfirmed |
| `reporting-events` | Reporting | Notification | ReportGenerated |
| `device-events` | Device | Notification, Ingestion | DeviceOffline, Revoked |
| `ingestion-dlq` | Ingestion | - | Messages en erreur |

### Format enveloppe (IntegrationEvent)

```json
{
  "eventId": "uuid-v4",
  "eventType": "telemetry.received",
  "occurredAt": "2026-05-28T14:30:00Z",
  "tenantId": "tenant-001",
  "correlationId": "uuid-v4",
  "payload": { ... }
}
```

### Garanties

- **At-least-once delivery** : consumers idempotents
- **Ordering** : par partition (key = deviceId ou tenantId)
- **DLQ** : messages non traitables apres 3 retries
- **Schema evolution** : additive-only (pas de breaking changes)

---

## 7. Modele de donnees

### PostgreSQL (par service)

| Service | Database | Tables principales |
|---------|----------|-------------------|
| Device | `device_db` | devices, claim_tokens, device_credentials, provisioning_sessions |
| Ingestion | `ingestion_db` | electrical_telemetry (hypertable), device_heartbeats, ingestion_rejections, data_quality_*, dataset_* |
| Signal Analysis | `analysis_db` | baseline_profiles (JSONB), signal_anomalies, analysis_results |
| Risk Scoring | `scoring_db` | risk_assessments (JSONB factors), scoring_feedback |
| Alerting | `alerting_db` | alerts (JSONB comments) |
| Notification | `notification_db` | notifications, notification_preferences, tenant_notification_policy, audit_log |
| Maintenance | `maintenance_db` | interventions (JSONB diagnostic), intervention_comments, intervention_recommendations |
| Reporting | `reporting_db` | reports (JSONB metadata) |
| Dashboard | `dashboard_db` | buildings, devices, alerts, interventions, dashboard_risk_trend, pilot_* |

### TimescaleDB (Ingestion)

```sql
-- Hypertable principale (chunks de 7 jours)
CREATE TABLE electrical_telemetry (
    device_id       TEXT NOT NULL,
    tenant_id       TEXT NOT NULL,
    timestamp       TIMESTAMPTZ NOT NULL,
    rms_voltage     DOUBLE PRECISION,
    rms_current     DOUBLE PRECISION,
    active_power    DOUBLE PRECISION,
    thd             DOUBLE PRECISION,
    temperature     DOUBLE PRECISION,
    hf_noise_level  DOUBLE PRECISION,
    micro_arc_count INTEGER,
    transient_count INTEGER,
    power_factor    DOUBLE PRECISION,
    crest_factor    DOUBLE PRECISION,
    signal_quality  DOUBLE PRECISION
);

SELECT create_hypertable('electrical_telemetry', 'timestamp', chunk_time_interval => INTERVAL '7 days');
```

### Continuous aggregates

| Vue | Granularite | Retention |
|-----|-------------|-----------|
| `telemetry_1min` | 1 minute | 180 jours |
| `telemetry_15min` | 15 minutes | 1 an |
| `telemetry_1hour` | 1 heure | 2 ans |
| `telemetry_daily` | 1 jour | Indefinie |

### Retention policies

| Donnees | Retention | Action |
|---------|-----------|--------|
| Raw telemetry | 90 jours | DROP_CHUNKS |
| Rejections | 30 jours | DROP_CHUNKS |
| Aggregats 1min | 180 jours | DROP_CHUNKS |
| Aggregats daily | Indefinie | - |

---

## 8. Securite

### Authentication & Authorization

- **OAuth2/OIDC** via Keycloak 24
- **Client frontend** : `pyrosense-frontend` (public, PKCE S256)
- **Client backend** : `pyrosense-backend` (confidential, service account)
- **JWT claims** : `tenant_id`, `roles`, `permissions`, `sub`
- **TenantContext** : ThreadLocal extrait du JWT, obligatoire sur chaque requete
- **@PreAuthorize** : method-level security sur chaque endpoint

### Roles (8)

| Role | Description |
|------|-------------|
| PLATFORM_ADMIN | Acces complet cross-tenant |
| TENANT_ADMIN | Admin d'un tenant (users, buildings, config) |
| PROPERTY_MANAGER | Gestion batiments et zones |
| ELECTRICIAN | Interventions terrain |
| DEVICE_MANAGER | Lifecycle devices |
| OPERATOR | Monitoring temps reel |
| OCCUPANT | Lecture seule zone |
| SUPPORT_READONLY | Support client lecture seule |

### Securite IoT

| Controle | Implementation |
|----------|----------------|
| Authentification device | HMAC-SHA256 sur chaque message |
| Anti-replay | Nonce uniqueness + messageId dedup + sequence monotone |
| Rate limiting | Per-device, synchronized window reset |
| Revocation | Effet immediat (pipeline rejette device REVOKED) |
| Constant-time comparison | Pas de timing leak sur verification signature |
| Credential hash storage | SHA-256, jamais en clair |
| Rotation | Nouveau credential, ancien revoque atomiquement |

### Headers securite (Gateway)

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

---

## 9. IoT et firmware

### Materiel cible

- **MCU** : ESP32-S3 (dual-core Xtensa LX7, 240MHz, 512KB SRAM, 16MB flash, 8MB PSRAM)
- **ADC** : ADS1115 (I2C, 16-bit, 860 SPS, 4 canaux differentiels)
- **Temperature** : MAX31865 (SPI, PT100, 3-wire)
- **Connectivite** : WiFi 802.11 b/g/n (2.4 GHz)

### Architecture firmware

```
┌─────────────────────────────────────────────────┐
│                  ESP32-S3                         │
│                                                  │
│  ┌────────────┐  ┌──────────────┐  ┌─────────┐ │
│  │ Acquisition│  │  Processing  │  │  Comms   │ │
│  │ Task (C1)  │  │  Task (C0)   │  │ Task(C0) │ │
│  │ Priority 4 │  │  Priority 3  │  │ Prio 2   │ │
│  └─────┬──────┘  └──────┬───────┘  └────┬─────┘ │
│        │                 │               │       │
│        ▼                 ▼               ▼       │
│  ┌──────────────────────────────────────────────┐│
│  │           FreeRTOS Queues                     ││
│  │  raw_samples(64) → features(32) → mqtt(16)   ││
│  └──────────────────────────────────────────────┘│
│                                                  │
│  ┌──────────────────────────────────────────────┐│
│  │  State Machine (7 etats)                      ││
│  │  BOOTING → PROVISIONING → CONNECTING →        ││
│  │  ACTIVE → OFFLINE_BUFFERING → DEGRADED → ERROR││
│  └──────────────────────────────────────────────┘│
│                                                  │
│  ┌──────────────────────────────────────────────┐│
│  │  Offline Queue (SPIFFS, 72h buffer)           ││
│  └──────────────────────────────────────────────┘│
│                                                  │
│  ┌──────────────────────────────────────────────┐│
│  │  Security: HMAC-SHA256 (mbedtls)              ││
│  │  NonceGenerator, constant-time verify         ││
│  └──────────────────────────────────────────────┘│
└─────────────────────────────────────────────────┘
```

### Modules firmware (8)

| Module | Fichiers | Responsabilite |
|--------|----------|----------------|
| config | device_config.h/cpp, state_machine.h/cpp | Configuration NVS, machine a etats |
| sensors | sensor_interface.h, mock_sensor_provider, sensor_registry | Abstraction capteurs |
| signal_processing | feature_extractor, rms_calculator, signal_quality | Calcul features edge |
| telemetry | payload_builder, heartbeat_builder | Construction payloads MQTT |
| connectivity | mqtt_client, wifi_manager | Communication reseau |
| security | hmac_signer, nonce_generator | Securite cryptographique |
| storage | offline_queue | Buffer offline SPIFFS |
| diagnostics | logger, self_test | Diagnostic et logs |

### Tests host (43 tests, 7 suites)

Build CMake host (`PYRO_HOST_BUILD`) : compile le firmware pour x86/x64 sans ESP-IDF, execute les tests unitaires avec assertions C++.

---

## 10. Protocole MQTT v1

### Topics

```
pyrosense/v1/{tenantId}/{deviceId}/telemetry    # Telemetrie periodique
pyrosense/v1/{tenantId}/{deviceId}/heartbeat    # Signal de vie
pyrosense/v1/{tenantId}/{deviceId}/events       # Evenements ponctuels
pyrosense/v1/{tenantId}/{deviceId}/provisioning # Enrolement device
pyrosense/v1/{tenantId}/{deviceId}/command      # Commandes cloud → device
pyrosense/v1/{tenantId}/{deviceId}/command-ack  # Acquittement commande
```

### Payload telemetrie

```json
{
  "schemaVersion": "1.0",
  "deviceId": "device-001",
  "tenantId": "tenant-001",
  "timestamp": 1716900000,
  "messageId": "uuid-v4",
  "sequenceNumber": 42,
  "isDrain": false,
  "features": {
    "rmsVoltage": 230.5,
    "rmsCurrent": 12.3,
    "activePower": 2835.15,
    "thd": 4.2,
    "temperature": 45.6,
    "hfNoiseLevel": 0.15,
    "microArcCount": 0,
    "transientCount": 1,
    "powerFactor": 0.98,
    "crestFactor": 1.41,
    "signalQuality": 92
  },
  "security": {
    "nonce": "1716900000-000042-a1b2c3d4",
    "signature": "hmac-sha256-hex-64-chars"
  }
}
```

### Pipeline de validation (6 etapes)

1. **Rate Limit** : max messages/min par device
2. **Schema Validation** : version supportee, champs obligatoires, ranges
3. **Device Status** : device actif et non-revoque
4. **HMAC Verification** : signature valide (constant-time)
5. **Anti-Replay** : nonce unique + messageId unique + sequence monotone
6. **Firmware Tracking** : mise a jour version firmware observee

---

## 11. Pipeline d'ingestion

```
MQTT Broker (Mosquitto)
    │
    ▼
MqttTelemetryListener
    │
    ├── handleV1Telemetry()
    │   └── ProtocolValidationPipeline (6 steps)
    │       ├── PASS → IngestTelemetryUseCase
    │       │           ├── TelemetryValidator (domain ranges)
    │       │           ├── IdempotencyCheck (Redis)
    │       │           ├── JdbcBatchPersist (TimescaleDB)
    │       │           ├── TelemetryQualityAssessor (scoring)
    │       │           └── KafkaPublish (telemetry-events)
    │       │
    │       └── REJECT → RejectionRepository + metrics
    │
    ├── handleV1Heartbeat()
    │   ├── Rate limit check
    │   ├── Device revocation check
    │   └── IngestHeartbeatUseCase → persist + publish
    │
    └── handleV1Event()
        └── ProtocolValidationPipeline → persist + publish
```

### Metriques d'ingestion

- `pyrosense_ingestion_received_total` (counter, par type)
- `pyrosense_ingestion_rejected_total` (counter, par raison)
- `pyrosense_ingestion_latency_ms` (timer, percentiles P50/P95/P99)
- `pyrosense_device_heartbeat_total` (counter)
- `pyrosense_data_quality_score` (gauge, par device)

---

## 12. Analyse de signal

### Detecteurs (5 algorithmes)

| Detecteur | Methode | Seuil configurable |
|-----------|---------|-------------------|
| ZScoreDetector | Deviation depuis baseline | `z-score-threshold: 3.0` |
| MicroArcPatternDetector | Recurrence arcs dans fenetre | `recurrence-threshold: 3` |
| TemperatureTrendDetector | Taux de montee + max absolu | `max: 85°C, rate: 5°C/h` |
| ThdDriftDetector | Derive THD vs baseline | `max-percent: 8.0` |
| ExponentialSmoothingDetector | EMA + deviation | `alpha: 0.3` |

### Baseline (Welford's Algorithm)

Calcul incremental de la moyenne et variance (pas besoin de stocker l'historique) :

```
M(n) = M(n-1) + (x - M(n-1)) / n
S(n) = S(n-1) + (x - M(n-1)) × (x - M(n))
Variance = S(n) / (n-1)
```

- Periode d'apprentissage : 7 jours minimum
- Profil par device (chaque installation est unique)
- Adaptatif : evolue avec les saisons

---

## 13. Scoring de risque

### Formule

```
RiskScore = Σ (weight_i × normalizedValue_i × recencyDecay × repetitionBoost) × 100
```

### Facteurs (6)

| Facteur | Poids | Input |
|---------|-------|-------|
| Micro-arcs | 0.30 | Confiance × recurrence |
| THD drift | 0.20 | Deviation vs baseline |
| Temperature | 0.20 | Rate + absolute |
| Transitoires | 0.10 | Frequence anormale |
| Bruit HF | 0.10 | Niveau vs baseline |
| Fiabilite | 0.10 | Penalite offline/no-baseline |

### Niveaux

| Niveau | Range | Action |
|--------|-------|--------|
| LOW | 0-29 | Surveillance normale |
| MODERATE | 30-59 | Vigilance accrue |
| HIGH | 60-79 | Intervention planifiee |
| CRITICAL | 80-100 | Intervention immediate |

### Prediction incident

```
days_to_incident = base_window[level] / trend_multiplier[trend]
```

---

## 14. Observabilite

### Stack

| Composant | Role |
|-----------|------|
| Micrometer | Instrumentation Java (metrics) |
| Prometheus | Stockage metriques, alertes |
| Grafana | Visualisation, 7 dashboards |
| Loki | Aggregation logs |
| OpenTelemetry Collector | Reception traces OTLP |

### Metriques business (14 MVP3)

- Ingestion : received/rejected/latency/quality
- Scoring : events/processed/duration
- Pilot : devices_online/data_quality/alert_count/fp_rate

### Alertes Prometheus (10 regles)

- Service down > 5min
- Ingestion rejection rate > 20%
- Device offline > 1h
- Risk score CRITICAL > 30min without ack
- Data quality < 50%
- etc.

### Dashboards Grafana (7)

1. Platform Overview
2. Ingestion Pipeline
3. Alerting & Notifications
4. MVP3 Device Fleet Health
5. MVP3 Ingestion Security
6. MVP3 Data Quality
7. MVP3 Pilot Monitoring

### Logging structure

```json
{
  "timestamp": "2026-05-28T14:30:00.123Z",
  "level": "INFO",
  "logger": "c.p.ingestion.adapter.in.mqtt.MqttTelemetryListener",
  "message": "Telemetry received",
  "traceId": "abc123",
  "spanId": "def456",
  "correlationId": "uuid",
  "tenantId": "tenant-001",
  "deviceId": "device-001"
}
```

---

## 15. CI/CD

### Pipeline GitHub Actions (`mvp3-ci.yml`)

| Job | Contenu | Condition |
|-----|---------|-----------|
| 1. Backend Build | Maven compile + test + JaCoCo | Toujours |
| 2. Frontend Build | ng build --prod + ng test | Toujours |
| 3. Firmware Build | CMake host build + CTest | Toujours |
| 4. Security Scan | OWASP Dependency-Check + secret scan | Toujours |
| 5. Docker Build | Multi-stage images (10 services) | Sur main |
| 6. Integration Tests | Testcontainers (PG, Kafka, Redis) | Sur main |
| 7. Pilot Deploy | Deploy pilote (manual approval) | workflow_dispatch |

### Qualite gates

- Tests unitaires : 900+ (0 failure)
- Couverture : domain 90%, application 85%
- ArchUnit : 10+ regles par service
- Spotless : format Palantir Java Format
- OWASP : CVSS < 8 (pas de vulnerabilite critique)

---

## 16. Base de donnees

### Multi-database init

Script Docker entrypoint creant 9 databases + TimescaleDB :

```sql
CREATE DATABASE device_db;
CREATE DATABASE ingestion_db;
CREATE DATABASE analysis_db;
CREATE DATABASE scoring_db;
CREATE DATABASE alerting_db;
CREATE DATABASE notification_db;
CREATE DATABASE maintenance_db;
CREATE DATABASE reporting_db;
CREATE DATABASE dashboard_db;

-- TimescaleDB sur ingestion_db
\c ingestion_db
CREATE EXTENSION IF NOT EXISTS timescaledb;
```

### Migrations Flyway

Chaque service a son propre repertoire `src/main/resources/db/migration/` avec des migrations versionnees (V001, V002, ...).

---

## 17. Infrastructure locale

### Docker Compose

| Service | Image | Port |
|---------|-------|------|
| PostgreSQL | postgres:16 | 5432 |
| Redis | redis:7-alpine | 6379 |
| Kafka | confluentinc/cp-kafka:7.5.0 | 9092, 29092 |
| Mosquitto | eclipse-mosquitto:2 | 1883, 8883 |
| Keycloak | quay.io/keycloak/keycloak:24 | 8180 |
| Prometheus | prom/prometheus:v2.48.0 | 9090 |
| Grafana | grafana/grafana:10.2.0 | 3000 |
| Loki | grafana/loki:2.9.0 | 3100 |
| OTEL Collector | otel/opentelemetry-collector-contrib:0.96.0 | 4317, 4318 |

### Profils Docker Compose

- `(default)` : infrastructure seule
- `services` : infra + 10 backend services
- `simulator` : infra + simulateur IoT
- `full` : tout

### Scripts

- `scripts/start-local.sh` : demarrage interactif avec selection profil
- `scripts/stop-local.sh` : arret graceful
- `scripts/reset-local.sh` : reset destructif (volumes)

---

## 18. Tests

### Strategie pyramidale

| Niveau | Scope | Outils | Quantite |
|--------|-------|--------|----------|
| Unitaire | Domaine + application | JUnit 5, Mockito | ~700 |
| Integration | Adapters + persistence | Testcontainers, H2 | ~100 |
| Architecture | Structure du code | ArchUnit | ~100 |
| Securite | RBAC, tenant isolation | @WebMvcTest, @WithMockUser | ~80 |
| Performance | Smoke tests charge | JUnit + timers | 5 |
| E2E | Parcours frontend | Cypress | 6 fichiers |
| Firmware | Host unit tests | CTest, C++ assertions | 43 |

### Total : 900+ tests backend, 109 tests frontend, 43 tests firmware

### Conventions

- Un test par comportement (pas par methode)
- Naming : `should_doSomething_when_condition`
- Pas de secrets dans les tests
- Pas de sleep (utiliser `Awaitility` si async)
- Domaine teste sans framework (POJO)

---

## 19. Performance

### Benchmarks

| Operation | Cible | Mesure |
|-----------|-------|--------|
| Ingestion telemetrie | < 50ms P99 | Timer Micrometer |
| Batch write TimescaleDB | 1000 msg < 500ms | IngestionPerformanceSmokeTest |
| Risk scoring | 500 calcs < 3s | RiskScoringPerformanceSmokeTest |
| API Gateway routing | < 10ms overhead | Actuator metrics |
| MQTT → Kafka end-to-end | < 200ms | Tracing OTLP |

### Optimisations

- **JDBC batch** pour les writes TimescaleDB (10-50x vs JPA)
- **Chunk exclusion** TimescaleDB via index tenant_id + timestamp
- **Redis cache** sur dashboard read-model (TTL 30-60s)
- **Kafka partitioning** par deviceId (localite)
- **Connection pooling** HikariCP (min 5, max 20)

---

## 20. Deploiement

### Environnements

| Env | Infra | Deploiement |
|-----|-------|-------------|
| Local | Docker Compose | `./scripts/start-local.sh` |
| CI | GitHub Actions | Automatique sur push |
| Pilote | Docker Compose (serveur) | Manual approval + workflow_dispatch |
| Production | Kubernetes (futur) | Helm charts (non implemente) |

### Profils Spring Boot

| Profil | Usage |
|--------|-------|
| `local` | Developpement IDE, H2, logs console |
| `test` | CI, H2, assertions |
| `docker` | Docker Compose, DNS Docker |
| `prod` | Production, logs JSON, sampling traces 10% |

---

## 21. Decisions architecturales

| ADR | Decision | Raison |
|-----|----------|--------|
| ADR-001 | Microservices + hexagonal | Bounded contexts avec besoins differents |
| ADR-002 | Kafka pour events | Durabilite, replay, ordering par partition |
| ADR-003 | TimescaleDB (pas InfluxDB) | SQL standard, continuous aggregates, extension PG |
| ADR-004 | JDBC batch (pas JPA) pour ingestion | Performance 10-50x sur time-series append-only |
| ADR-005 | HMAC-SHA256 (pas mTLS) pour MVP | Complexite PKI trop elevee pour prototype |
| ADR-006 | Features calculees sur edge | Pas de donnees brutes au cloud, bande passante |
| ADR-007 | Scoring statistique avant ML | Explicable, zero training data, baseline comparaison |
| ADR-008 | Redis pour idempotency/cache | Performant, TTL natif, deja en stack |
| ADR-009 | Keycloak (pas custom auth) | Standards OAuth2/OIDC, multi-tenant natif |
| ADR-010 | ArchUnit enforcement | Garantie structurelle hexagonale via CI |
| ADR-011 | Frontend Angular standalone | Tree-shaking, signals, lazy loading natif |
| ADR-012 | MQTT QoS 1 (at-least-once) | Compromis fiabilite/performance, idempotency cote server |
| ADR-013 | Offline buffer 72h | Resilience terrain, drain ordonne a la reconnexion |
| ADR-014 | NoOp ML adapters | Ports prepares, deploiement ML sans modification domaine |

---

## Annexe : Ports et URLs

| Service | Port | Health | Metrics |
|---------|------|--------|---------|
| API Gateway | 8080 | /actuator/health | /actuator/prometheus |
| Identity | 8081 | /actuator/health | /actuator/prometheus |
| Device | 8082 | /actuator/health | /actuator/prometheus |
| Ingestion | 8083 | /actuator/health | /actuator/prometheus |
| Signal Analysis | 8084 | /actuator/health | /actuator/prometheus |
| Risk Scoring | 8085 | /actuator/health | /actuator/prometheus |
| Alerting | 8086 | /actuator/health | /actuator/prometheus |
| Notification | 8087 | /actuator/health | /actuator/prometheus |
| Dashboard | 8088 | /actuator/health | /actuator/prometheus |
| Maintenance | 8089 | /actuator/health | /actuator/prometheus |
| Reporting | 8091 | /actuator/health | /actuator/prometheus |
| Keycloak | 8180 | /health/ready | - |
| Grafana | 3000 | - | - |
| Prometheus | 9090 | /-/healthy | - |
