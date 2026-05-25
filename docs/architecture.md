# PyroSense AI Platform - Architecture Document

---

## 1. Vision

### What is PyroSense AI?

PyroSense AI is a **predictive electrical fire prevention platform**. It combines IoT sensors deployed on electrical panels with real-time signal analysis and risk scoring to detect early signs of electrical faults **before** they cause fires.

### The Problem We Solve

Electrical fires are responsible for thousands of building fires each year. Most are caused by:

- **Micro-arcs**: tiny electrical discharges in degraded wiring
- **Insulation degradation**: aging or heat-damaged insulation allowing current leakage
- **Loose connections**: vibration or thermal cycling loosening terminals
- **Overheating**: sustained overcurrent causing progressive conductor damage

These phenomena produce detectable electrical and thermal signatures **hours to weeks** before ignition. PyroSense captures these signatures continuously and alerts building managers before danger materializes.

### Maturity Phases

| Phase | Scope | Detection Method | Timeline |
|-------|-------|------------------|----------|
| **MVP** | Statistical detection, core platform, 1-3 test buildings | Z-score, moving averages, threshold-based rules | Current |
| **Pilot** | Limited deployment (10 buildings), validated alerts, technician workflows | Statistical + basic pattern matching | Future |
| **Production** | At scale (100+ buildings), multi-tenant, certified hardware | ML models + statistical fallback, real-time edge inference | Future |

### Key Differentiators (Target)

- **Predictive, not reactive**: detect degradation trends, not just overcurrent events
- **Multi-signal correlation**: combine temperature, current, impedance, partial discharge, harmonics
- **Explainable risk**: every risk score includes contributing factors and recommended actions
- **Integration-ready**: open APIs for BMS, insurance platforms, maintenance tools

---

## 2. Architecture Style

### Decision: Microservices + Hexagonal Architecture + Domain-Driven Design

PyroSense adopts a **microservices architecture** with each service internally structured following the **hexagonal (ports & adapters) pattern** and tactical patterns from **Domain-Driven Design**.

### Why Microservices?

| Justification | Explanation |
|---------------|-------------|
| **Bounded contexts need independent evolution** | Signal analysis algorithms change frequently; alerting rules are stable. They should not be coupled. |
| **IoT ingestion has different scaling needs** | Ingestion handles thousands of sensor readings per second; reporting generates PDFs once a day. Separate scaling profiles. |
| **Team autonomy** | Each bounded context can be owned by a small team with full lifecycle control. |
| **Fault isolation** | A bug in notification should never stop ingestion from collecting sensor data. |
| **Technology flexibility** | Future ML services may use Python while core services remain Java. Hexagonal adapters make this transparent. |

### Why Not a Monolith?

A monolith was considered for the MVP. However, the strong bounded context boundaries in the domain (IoT ingestion vs. alerting vs. maintenance) and the vastly different non-functional requirements (throughput, latency, availability) made microservices the right choice even at MVP scale. The shared kernel library minimizes duplication overhead.

### Why Not Pure Event Sourcing?

Event sourcing was evaluated but rejected for MVP:
- Adds significant complexity for state reconstruction
- TimescaleDB already handles time-series data natively
- Can be introduced per-service later if audit requirements demand it

---

## 3. Hexagonal Architecture (Ports & Adapters)

### Core Principle

The **domain layer has zero framework dependencies**. It knows nothing about Spring, JPA, Kafka, or HTTP. This guarantees:

- Domain logic is testable with plain unit tests (no container, no mocks of framework classes)
- Frameworks can be replaced without touching business rules
- The architecture enforces separation of concerns structurally

### Layering (Inside-Out)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ADAPTERS (Infrastructure)                     │
│                                                                     │
│  ┌─────────────────────────────┐  ┌──────────────────────────────┐ │
│  │      DRIVING ADAPTERS       │  │       DRIVEN ADAPTERS        │ │
│  │      (Adapters IN)          │  │       (Adapters OUT)         │ │
│  │                             │  │                              │ │
│  │  - REST Controllers         │  │  - JPA Repositories         │ │
│  │  - Kafka Consumers          │  │  - Kafka Producers          │ │
│  │  - MQTT Subscribers         │  │  - Redis Cache Adapters     │ │
│  │  - Scheduled Tasks          │  │  - HTTP Clients (external)  │ │
│  │  - gRPC Endpoints (future)  │  │  - SMTP Adapters            │ │
│  └──────────────┬──────────────┘  └──────────────┬───────────────┘ │
│                 │                                 ▲                  │
│                 ▼                                 │                  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   APPLICATION LAYER                           │  │
│  │                                                              │  │
│  │  ┌─────────────────┐    ┌──────────────────────────────┐    │  │
│  │  │  PORTS IN        │    │  PORTS OUT                    │    │  │
│  │  │  (Use Cases)     │    │  (Repository/Publisher/Cache)  │    │  │
│  │  │                 │    │                              │    │  │
│  │  │  interfaces     │    │  interfaces                  │    │  │
│  │  │  that driving   │    │  that domain calls           │    │  │
│  │  │  adapters call  │    │  (implemented by driven      │    │  │
│  │  │                 │    │   adapters)                  │    │  │
│  │  └─────────────────┘    └──────────────────────────────┘    │  │
│  │                                                              │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │  USE CASE IMPLEMENTATIONS                             │   │  │
│  │  │  (Orchestrate domain objects via ports)               │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                      DOMAIN LAYER                             │  │
│  │                                                              │  │
│  │  - Aggregates, Entities, Value Objects                       │  │
│  │  - Domain Events                                             │  │
│  │  - Domain Services (stateless business logic)                │  │
│  │  - Business Rules & Invariants                               │  │
│  │                                                              │  │
│  │  *** NO framework dependencies ***                           │  │
│  │  *** NO annotations from Spring/JPA/Kafka ***                │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### Package Structure (per service)

```
com.pyrosense.{context}/
├── domain/
│   ├── model/          # Aggregates, entities, value objects
│   ├── valueobject/    # Standalone value objects (IDs, enums, measures)
│   ├── event/          # Domain events (immutable records)
│   └── service/        # Domain services (stateless logic)
├── application/
│   ├── port/
│   │   ├── in/         # Use case interfaces (driving ports)
│   │   └── out/        # Repository/publisher/cache interfaces (driven ports)
│   └── usecase/        # Use case implementations (orchestration)
├── adapter/
│   ├── in/
│   │   ├── rest/       # REST controllers (driving adapters)
│   │   ├── messaging/  # Kafka/MQTT consumers (driving adapters)
│   │   └── scheduler/  # Scheduled tasks (driving adapters)
│   └── out/
│       ├── persistence/# JPA repositories (driven adapters)
│       ├── messaging/  # Kafka producers (driven adapters)
│       ├── cache/      # Redis adapters (driven adapters)
│       └── external/   # External API clients (driven adapters)
└── config/             # Spring configuration, beans, properties
```

### Dependency Rule

Dependencies point **inward only**:

- `adapter` depends on `application` (implements ports, calls use cases)
- `application` depends on `domain` (orchestrates domain objects)
- `domain` depends on **nothing** (pure Java, no imports from outer layers)

---

## 4. Bounded Contexts

### Overview

The platform is decomposed into **9 bounded contexts**, each deployed as an independent microservice with its own database.

| # | Bounded Context | Responsibility | Key Aggregate | Key Event Published |
|---|-----------------|----------------|---------------|---------------------|
| 1 | **Identity** | User authentication, authorization, tenant management, RBAC | `User` | `user.created`, `user.role.assigned` |
| 2 | **Device** | Sensor lifecycle management, provisioning, health monitoring | `Device` | `device.registered`, `device.activated`, `device.offline` |
| 3 | **Ingestion** | Receive, validate, deduplicate, and persist telemetry readings from sensors | `TelemetryReading` | `ingestion.signal.ingested` |
| 4 | **Signal Analysis** | Detect anomalies in telemetry streams using statistical methods (z-score, moving averages) | `SignalWindow` | `analysis.anomaly.detected`, `analysis.baseline.updated` |
| 5 | **Risk Scoring** | Compute composite risk scores from anomalies, correlate multi-signal patterns | `RiskAssessment` | `scoring.risk.assessed`, `scoring.risk.escalated` |
| 6 | **Alerting** | Create, deduplicate, escalate, and manage alerts based on risk thresholds | `Alert` | `alerting.alert.created`, `alerting.alert.escalated` |
| 7 | **Notification** | Deliver alert notifications via multiple channels (email, SMS, push, webhook) | `Notification` | `notification.sent`, `notification.failed` |
| 8 | **Reporting** | Generate periodic and on-demand risk reports (PDF, CSV) | `Report` | `reporting.report.generated` |
| 9 | **Maintenance** | Track interventions from alert to resolution, assign technicians, track SLA | `Intervention` | `maintenance.intervention.created`, `maintenance.intervention.completed` |

### Service Deployment

| Service | Port | Database | Special Infrastructure |
|---------|------|----------|----------------------|
| pyrosense-identity-service | 8081 | pyrosense_identity (PostgreSQL) | Keycloak integration |
| pyrosense-device-service | 8082 | pyrosense_device (PostgreSQL) | — |
| pyrosense-ingestion-service | 8083 | pyrosense_ingestion (TimescaleDB) | MQTT broker, Redis (idempotency) |
| pyrosense-signal-analysis-service | 8084 | pyrosense_analysis (TimescaleDB) | Redis (window cache) |
| pyrosense-risk-scoring-service | 8085 | pyrosense_scoring (PostgreSQL) | Redis (score cache) |
| pyrosense-alerting-service | 8086 | pyrosense_alerting (PostgreSQL) | — |
| pyrosense-notification-service | 8087 | pyrosense_notification (PostgreSQL) | SMTP, SMS gateway, push service |
| pyrosense-reporting-service | 8088 | pyrosense_reporting (PostgreSQL) | Object storage (PDF files) |
| pyrosense-maintenance-service | 8089 | pyrosense_maintenance (PostgreSQL) | — |

---

## 5. C4 Diagrams

### Level 1: System Context

```
┌────────────────────┐                           ┌────────────────────────┐
│  Building Managers │                           │  Notification Channels │
│  (Web Dashboard)   │                           │                        │
└────────┬───────────┘                           │  - Email (SMTP)        │
         │ HTTPS                                 │  - SMS (Twilio)        │
         ▼                                       │  - Push (FCM/APNS)     │
┌────────────────────────────────────────┐       │  - Webhook             │
│                                        │       │  - Dashboard (SSE)     │
│      PyroSense AI Platform             │──────>└────────────────────────┘
│                                        │
│  Predictive Electrical Fire Prevention │
│                                        │       ┌────────────────────────┐
│  - Real-time sensor monitoring         │       │  External Systems      │
│  - Anomaly detection                   │       │                        │
│  - Risk scoring & alerting             │<─────>│  - BMS (Building Mgmt) │
│  - Maintenance workflows              │       │  - Insurance APIs      │
│                                        │       │  - CMMS (future)       │
└──────────┬─────────────────┬───────────┘       └────────────────────────┘
           │                 ▲
           │ HTTPS           │ MQTT (TLS)
           ▼                 │
┌────────────────────┐  ┌────────────────────┐
│  Technicians       │  │  IoT Sensors       │
│  (Mobile App)      │  │  (Per electrical   │
└────────────────────┘  │   panel)           │
                        └────────────────────┘
┌────────────────────┐
│  Insurers          │
│  (API / Reports)   │
└────────────────────┘
```

### Level 2: Container Diagram

```
                         ┌───────────────────────────────┐
                         │     Spring Cloud Gateway       │
                         │          :8080                 │
                         │  (JWT validation, routing,     │
                         │   rate limiting, CORS)         │
                         └───────────────┬───────────────┘
                                         │
         ┌───────────┬──────────┬────────┼────────┬──────────┬───────────┐
         │           │          │        │        │          │           │
         ▼           ▼          ▼        ▼        ▼          ▼           ▼
   ┌──────────┐┌──────────┐┌────────┐┌───────┐┌───────┐┌─────────┐┌─────────┐
   │ Identity ││ Device   ││Alerting││Report ││Maint. ││Risk     ││Signal   │
   │  :8081   ││  :8082   ││ :8086  ││ :8088 ││ :8089 ││Scoring  ││Analysis │
   └──────────┘└──────────┘└────────┘└───────┘└───────┘│ :8085   ││ :8084   │
                                                        └─────────┘└─────────┘
         │           │          │        │        │          │           │
         └───────────┴──────────┴────────┼────────┴──────────┴───────────┘
                                         │
                              ┌──────────┴──────────┐
                              │   Apache Kafka       │
                              │   (KRaft mode)       │
                              │   Domain Events Bus  │
                              └──────────┬──────────┘
                                         │
                                         ▲
                                         │
                         ┌───────────────┴───────────────┐
                         │    Ingestion Service :8083     │
                         └───────────────┬───────────────┘
                                         ▲
                                         │ MQTT
                         ┌───────────────┴───────────────┐
                         │    Eclipse Mosquitto (MQTT)    │
                         │    MQTT Broker :1883 / :8883   │
                         └───────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────┐
  │  Infrastructure                                                         │
  │                                                                         │
  │  [PostgreSQL 16]  [TimescaleDB]  [Redis 7]  [Keycloak 24]             │
  │  [Prometheus]     [Grafana]      [Loki]     [Tempo]                    │
  └─────────────────────────────────────────────────────────────────────────┘
```

### Level 3: Component Diagram - Ingestion Service

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Ingestion Service :8083                               │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  DRIVING ADAPTERS (Inbound)                                          │   │
│  │                                                                     │   │
│  │  ┌───────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │  MQTT Subscriber  │  │  REST Controller   │  │  Kafka Replay │  │   │
│  │  │  (sensor data)    │  │  (manual ingest)   │  │  (reprocess)  │  │   │
│  │  └────────┬──────────┘  └────────┬───────────┘  └───────┬───────┘  │   │
│  └───────────┼──────────────────────┼───────────────────────┼──────────┘   │
│              │                      │                       │               │
│              ▼                      ▼                       ▼               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  APPLICATION LAYER                                                   │   │
│  │                                                                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐    │   │
│  │  │  IngestTelemetryUseCase                                      │    │   │
│  │  │                                                             │    │   │
│  │  │  1. Check idempotency (deduplication key)                   │    │   │
│  │  │  2. Validate reading (schema + business rules)              │    │   │
│  │  │  3. Enrich with metadata (timestamp normalization)          │    │   │
│  │  │  4. Persist telemetry reading                               │    │   │
│  │  │  5. Publish SignalIngestedEvent to Kafka                    │    │   │
│  │  └─────────────────────────────────────────────────────────────┘    │   │
│  │                                                                     │   │
│  │  Ports OUT:                                                         │   │
│  │  - TelemetryRepository (persist readings)                           │   │
│  │  - IdempotencyStore (check/store dedup keys)                        │   │
│  │  - EventPublisher (publish domain events)                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│              │                      │                       │               │
│              ▼                      ▼                       ▼               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  DRIVEN ADAPTERS (Outbound)                                          │   │
│  │                                                                     │   │
│  │  ┌───────────────────┐  ┌────────────────────┐  ┌───────────────┐  │   │
│  │  │  TimescaleDB      │  │  Redis             │  │  Kafka        │  │   │
│  │  │  (persist         │  │  (idempotency      │  │  (publish     │  │   │
│  │  │   readings)       │  │   store, TTL 24h)  │  │   events)     │  │   │
│  │  └───────────────────┘  └────────────────────┘  └───────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Technical Choices (ADRs)

### Architecture Decision Records

| ADR | Decision | Justification |
|-----|----------|---------------|
| ADR-001 | **Java 21** | Virtual threads (Project Loom) for high-throughput IoT ingestion, pattern matching for cleaner domain logic, records for immutable value objects, sealed classes for type-safe state machines |
| ADR-002 | **Spring Boot 3.4** | Mature ecosystem, excellent Kafka/MQTT/JPA integration, smooth migration path to 4.x, massive community support |
| ADR-003 | **Apache Kafka (KRaft mode)** | At-least-once delivery guarantees, event replay capability, consumer groups for scaling, KRaft eliminates ZooKeeper dependency |
| ADR-004 | **TimescaleDB** | PostgreSQL-compatible time-series database, hypertables with automatic partitioning, continuous aggregates for dashboards, no separate time-series DB to operate |
| ADR-005 | **Hexagonal Architecture** | Framework-independent domain, testable without containers, adapter substitution for testing (in-memory repos), clean separation of concerns |
| ADR-006 | **Database-per-Service** | Loose coupling between services, independent schema evolution (Flyway per service), no shared mutable state, each DB sized to its workload |
| ADR-007 | **Spotless (code formatter)** | Consistent code style enforced in CI, no style debates in code review, automatic formatting on build |
| ADR-008 | **Flyway (schema migrations)** | Version-controlled schema changes, repeatable migrations for views/functions, rollback scripts, environment parity |
| ADR-009 | **Maven multi-module** | Centralized dependency management (BOM), shared build configuration (plugins, profiles), CI-friendly with parallel builds |
| ADR-010 | **Spring Cloud Gateway** | Single entry point, centralized JWT validation, rate limiting, CORS configuration, service routing, load balancing |
| ADR-011 | **Keycloak** | Standards-based OIDC/OAuth2, multi-tenant realm support, self-hosted (data sovereignty), admin UI for user management |
| ADR-012 | **Statistical analysis first (MVP)** | Interpretable results (explainable to building managers), fast to implement, does not require training data, ML can be added later behind the same port interface |
| ADR-013 | **Shared Kernel as library** | Minimal shared types (DomainEvent base class, AggregateRoot, strongly-typed IDs, common exceptions), published as internal Maven artifact, avoids duplication without coupling |
| ADR-014 | **Microservices (not monolith)** | Strong bounded context boundaries in domain, vastly different NFRs per context (ingestion throughput vs. reporting latency), independent deployment lifecycle |

---

## 7. Communication Patterns

### Synchronous: REST via API Gateway

**When used**: Client-facing queries, command responses that need immediate confirmation.

```
Client → HTTPS → Gateway (:8080) → Service REST endpoint → Response
```

Examples:
- Dashboard queries: "show me all active alerts for building X"
- Command acknowledgments: "register this new device" → return device ID
- User authentication: login/token refresh via Keycloak

**Constraints**:
- Gateway validates JWT on every request
- Rate limiting applied per tenant
- Circuit breaker pattern on inter-service calls (Resilience4j)

### Asynchronous: Kafka Domain Events

**When used**: Inter-service communication for domain events, eventual consistency.

```
Service A → publishes DomainEvent to Kafka topic → Service B (consumer group) processes
```

Examples:
- `ingestion.signal.ingested` → consumed by Signal Analysis
- `analysis.anomaly.detected` → consumed by Risk Scoring and Alerting
- `alerting.alert.created` → consumed by Notification and Maintenance

**Guarantees**:
- At-least-once delivery (idempotent consumers required)
- Ordered per partition (partition key = deviceId)
- Consumer group ensures each event processed by exactly one instance per service

### IoT Protocol: MQTT

**When used**: Sensor-to-platform telemetry ingestion.

```
Sensor → MQTT (TLS) → Mosquitto Broker → Ingestion Service (MQTT subscriber)
```

**Why MQTT**:
- Designed for constrained devices and unreliable networks
- Small packet overhead (minimal bandwidth)
- QoS levels (QoS 1 = at-least-once for telemetry)
- Persistent sessions for offline sensors

**Topic structure**: `pyrosense/{tenantId}/{buildingId}/{panelId}/{deviceId}/telemetry`

---

## 8. Data Flow

### End-to-End: From Sensor to Notification

```
┌──────────┐     ┌──────────┐     ┌───────────┐     ┌──────────────┐
│  Sensor  │────>│  MQTT    │────>│ Ingestion │────>│    Kafka     │
│  (IoT)   │MQTT │  Broker  │     │  Service  │     │ signal.      │
│          │     │          │     │  :8083    │     │ ingested     │
└──────────┘     └──────────┘     └───────────┘     └──────┬───────┘
                                                           │
                                                           ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────────────────┐
│    Kafka     │<────│   Signal     │<────│  Kafka consumer          │
│  anomaly.    │     │  Analysis    │     │  (signal-analysis-group) │
│  detected    │     │  :8084       │     │                          │
└──────┬───────┘     └──────────────┘     └──────────────────────────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐
│    Risk      │────>│    Kafka     │
│   Scoring    │     │  risk.       │
│   :8085      │     │  assessed    │
└──────────────┘     └──────┬───────┘
                            │
                            ▼
┌──────────────┐     ┌──────────────┐
│  Alerting    │────>│    Kafka     │
│   :8086      │     │  alert.      │
│              │     │  created     │
└──────────────┘     └──────┬───────┘
                            │
                ┌───────────┼───────────┐
                ▼                       ▼
┌──────────────────────┐  ┌──────────────────────┐
│  Notification        │  │  Maintenance         │
│  Service :8087       │  │  Service :8089       │
│                      │  │                      │
│  → Email             │  │  → Create            │
│  → SMS               │  │    Intervention      │
│  → Push              │  │                      │
│  → Webhook           │  │                      │
└──────────────────────┘  └──────────────────────┘
```

### Processing Latency Targets (MVP)

| Stage | Target Latency |
|-------|---------------|
| Sensor → Ingestion persisted | < 500ms |
| Ingestion → Anomaly detected | < 2s |
| Anomaly → Risk score computed | < 1s |
| Risk → Alert created | < 500ms |
| Alert → Notification sent | < 5s |
| **End-to-end (sensor → notification)** | **< 10s** |

---

## 9. Cross-Cutting Concerns

### Observability

- **Metrics**: Micrometer → Prometheus → Grafana dashboards
- **Logging**: Structured JSON → Loki (correlation via traceId)
- **Tracing**: Micrometer Tracing → Tempo (distributed trace visualization)
- **Alerting**: Grafana alerting rules on SLO violations

### Security

- **Authentication**: OAuth2/OIDC via Keycloak
- **Authorization**: RBAC with tenant isolation (every query filtered by tenantId)
- **Transport**: TLS everywhere (MQTT 8883, HTTPS)
- **Secrets**: Environment variables (Docker secrets in production)
- **API Gateway**: JWT validation, rate limiting, CORS

### Resilience

- **Circuit Breaker**: Resilience4j on outbound HTTP calls
- **Retry**: Exponential backoff on Kafka publish failures
- **Idempotency**: Redis-based deduplication keys (TTL 24h)
- **Dead Letter Queue**: Failed events routed to DLQ for manual inspection
- **Health checks**: Spring Boot Actuator /health for orchestrator probes

---

## 10. Limitations and Honest Constraints (MVP)

The following limitations are acknowledged for the current MVP phase:

| Limitation | Impact | Mitigation Path |
|------------|--------|-----------------|
| **No real ML model** | Detection is statistical only (z-score, thresholds). May miss complex failure patterns. | Hexagonal architecture allows ML adapter behind same port interface. |
| **No real hardware certification** | Sensors are simulated. No CE/UL mark. | Hardware partnership planned for Pilot phase. |
| **No NF C 15-100 compliance** | French electrical norm not formally validated. | Electrical engineer review planned for Pilot. |
| **No load testing at scale** | Platform not validated beyond ~100 simulated sensors. | Load testing with Gatling planned before Pilot. |
| **No multi-region deployment** | Single region only. No geo-redundancy. | Cloud-native patterns (Kubernetes) ready for scale-out. |
| **No blue-green deployment** | Deployments require brief downtime or rolling restart. | Kubernetes rolling update strategy for Pilot. |
| **No edge computing** | All processing is cloud-side. Sensor failures during network outage lose data. | Edge inference agent planned for Production phase. |
| **No formal SLA guarantees** | Latency targets are best-effort. | SLO definition and monitoring for Pilot phase. |
| **Simulated notifications** | Email/SMS not connected to real providers in MVP. | Adapter pattern allows drop-in Twilio/SendGrid integration. |

---

## 11. Anti-Corruption Rules

These rules are enforced by code review and architecture tests (ArchUnit):

1. **Domain purity**: No Spring/JPA/Kafka annotations in domain layer. Zero framework imports.
2. **Port contract**: Use cases return domain objects, not DTOs. Adapters handle mapping.
3. **Adapter isolation**: Each adapter has its own model (JPA entity != domain entity, Kafka message != domain event).
4. **No cross-service domain sharing**: Only shared-kernel primitives (base classes, IDs) cross service boundaries.
5. **Event-driven decoupling**: Services never call each other's REST APIs for commands. Only queries (read) may use synchronous calls.
6. **Tenant isolation**: Every database query includes tenantId. No data leakage between tenants.
7. **Immutable events**: Domain events are records (immutable). Once published, never modified.
8. **Single aggregate per transaction**: Each use case modifies at most one aggregate. Cross-aggregate consistency via events.

---

## 12. Future Architecture Evolution

| Phase | Architecture Changes |
|-------|---------------------|
| **Pilot** | Add Gatling load tests, connect real notification providers, add SLO monitoring, Kubernetes deployment |
| **Production** | Add ML service (Python, behind port interface), edge inference agent, multi-region with event replication, blue-green deployments, formal security audit |
| **Scale** | Event sourcing for audit-critical services, CQRS for reporting, GraphQL gateway for flexible queries, data lake for historical analysis |
