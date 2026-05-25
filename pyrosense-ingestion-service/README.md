# PyroSense Ingestion Service

## Responsibility
Receives IoT electrical signals from sensors via MQTT and persists them in TimescaleDB.

## Bounded Context
Signal Ingestion

## Key Features
- MQTT 5 subscription (topic: pyrosense/+/signals)
- Signal validation and persistence
- Event publishing to Kafka for downstream processing
- TimescaleDB hypertable storage for time-series data

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/signals/{sensorId} | Query signals by sensor |
| GET | /api/v1/signals/{sensorId}/latest | Get latest signal |

## Events Published
- `ingestion.signal.ingested`

## Port
8083

## Dependencies
- PostgreSQL + TimescaleDB
- Mosquitto MQTT Broker
- Kafka (event publishing)
