# PyroSense Alerting Service

## Overview

The alerting service manages the full lifecycle of alerts generated from the signal analysis and risk scoring pipeline. It handles alert creation, deduplication, assignment, escalation, SLA tracking, and resolution.

## Architecture

```
scoring-events ──┐
                 ├──► KafkaScoringEventListener ──► CreateAlertUseCase ──► Alert Aggregate
analysis-events ─┘                                                              │
                                                                                ▼
REST API ◄── AlertController ◄── ManageAlertUseCase / GetAlertQuery     JdbcAlertRepository
                                                                                │
EscalationScheduler ──► EscalateAlertsUseCase                           alerting-events ──► Kafka
```

## Domain Model

### Alert (Aggregate Root)

The `Alert` is a rich aggregate with full state machine lifecycle:

- **States**: `OPEN` → `ACKNOWLEDGED` → `IN_PROGRESS` → `RESOLVED` | `FALSE_POSITIVE`
- **Deduplication**: Same device + same alert type = increment occurrence count instead of new alert
- **SLA Deadlines**: CRITICAL < 24h, WARNING < 7 days, INFO < 30 days
- **Escalation**: Automatic for CRITICAL alerts not actioned within escalation interval (default 4h)
- **Comments**: Audit trail of actions and notes on the alert

### Alert Types

| Type | Default Severity | Trigger |
|------|------------------|---------|
| MICRO_ARC_DETECTED | CRITICAL | Signal analysis anomaly |
| INSULATION_DEGRADATION | WARNING | HF noise anomaly |
| LOOSE_CONNECTION | WARNING | Signal analysis |
| OVERHEATING | CRITICAL | Temperature anomaly |
| ABNORMAL_TRANSIENT | WARNING | Transient anomaly |
| HARMONIC_DISTORTION | WARNING | THD anomaly |
| LOAD_IMBALANCE | INFO | Signal analysis |
| SENSOR_OFFLINE | INFO | Device heartbeat timeout |
| CRITICAL_RISK_SCORE | CRITICAL | Risk score ≥ 80 |
| HIGH_RISK_SCORE | WARNING | Risk level changed to HIGH |
| BASELINE_DEVIATION | INFO | Baseline drift detected |

### Escalation Levels

`NONE` → `FIRST` → `SECOND` → `EMERGENCY`

Escalation occurs automatically every `escalation-interval` (4h default) for unactioned CRITICAL alerts.

## Domain Events

| Event | When |
|-------|------|
| `AlertCreatedEvent` | New alert created (not deduplicated) |
| `AlertAcknowledgedEvent` | User acknowledges alert |
| `AlertAssignedEvent` | Alert assigned to technician |
| `AlertResolvedEvent` | Alert resolved or marked false positive |
| `AlertEscalatedEvent` | Alert escalated to next level |

## REST API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/alerts` | GET | List alerts (filter by tenant, status, severity, device) |
| `/api/v1/alerts/{id}` | GET | Get alert details |
| `/api/v1/alerts/critical` | GET | List open critical alerts |
| `/api/v1/alerts/statistics` | GET | Alert statistics by tenant |
| `/api/v1/alerts/{id}/acknowledge` | POST | Acknowledge alert |
| `/api/v1/alerts/{id}/assign` | POST | Assign alert to user |
| `/api/v1/alerts/{id}/resolve` | POST | Resolve alert |
| `/api/v1/alerts/{id}/false-positive` | POST | Mark as false positive |
| `/api/v1/alerts/{id}/comments` | POST | Add comment |

## Kafka Topics

| Topic | Direction | Purpose |
|-------|-----------|---------|
| `scoring-events` | Consumer | Listens for critical/high risk events |
| `analysis-events` | Consumer | Listens for high-confidence anomalies |
| `alerting-events` | Producer | Publishes alert lifecycle events |

## Configuration

```yaml
pyrosense:
  alerting:
    escalation-check-interval: 300000  # 5 min
    sla:
      critical-deadline: 24h
      warning-deadline: 7d
      info-deadline: 30d
      escalation-interval: 4h
    kafka:
      scoring-topic: scoring-events
      analysis-topic: analysis-events
      output-topic: alerting-events
```

## Deduplication Logic

When a new alert is triggered for a device+type combination that already has an active (non-resolved) alert:
1. The existing alert's `occurrenceCount` is incremented
2. `lastOccurrenceAt` is updated
3. No new alert or event is created

This prevents alert storms from generating duplicate notifications.

## Security (Role-Based Access)

- **Occupant**: sees only their tenant's alerts
- **Manager**: sees all alerts for their property portfolio
- **Technician**: sees only alerts assigned to them
- **Admin**: full access

All actions are audited via domain events published to Kafka.

## Tests

- 23 domain model tests (Alert aggregate, state machine)
- 14 AlertStatus tests (transition rules)
- 3 EscalationLevel tests
- 3 SlaPolicy tests
- 3 DeduplicationKey tests
- 13 use case tests (CreateAlert, ManageAlert, EscalateAlerts)
- 8 persistence integration tests (H2)
- 10 ArchUnit tests (hexagonal enforcement)
- 1 context load test (embedded Kafka)

**Total: 78 tests**
