# PyroSense Signal Analysis Service

## Responsibility
Baseline learning, statistical anomaly detection, and signal quality analysis.

## Bounded Context
Signal Analysis

## Key Features
- Statistical baseline learning (min 1000 readings per sensor)
- Z-score anomaly detection (sigma threshold: 3.0)
- Multi-metric analysis (voltage, current, THD, temperature, micro-arcs)
- Real-time processing via Kafka consumers

## Domain Services
- **BaselineLearner**: Builds statistical profile per sensor
- **AnomalyDetector**: Z-score based deviation detection

## Events Consumed
- `ingestion.signal.ingested`

## Events Published
- `analysis.anomaly.detected`

## Port
8084

## Dependencies
- PostgreSQL (baseline storage)
- Redis (profile cache)
- Kafka (event consumption/publishing)
