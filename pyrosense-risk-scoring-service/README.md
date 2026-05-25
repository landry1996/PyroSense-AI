# PyroSense Risk Scoring Service

## Responsibility
Calculates and maintains real-time risk scores per sensor and installation.

## Bounded Context
Risk Assessment

## Key Features
- Weighted multi-factor risk scoring (0-100)
- Trend analysis (IMPROVING, STABLE, DEGRADING, CRITICAL)
- Predicted incident window calculation
- Redis caching for frequently queried assessments

## Domain Model
- **RiskAssessment**: Computed score with factors and trend
- **RiskScore**: Value object [0-100] with severity mapping
- **RiskFactor**: Named weighted contribution

## Events Consumed
- `analysis.anomaly.detected`

## Events Published
- `scoring.risk.updated`

## Port
8085

## Dependencies
- PostgreSQL (assessment history)
- Redis (score cache, TTL 5min)
- Kafka (event consumption/publishing)
