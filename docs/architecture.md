# PyroSense AI Platform - Architecture Document

## 1. Vision

PyroSense AI is a predictive electrical fire prevention platform. It combines IoT sensors, real-time signal analysis, and risk scoring to detect early signs of electrical faults (micro-arcs, insulation degradation, loose connections, overheating) before they cause fires.

## 2. Architecture Style

**Microservices + Hexagonal Architecture + DDD Tactical Patterns**

Each bounded context is deployed as an independent service with its own database (Database-per-Service pattern). Services communicate via:
- **Synchronous**: REST via API Gateway (client-facing queries)
- **Asynchronous**: Apache Kafka events (inter-service domain events)

## 3. C4 Context Diagram (Level 1)

```
+------------------+         +-----------------------------+
|  Building        |         |                             |
|  Managers        |--HTTPS->|    PyroSense AI Platform    |
+------------------+         |                             |
                             |  [API Gateway :8080]        |
+------------------+         |  [9 Microservices]          |
|  Technicians     |--HTTPS->|  [Kafka Event Bus]         |
+------------------+         |  [PostgreSQL + TimescaleDB] |
                             |                             |
+------------------+         +-----------------------------+
|  IoT Sensors     |--MQTT-->|  Ingestion Service :8083    |
+------------------+         +-----------------------------+
                                        |
                                        v
                             +-----------------------------+
                             |  Notification Channels      |
                             |  (Email, SMS, Webhook)      |
                             +-----------------------------+
```

## 4. C4 Container Diagram (Level 2)

```
                    [API Gateway :8080]
                           |
        +------------------+------------------+
        |                  |                  |
   [Identity]        [Device]           [Alerting]
    :8081              :8082              :8086
        |                  |                  |
        +--------+---------+--------+---------+
                 |                   |
           [Ingestion]         [Notification]
             :8083               :8087
                 |
    [Signal Analysis]  -->  [Risk Scoring]
         :8084                  :8085
                                    |
                    +---------------+---------------+
                    |                               |
              [Reporting]                   [Maintenance]
                :8088                          :8089

Infrastructure:
  [PostgreSQL+TimescaleDB] [Kafka KRaft] [Redis] [Mosquitto] [Keycloak]
  [Prometheus] [Grafana] [Loki]
```

## 5. Bounded Contexts

| # | Bounded Context | Service | Port | Database |
|---|-----------------|---------|------|----------|
| 1 | Identity & Access | pyrosense-identity-service | 8081 | pyrosense_identity |
| 2 | Device Management | pyrosense-device-service | 8082 | pyrosense_device |
| 3 | Signal Ingestion | pyrosense-ingestion-service | 8083 | pyrosense_ingestion |
| 4 | Signal Analysis | pyrosense-signal-analysis-service | 8084 | pyrosense_analysis |
| 5 | Risk Assessment | pyrosense-risk-scoring-service | 8085 | pyrosense_scoring |
| 6 | Alert Management | pyrosense-alerting-service | 8086 | pyrosense_alerting |
| 7 | Notification | pyrosense-notification-service | 8087 | pyrosense_notification |
| 8 | Reporting | pyrosense-reporting-service | 8088 | pyrosense_reporting |
| 9 | Maintenance | pyrosense-maintenance-service | 8089 | pyrosense_maintenance |

## 6. Event Flow (Domain Events via Kafka)

```
ingestion.signal.ingested
    -> signal-analysis-service (consumer group: signal-analysis-group)

analysis.anomaly.detected
    -> risk-scoring-service (consumer group: risk-scoring-group)
    -> alerting-service (consumer group: alerting-group)

scoring.risk.updated
    -> alerting-service
    -> maintenance-service (consumer group: maintenance-group)

alerting.alert.created
    -> notification-service (consumer group: notification-group)
    -> maintenance-service

device.installation.created
    -> (future consumers)
```

## 7. Hexagonal Architecture (per service)

Each service follows this internal structure:

```
com.pyrosense.{context}/
├── domain/
│   ├── model/          # Aggregates, entities, value objects
│   ├── valueobject/    # Standalone value objects
│   ├── event/          # Domain events
│   └── service/        # Domain services (stateless logic)
├── application/
│   ├── port/
│   │   ├── in/         # Use case interfaces (driving ports)
│   │   └── out/        # Repository/publisher interfaces (driven ports)
│   └── usecase/        # Use case implementations
├── adapter/
│   ├── in/
│   │   ├── rest/       # REST controllers (driving adapters)
│   │   └── messaging/  # Kafka consumers (driving adapters)
│   └── out/
│       ├── persistence/# JPA repositories (driven adapters)
│       ├── messaging/  # Kafka producers (driven adapters)
│       └── external/   # External API clients
└── config/             # Spring configuration
```

## 8. Architectural Decision Records (ADRs)

| ADR | Decision | Rationale |
|-----|----------|-----------|
| ADR-001 | Microservices | Independent deployment, team autonomy, bounded context alignment |
| ADR-002 | Hexagonal per service | Testable domain, framework independence, adapter substitution |
| ADR-003 | Java 21 | Virtual threads, pattern matching, records, sealed classes |
| ADR-004 | Spring Boot 3.4.x | Stable, mature ecosystem, easy migration to 4.x |
| ADR-005 | Kafka for events | At-least-once delivery, replay, consumer groups, KRaft simplicity |
| ADR-006 | Database per service | Loose coupling, independent scaling, schema autonomy |
| ADR-007 | TimescaleDB | Time-series native in PostgreSQL, continuous aggregates |
| ADR-008 | API Gateway | Single entry point, JWT validation, rate limiting, CORS |
| ADR-009 | Keycloak | Standards-based OIDC, multi-tenant, self-hosted |
| ADR-010 | Statistical analysis first | Interpretable, fast MVP, ML extension via interface |
| ADR-011 | Shared Kernel library | Minimal shared types (DomainEvent, AggregateRoot, exceptions) |
| ADR-012 | Maven multi-module | Centralized versions, shared build config, CI-friendly |

## 9. Anti-Corruption Rules

1. **Domain purity**: No Spring/JPA/Kafka annotations in domain layer
2. **Port contract**: Use cases return domain objects, not DTOs
3. **Adapter isolation**: Each adapter has its own model (JPA entity, Kafka message)
4. **No cross-service domain sharing**: Only shared-kernel primitives cross boundaries
5. **Event-driven decoupling**: Services never call each other's REST APIs directly for commands
