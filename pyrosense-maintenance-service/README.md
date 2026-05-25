# PyroSense Maintenance Service

## Responsibility
Predictive maintenance scheduling and intervention tracking.

## Bounded Context
Maintenance & Interventions

## Key Features
- Intervention scheduling (preventive, corrective, predictive, emergency)
- Technician assignment
- Status tracking (PLANNED -> ASSIGNED -> IN_PROGRESS -> COMPLETED)
- History per installation

## Events Consumed
- `scoring.risk.updated`
- `alerting.alert.created`

## Port
8089

## Dependencies
- PostgreSQL (intervention records)
- Kafka (event consumption)
