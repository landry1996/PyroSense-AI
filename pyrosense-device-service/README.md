# PyroSense Device Service

## Responsibility
Manages the lifecycle of electrical installations and their IoT sensors.

## Bounded Context
Device Management

## Key Features
- Installation registration and lifecycle (PROVISIONING -> ACTIVE -> DECOMMISSIONED)
- Sensor provisioning and status tracking
- Heartbeat monitoring
- Building type classification

## Aggregate Roots
- **Installation**: Manages sensors as child entities
- **Sensor**: Lifecycle states (PROVISIONING, LEARNING, ACTIVE, OFFLINE, DECOMMISSIONED)

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/installations | Register installation |
| GET | /api/v1/installations/{id} | Get installation |
| POST | /api/v1/installations/{id}/sensors | Provision sensor |
| GET | /api/v1/sensors/{id} | Get sensor details |

## Events Published
- `device.installation.created`
- `device.sensor.provisioned`

## Port
8082

## Dependencies
- PostgreSQL (dedicated schema)
- Kafka (event publishing)
