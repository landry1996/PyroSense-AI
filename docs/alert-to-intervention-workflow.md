# Alert → Intervention → Resolution → Feedback Workflow

## Overview

This document describes the end-to-end workflow from alert detection to intervention completion and feedback loop. The workflow ensures that every electrical risk detection leads to a traceable field action with auditability, idempotence, and SLA enforcement.

## Workflow Diagram

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
│  Alert Service  │────▶│  Maintenance Service  │────▶│  Risk Scoring Svc   │
│ (alerting-events)     │ (Kafka consumer)      │     │ (risk-reevaluation) │
└─────────────────┘     └──────────────────────┘     └─────────────────────┘
                              │         │
                    ┌─────────┘         └──────────┐
                    ▼                              ▼
           ┌──────────────┐              ┌─────────────────┐
           │ Intervention │              │ Recommendation  │
           │ (CRITICAL)   │              │ (WARNING)       │
           │ auto-created │              │ pending accept  │
           └──────────────┘              └─────────────────┘
                    │                              │
                    ▼                              ▼ (manager accepts)
           ┌──────────────────────────────────────────────┐
           │              Intervention Lifecycle            │
           │ CREATED → ASSIGNED → IN_PROGRESS → COMPLETED │
           └──────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼────────────────┐
                    ▼               ▼                ▼
           ┌──────────────┐ ┌─────────────┐ ┌────────────────┐
           │ Notification │ │   Risk      │ │   Reporting    │
           │   Service    │ │  Scoring    │ │    Service     │
           └──────────────┘ └─────────────┘ └────────────────┘
```

## Step-by-Step Flow

### 1. Alert Created Event Received

The `KafkaAlertEventListener` consumes events from the `alerting-events` topic and filters for `alerting.alert.created` events.

### 2-3. Severity-Based Routing

| Alert Severity | Action | Priority | SLA |
|---|---|---|---|
| CRITICAL | Auto-create intervention | URGENT | Response: 4h, Resolution: 24h |
| WARNING (score ≥ 70) | Create recommendation | HIGH | Response: 24h, Resolution: 3d |
| WARNING (score < 70) | Create recommendation | MEDIUM | Response: 3d, Resolution: 7d |

**CRITICAL alerts** bypass the recommendation step — an intervention is created immediately with `URGENT` priority and `EMERGENCY` type.

**WARNING alerts** create an `InterventionRecommendation` with an SLA deadline. The recommendation remains `PENDING` until a manager acts on it.

### 4. Manager Accepts/Rejects Recommendation

```
GET  /api/v1/recommendations           → list pending recommendations
POST /api/v1/recommendations/{id}/accept → create intervention from recommendation
POST /api/v1/recommendations/{id}/reject → reject with mandatory reason
```

Roles: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `PROPERTY_MANAGER`

### 5. Intervention Created

Status: `CREATED`. Published event: `MaintenanceInterventionCreatedEvent`

### 6. Electrician Assigned

```
POST /api/v1/interventions/{id}/assign
```
Status: `ASSIGNED`. Published event: `MaintenanceInterventionAssignedEvent`

### 7. Work Started

```
POST /api/v1/interventions/{id}/start
```
Status: `IN_PROGRESS`. Published event: `MaintenanceInterventionStartedEvent`

### 8. Diagnostic Added

```
POST /api/v1/interventions/{id}/diagnostic
```
Required before completion. Captures: observations, measurements, recommendations, diagnosticBy.

### 9. Intervention Completed

```
POST /api/v1/interventions/{id}/complete
```
Status: `COMPLETED`. Requires diagnostic to be present.

### 10. Events Published on Completion

| Event | Condition |
|---|---|
| `MaintenanceInterventionCompletedEvent` | Always |
| `ElectricalDefectConfirmedEvent` | Result = CONFIRMED_DEFECT, REPAIRED, REPLACED_COMPONENT, NEEDS_FOLLOW_UP |
| `FalsePositiveConfirmedEvent` | Result = NO_DEFECT_FOUND or FALSE_POSITIVE |

### 11. Risk Score Reevaluation

On completion, the maintenance service publishes a `risk-reevaluation-requests` Kafka message. The Risk Scoring Service recalculates the device's score incorporating the intervention outcome.

### 12. Notification Dispatch

The Notification Service subscribes to `maintenance-events` and dispatches notifications:
- Manager: intervention completed summary
- Electrician: assignment notifications
- Tenant admin: SLA breach warnings

### 13. Reporting Integration

The Reporting Service queries interventions via the maintenance service API to include them in:
- Monthly compliance reports
- Building risk assessment reports
- Intervention history reports

## Domain Model

### InterventionRecommendation

```java
InterventionRecommendation {
    id, tenantId, alertId, deviceId,
    suggestedType, suggestedPriority, reason,
    slaDeadline, slaExpiresAt,
    status (PENDING → ACCEPTED | REJECTED | EXPIRED),
    rejectionReason, acceptedInterventionId, decidedAt
}
```

### InterventionPriorityPolicy

Pure domain logic that determines priority and type from alert severity and risk score:
- CRITICAL → URGENT + EMERGENCY
- WARNING + score ≥ 70 → HIGH + PREVENTIVE
- WARNING + score < 70 → MEDIUM + PREVENTIVE

### SlaPolicy

Defines response and resolution deadlines per priority level:
- URGENT: 4h response, 24h resolution
- HIGH: 24h response, 3d resolution
- MEDIUM: 3d response, 7d resolution
- LOW: 7d response, 30d resolution

## Idempotence & Concurrency

### Alert Deduplication
- Unique DB index on `interventions.source_alert_id` prevents duplicate interventions
- Unique DB index on `intervention_recommendations.alert_id` prevents duplicate recommendations
- `CreateInterventionService` checks `findBySourceAlertId()` before insert
- `KafkaAlertEventListener` checks `findByAlertId()` before creating recommendations

### Kafka Idempotence
- Consumer group ensures each partition's events are processed by exactly one instance
- Business-level idempotence via unique alert ID check (safe against consumer rebalancing/replay)

### Concurrent Access
- Database unique indexes are the final safety net for race conditions
- Service-level checks provide fast-path rejection without hitting the DB for most duplicates

## Audit Trail

All workflow actions are logged through `AuditLogPort`:
- `AUTO_INTERVENTION_CREATED` — CRITICAL alert auto-created intervention
- `RECOMMENDATION_CREATED` — WARNING alert created recommendation
- `RECOMMENDATION_ACCEPTED` — Manager accepted recommendation
- `RECOMMENDATION_REJECTED` — Manager rejected recommendation with reason

## Database Schema

### intervention_recommendations table (V003)

| Column | Type | Description |
|---|---|---|
| id | UUID PK | Recommendation ID |
| tenant_id | UUID | Tenant scope |
| alert_id | UUID UNIQUE | Source alert (idempotence key) |
| device_id | UUID | Target device |
| suggested_type | VARCHAR(20) | PREVENTIVE, EMERGENCY, etc. |
| suggested_priority | VARCHAR(10) | URGENT, HIGH, MEDIUM, LOW |
| reason | TEXT | Human-readable reason |
| sla_deadline_seconds | BIGINT | SLA duration in seconds |
| sla_expires_at | TIMESTAMPTZ | Absolute SLA expiry |
| status | VARCHAR(10) | PENDING, ACCEPTED, REJECTED, EXPIRED |
| rejection_reason | TEXT | If rejected |
| accepted_intervention_id | UUID FK | Created intervention if accepted |
| decided_at | TIMESTAMPTZ | When decision was made |
| created_at | TIMESTAMPTZ | Record creation |

## Error Handling

| Scenario | Behavior |
|---|---|
| Duplicate alert (intervention exists) | Skip silently (idempotent) |
| Duplicate alert (recommendation exists) | Skip silently (idempotent) |
| Accept already-decided recommendation | 409 Conflict |
| Reject with blank reason | 400 Bad Request |
| Complete without diagnostic | 409 Conflict |
| Cancel from terminal state | 409 Conflict |

## Configuration

```yaml
pyrosense:
  maintenance:
    kafka:
      alerting-topic: alerting-events
      output-topic: maintenance-events
spring:
  kafka:
    consumer:
      group-id: maintenance-service
```
