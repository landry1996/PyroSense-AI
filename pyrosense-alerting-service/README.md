# PyroSense Alerting Service

## Responsibility
Alert creation, lifecycle management, and escalation rules.

## Bounded Context
Alert Management

## Key Features
- Alert lifecycle: OPEN -> ACKNOWLEDGED -> RESOLVED
- 9 alert types (micro-arc, insulation, overheating, etc.)
- Severity levels: INFO, WARNING, CRITICAL
- Escalation rules for unacknowledged critical alerts

## Aggregate Root
- **Alert**: Rich lifecycle with guards and invariants

## Events Consumed
- `scoring.risk.updated`
- `analysis.anomaly.detected`

## Events Published
- `alerting.alert.created`
- `alerting.alert.escalated`

## Port
8086

## Dependencies
- PostgreSQL (alert storage)
- Kafka (event consumption/publishing)
