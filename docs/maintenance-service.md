# PyroSense Maintenance Service

## Overview

The Maintenance Service manages the lifecycle of preventive interventions, transforming PyroSense alerts into traceable field actions. It ensures that every electrical risk detection leads to a structured response — from planning through completion and feedback.

**Port:** 8086  
**Kafka topics:** maintenance-events (producer), alerting-events (consumer), risk-reevaluation-requests (producer)

## Architecture

```
┌─────────────────────────────────────────────────────┐
│ REST Controller (adapter/in/rest)                    │
│   └─ InterventionController                         │
├─────────────────────────────────────────────────────┤
│ Kafka Listener (adapter/in/messaging)               │
│   └─ KafkaAlertEventListener                        │
├─────────────────────────────────────────────────────┤
│ Use Cases (application/usecase)                     │
│   ├─ CreateInterventionService                      │
│   ├─ ManageInterventionService                      │
│   ├─ AddInterventionCommentService                  │
│   └─ GetInterventionService                         │
├─────────────────────────────────────────────────────┤
│ Ports (application/port)                            │
│   ├─ in: CreateInterventionUseCase,                 │
│   │      ManageInterventionUseCase,                 │
│   │      AddInterventionCommentUseCase,             │
│   │      GetInterventionQuery                       │
│   └─ out: InterventionRepositoryPort,               │
│           MaintenanceEventPublisherPort,             │
│           RiskScoreReevaluationPublisherPort,        │
│           AlertLookupPort, TechnicianLookupPort,     │
│           AuditLogPort                              │
├─────────────────────────────────────────────────────┤
│ Domain (domain/model + domain/event)                │
│   ├─ Intervention aggregate                         │
│   ├─ InterventionStatus, Priority, Result, Type     │
│   ├─ FieldDiagnostic, RiskImpact, InterventionComment│
│   └─ 8 domain events                               │
├─────────────────────────────────────────────────────┤
│ Adapters (adapter/out)                              │
│   ├─ JdbcInterventionRepository (persistence)       │
│   ├─ KafkaMaintenanceEventPublisher (messaging)     │
│   ├─ KafkaRiskReevaluationPublisher (messaging)     │
│   ├─ LoggingAuditLogAdapter (stub)                  │
│   ├─ StubAlertLookupAdapter (stub)                  │
│   └─ StubTechnicianLookupAdapter (stub)             │
└─────────────────────────────────────────────────────┘
```

## Intervention Lifecycle

```
CREATED → PLANNED → ASSIGNED → IN_PROGRESS → COMPLETED
    ↓                                            ↑
    └──────────────── CANCELLED ←────────────────┘
         (from any non-terminal state)
```

### Priorities
- **LOW** — routine preventive
- **MEDIUM** — scheduled within week
- **HIGH** — schedule within 48h
- **URGENT** — immediate response (auto-created from CRITICAL alerts)

### Results (on completion)
- CONFIRMED_DEFECT — electrical defect validated
- NO_DEFECT_FOUND — false positive
- REPAIRED — defect fixed in-place
- REPLACED_COMPONENT — component swap
- NEEDS_FOLLOW_UP — requires additional intervention
- FALSE_POSITIVE — no issue found

## Business Rules

| # | Rule | Enforcement |
|---|------|-------------|
| 1 | CRITICAL alert → auto-propose URGENT intervention | KafkaAlertEventListener + CreateInterventionService |
| 2 | Cannot complete without diagnostic | Intervention.complete() throws if diagnostic == null |
| 3 | COMPLETED triggers risk score reevaluation | ManageInterventionService → RiskScoreReevaluationPublisherPort |
| 4 | FALSE_POSITIVE feeds feedback loop | FalsePositiveConfirmedEvent published |
| 5 | CANCELLED must have reason | Intervention.cancel(reason) requires non-blank reason |
| 6 | Electrician can only modify assigned interventions | @PreAuthorize per endpoint |
| 7 | PROPERTY_MANAGER can plan and assign | @PreAuthorize on schedule/assign endpoints |
| 8 | All actions audited | AuditLogPort adapter (logging) |

## REST API

### Create
```
POST /api/v1/interventions/from-alert/{alertId}
POST /api/v1/interventions
```

### Lifecycle
```
POST /api/v1/interventions/{id}/schedule
POST /api/v1/interventions/{id}/assign
POST /api/v1/interventions/{id}/start
POST /api/v1/interventions/{id}/diagnostic
POST /api/v1/interventions/{id}/complete
POST /api/v1/interventions/{id}/risk-impact
POST /api/v1/interventions/{id}/cancel
POST /api/v1/interventions/{id}/comments
```

### Queries
```
GET /api/v1/interventions
GET /api/v1/interventions/{id}
GET /api/v1/interventions/electrician/{id}
GET /api/v1/interventions/device/{deviceId}
GET /api/v1/interventions/overdue
GET /api/v1/interventions/kanban
GET /api/v1/interventions/statistics
```

## Kafka Events

| Event | Topic | Trigger |
|-------|-------|---------|
| MaintenanceInterventionCreatedEvent | maintenance-events | Intervention created |
| MaintenanceInterventionPlannedEvent | maintenance-events | Scheduled |
| MaintenanceInterventionAssignedEvent | maintenance-events | Electrician assigned |
| MaintenanceInterventionStartedEvent | maintenance-events | Work started |
| MaintenanceInterventionCompletedEvent | maintenance-events | Work completed |
| MaintenanceInterventionCancelledEvent | maintenance-events | Intervention cancelled |
| ElectricalDefectConfirmedEvent | maintenance-events | Completion with defect result |
| FalsePositiveConfirmedEvent | maintenance-events | Completion with false positive |

## Database Schema

### interventions table
- id, tenant_id, source_alert_id (unique), device_id
- type, priority, description, status
- assigned_electrician_id, scheduled_at, started_at, completed_at
- result, diagnostic (JSONB), risk_impact (JSONB), cancellation_reason
- created_at, updated_at

### intervention_comments table
- id, intervention_id (FK), author_id, content, created_at

## Role-Based Access

| Role | Create | Plan/Assign | Start/Complete | Comment | View |
|------|--------|-------------|----------------|---------|------|
| PLATFORM_ADMIN | Yes | Yes | Yes | Yes | Yes |
| TENANT_ADMIN | Yes | Yes | Yes | Yes | Yes |
| PROPERTY_MANAGER | Yes | Yes | Yes | Yes | Yes |
| ELECTRICIAN | No | No | Yes (assigned) | Yes | Yes |
| SUPPORT_READONLY | No | No | No | No | Yes |

## Configuration

```yaml
spring:
  kafka:
    consumer:
      group-id: maintenance-service
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

## Running

```bash
mvn clean package -pl pyrosense-maintenance-service -am
java -jar pyrosense-maintenance-service/target/pyrosense-maintenance-service-0.1.0-SNAPSHOT.jar
```
