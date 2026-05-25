# PyroSense Maintenance Service

## Overview

The Maintenance Service transforms alerts into preventive interventions, tracks field work by electricians, measures risk reduction impact, and feeds false positive data back to the AI pipeline.

## Architecture

```
                ┌─────────────────────────────────────────────────┐
                │           Maintenance Service (:8089)           │
                │                                                  │
 Kafka ──────► │  KafkaAlertEventListener                        │
(alerting-     │        │                                          │
 events)       │        ▼                                          │
               │  CreateInterventionService ◄── REST API          │
               │        │                         │                │
               │        ▼                         ▼                │
               │  ManageInterventionService  GetInterventionService│
               │        │                                          │
               │        ▼                                          │
               │  InterventionRepositoryPort (JDBC)               │
               │        │                                          │
               │        ▼                                          │
               │  MaintenanceEventPublisherPort ─────────►  Kafka │
               │                                    (maintenance- │
               │                                     events)      │
               └─────────────────────────────────────────────────┘
```

## Domain Model

### Intervention (Aggregate Root)

The central entity representing a maintenance intervention triggered by an alert.

**State Machine:**
```
CREATED ──► PLANNED ──► ASSIGNED ──► IN_PROGRESS ──► COMPLETED
   │            │            │             │
   └─── CANCELLED ◄─────────┴─────────────┘
```

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| tenantId | TenantId | Owning tenant |
| sourceAlertId | AlertId | Alert that triggered this intervention |
| deviceId | DeviceId | Target device |
| type | InterventionType | PREVENTIVE, CORRECTIVE, PREDICTIVE, EMERGENCY |
| priority | InterventionPriority | LOW, MEDIUM, HIGH, URGENT |
| status | InterventionStatus | Current lifecycle state |
| assignedElectricianId | UserId | Assigned technician |
| scheduledAt | Instant | Planned execution time |
| diagnostic | FieldDiagnostic | Field observations (JSONB) |
| result | InterventionResult | Outcome of the intervention |
| riskImpact | RiskImpact | Before/after risk measurement |

### InterventionResult

| Result | Description | Feedback Loop |
|--------|-------------|---------------|
| CONFIRMED_DEFECT | Defect found as predicted | Positive reinforcement for AI model |
| NO_DEFECT_FOUND | False positive | Negative feedback — AI model needs adjustment |
| REPAIRED | Defect found and fixed | Positive reinforcement + risk reduction |
| REPLACED_COMPONENT | Component replaced | Positive reinforcement + risk reduction |
| NEEDS_FOLLOW_UP | Partial resolution | Schedule follow-up intervention |

### RiskImpact

Measures the effectiveness of the intervention:
- `riskScoreBefore`: Risk score at time of intervention creation
- `riskScoreAfter`: Risk score after completion (re-evaluated)
- `avoidedIncidentEstimateDays`: Estimated days to incident if no action taken
- `riskReduction()`: Computed delta (before - after, clamped to 0)

## Business Rules

1. **CRITICAL alerts automatically propose an intervention** — via Kafka consumer, CRITICAL and WARNING alerts trigger `CreateInterventionService`.

2. **One intervention per alert** — duplicate prevention via unique index on `source_alert_id`.

3. **Completed interventions trigger risk re-evaluation** — `MaintenanceInterventionCompletedEvent` signals the risk-scoring service to recalculate.

4. **False positives feed the AI feedback loop** — `FalsePositiveConfirmedEvent` is consumed by the signal-analysis service to adjust detection thresholds.

5. **All modifications are audited** — state transitions publish domain events via Kafka.

6. **Alert severity maps to intervention type and priority:**
   - CRITICAL → EMERGENCY + URGENT priority
   - WARNING → PREVENTIVE + HIGH priority
   - INFO → PREDICTIVE + MEDIUM priority

## Domain Events

| Event | Published When | Consumers |
|-------|---------------|-----------|
| `MaintenanceInterventionCreatedEvent` | Intervention created | Notification service (alert electricians) |
| `MaintenanceInterventionCompletedEvent` | Intervention completed | Risk-scoring service (re-evaluate) |
| `ElectricalDefectConfirmedEvent` | Result = CONFIRMED_DEFECT / REPAIRED / REPLACED | Signal-analysis (positive feedback) |
| `FalsePositiveConfirmedEvent` | Result = NO_DEFECT_FOUND | Signal-analysis (threshold adjustment) |

## API Endpoints

### Create & Manage

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/v1/interventions` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER | Create from alert |
| POST | `/{id}/schedule` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER | Set scheduled date |
| POST | `/{id}/assign` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER | Assign electrician |
| POST | `/{id}/start` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, ELECTRICIAN | Begin work |
| POST | `/{id}/diagnostic` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, ELECTRICIAN | Record field observations |
| POST | `/{id}/complete` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, ELECTRICIAN | Complete with result |
| POST | `/{id}/risk-impact` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER | Record risk before/after |
| POST | `/{id}/cancel` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER | Cancel intervention |

### Query

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/{id}` | All except OCCUPANT, DEVICE | Get by ID |
| GET | `/` | All except OCCUPANT, DEVICE | List by tenant (+ optional status filter) |
| GET | `/electrician/{id}` | All except OCCUPANT, DEVICE | List by assigned electrician |
| GET | `/device/{id}` | All except OCCUPANT, DEVICE | List by device |
| GET | `/statistics` | ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, SUPPORT_READONLY | Aggregated stats |

### Request/Response Examples

**Create Intervention:**
```json
POST /api/v1/interventions
{
  "tenantId": "uuid",
  "alertId": "uuid",
  "deviceId": "uuid",
  "severity": "CRITICAL",
  "alertType": "MICRO_ARC",
  "description": "Critical micro-arc detected on circuit C7, panel B3"
}
```

**Complete with Result:**
```json
POST /api/v1/interventions/{id}/complete
{
  "result": "REPAIRED"
}
```

**Record Risk Impact:**
```json
POST /api/v1/interventions/{id}/risk-impact
{
  "riskScoreBefore": 85,
  "riskScoreAfter": 20,
  "avoidedIncidentEstimateDays": 45
}
```

**Statistics Response:**
```json
{
  "total": 142,
  "inProgress": 8,
  "completed": 115,
  "falsePositives": 12,
  "averageRiskReduction": 42.5
}
```

## Kafka Integration

### Consumer
- **Topic:** `alerting-events`
- **Event filter:** `alerting.alert.created` with severity CRITICAL or WARNING
- **Action:** Auto-creates intervention with appropriate type/priority

### Producer
- **Topic:** `maintenance-events`
- **Events:** All domain events (created, completed, defect confirmed, false positive)

## Database Schema

```sql
CREATE TABLE interventions (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    source_alert_id UUID NOT NULL UNIQUE,
    device_id       UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    priority        VARCHAR(10) NOT NULL,
    description     TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    assigned_electrician_id UUID,
    scheduled_at    TIMESTAMP WITH TIME ZONE,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    result          VARCHAR(30),
    diagnostic      JSONB,
    risk_impact     JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### Indexes
- `idx_interventions_tenant` — tenant-level queries
- `idx_interventions_tenant_status` — filtered by status
- `idx_interventions_device` — device history
- `idx_interventions_electrician` — electrician workload (partial)
- `idx_interventions_scheduled` — upcoming work (partial WHERE status IN PLANNED/ASSIGNED)
- `idx_interventions_alert` — unique, one intervention per alert

## Feedback Loop Architecture

```
Alert ──► Intervention ──► Field Diagnostic ──► Result
                                                    │
                          ┌─────────────────────────┤
                          │                         │
                          ▼                         ▼
              (DEFECT CONFIRMED)          (FALSE POSITIVE)
                          │                         │
                          ▼                         ▼
              Risk Scoring Service         Signal Analysis Service
              "recalculate score"          "adjust thresholds"
                          │                         │
                          ▼                         ▼
              Lower risk score             Fewer false alerts
```

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `maintenance.interventions.created` | Counter | Total interventions created |
| `maintenance.interventions.completed` | Counter | Total interventions completed |
| `maintenance.false_positives` | Counter | False positive count |
| `maintenance.risk_reduction.average` | Gauge | Rolling average risk reduction |
| `maintenance.resolution.duration` | Timer | Time from creation to completion |

## Configuration

```yaml
pyrosense:
  maintenance:
    kafka:
      alerting-topic: alerting-events
      output-topic: maintenance-events
```
