# Local Development Environment

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Docker Desktop | 4.x+ | Container runtime |
| Docker Compose | v2+ | Service orchestration |
| Java | 21+ | Service development |
| Maven | 3.9+ | Build system |

Ensure Docker Desktop is running and has at least **8 GB RAM** allocated (Settings > Resources).

## Quick Start

```bash
# Start infrastructure only (DB, Kafka, Redis, MQTT, observability)
./scripts/start-local.sh

# Start everything including all microservices
./scripts/start-local.sh services

# Start everything + IoT simulator
./scripts/start-local.sh full

# Stop
./scripts/stop-local.sh

# Full reset (destroys all data)
./scripts/reset-local.sh
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    pyrosense-network                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  INFRASTRUCTURE (always running)                         │
│  ┌──────────┐ ┌───────┐ ┌───────┐ ┌──────────┐        │
│  │PostgreSQL│ │ Redis │ │ Kafka │ │Mosquitto │        │
│  │  :5432   │ │ :6379 │ │:29092 │ │  :1883   │        │
│  └──────────┘ └───────┘ └───────┘ └──────────┘        │
│  ┌──────────┐                                           │
│  │ Keycloak │                                           │
│  │  :8180   │                                           │
│  └──────────┘                                           │
│                                                          │
│  OBSERVABILITY (always running)                          │
│  ┌──────────┐ ┌───────┐ ┌──────┐ ┌────────────┐       │
│  │Prometheus│ │Grafana│ │ Loki │ │OTEL Collect│       │
│  │  :9090   │ │ :3000 │ │:3100 │ │:4317/:4318 │       │
│  └──────────┘ └───────┘ └──────┘ └────────────┘       │
│                                                          │
│  SERVICES (profile: services)                            │
│  ┌──────────┐ ┌────────┐ ┌──────────┐ ┌─────────┐     │
│  │ Gateway  │ │Identity│ │  Device  │ │Ingestion│     │
│  │  :8080   │ │ :8081  │ │  :8082   │ │  :8083  │     │
│  └──────────┘ └────────┘ └──────────┘ └─────────┘     │
│  ┌──────────┐ ┌────────┐ ┌──────────┐ ┌────────────┐  │
│  │ Analysis │ │Scoring │ │Alerting  │ │Notification│  │
│  │  :8084   │ │ :8085  │ │  :8086   │ │   :8087    │  │
│  └──────────┘ └────────┘ └──────────┘ └────────────┘  │
│  ┌──────────┐ ┌──────────┐                              │
│  │Reporting │ │Maintenance│                              │
│  │  :8091   │ │   :8089   │                              │
│  └──────────┘ └──────────┘                              │
│                                                          │
│  SIMULATOR (profile: simulator)                          │
│  ┌─────────────┐                                        │
│  │IoT Simulator│                                        │
│  └─────────────┘                                        │
└─────────────────────────────────────────────────────────┘
```

## Port Map

| Service | Port | Protocol | Notes |
|---------|------|----------|-------|
| API Gateway | 8080 | HTTP | Entry point |
| Identity | 8081 | HTTP | |
| Device | 8082 | HTTP | |
| Ingestion | 8083 | HTTP | |
| Signal Analysis | 8084 | HTTP | |
| Risk Scoring | 8085 | HTTP | |
| Alerting | 8086 | HTTP | |
| Notification | 8087 | HTTP | |
| Maintenance | 8089 | HTTP | |
| Reporting | 8091 | HTTP | |
| PostgreSQL | 5432 | TCP | TimescaleDB |
| Redis | 6379 | TCP | |
| Kafka | 29092 | TCP | External listener |
| MQTT | 1883 | TCP | |
| MQTT WebSocket | 9001 | WS | |
| Keycloak | 8180 | HTTP | Admin console |
| Prometheus | 9090 | HTTP | |
| Grafana | 3000 | HTTP | admin / admin_local_dev |
| Loki | 3100 | HTTP | |
| OTEL (gRPC) | 4317 | gRPC | |
| OTEL (HTTP) | 4318 | HTTP | |

### Debug Ports (via docker-compose.override.yml)

| Service | Debug Port | JDWP |
|---------|-----------|------|
| Gateway | 5010 | Remote attach |
| Identity | 5011 | Remote attach |
| Device | 5012 | Remote attach |
| Ingestion | 5013 | Remote attach |
| Analysis | 5014 | Remote attach |
| Scoring | 5015 | Remote attach |
| Alerting | 5016 | Remote attach |
| Notification | 5017 | Remote attach |
| Reporting | 5018 | Remote attach |
| Maintenance | 5019 | Remote attach |

## Profiles

Docker Compose profiles control which services start:

| Profile | What starts | Use case |
|---------|-------------|----------|
| *(none)* | Infrastructure + observability | IDE development (run services locally) |
| `services` | All Spring Boot microservices | Full stack in Docker |
| `simulator` | IoT simulator | Generate test telemetry |
| `full` | Everything | Demo / integration testing |

```bash
# Infrastructure only (default)
docker compose --env-file .env.docker up -d

# Add services
docker compose --env-file .env.docker --profile services up -d

# Just the simulator
docker compose --env-file .env.docker --profile simulator up -d
```

## Development Workflows

### Workflow 1: IDE Development (Recommended)

Run infrastructure in Docker, services from your IDE:

```bash
./scripts/start-local.sh   # Infrastructure only

# Run any service from IDE with profile: local
# IntelliJ: Run Configuration → Active Profiles: local
# CLI: mvn spring-boot:run -pl pyrosense-device-service -Dspring-boot.run.profiles=local
```

Services connect to `localhost:5432`, `localhost:9092`, etc. (defaults in application.yml).

### Workflow 2: Full Docker Stack

Run everything in Docker:

```bash
./scripts/start-local.sh services
```

### Workflow 3: Mixed (IDE + Docker services)

Run infrastructure + most services in Docker, debug one from IDE:

```bash
# Start everything
./scripts/start-local.sh services

# Stop the service you want to debug
docker compose --env-file .env.docker stop device-service

# Run it from your IDE with profile: local
# It connects to Docker infrastructure via localhost ports
```

### Workflow 4: Remote Debugging

With `docker-compose.override.yml` active, all services expose JDWP ports:

1. Start: `./scripts/start-local.sh services`
2. IntelliJ: Run > Edit Configurations > Remote JVM Debug
3. Host: `localhost`, Port: see debug port table above

## Building Docker Images

```bash
# Build all services
docker compose --env-file .env.docker --profile services build

# Build a specific service
docker compose --env-file .env.docker build device-service

# Build with Maven docker profile (Spring Boot Buildpacks)
mvn package -Pdocker -DskipTests
```

## Environment Configuration

All secrets and configuration are in `.env.docker` (gitignored pattern matches `.env.*`).

The `.env.docker` file ships with safe local defaults. To customize:

```bash
# Copy and edit
cp .env.docker .env.docker.local
# Then use: docker compose --env-file .env.docker.local up -d
```

## Observability Access

### Grafana

- URL: http://localhost:3000
- Login: `admin` / `admin_local_dev`
- Pre-provisioned dashboards:
  - **Platform Overview**: Service health, telemetry rates, alerts
  - **Ingestion Pipeline**: Throughput, latency, rejections
  - **Alerting & Notifications**: Alert lifecycle, notification channels

### Prometheus

- URL: http://localhost:9090
- Targets: http://localhost:9090/targets (check scrape status)
- Alerts: http://localhost:9090/alerts

### Logs (Loki via Grafana)

1. Open Grafana > Explore
2. Select "Loki" datasource
3. Query: `{service="pyrosense-ingestion-service"}`

## Troubleshooting

### Services won't start

```bash
# Check logs
docker compose --env-file .env.docker logs -f identity-service

# Check health
docker compose --env-file .env.docker ps

# Rebuild after code changes
docker compose --env-file .env.docker --profile services build --no-cache identity-service
docker compose --env-file .env.docker --profile services up -d identity-service
```

### Database connection refused

PostgreSQL needs time to initialize all 9 databases. Wait for the healthcheck:

```bash
docker compose --env-file .env.docker logs -f postgres
# Wait for "Multiple databases created" and "database system is ready"
```

### Kafka not ready

Kafka KRaft takes ~30s to elect a controller and become ready:

```bash
docker compose --env-file .env.docker logs -f kafka | grep "started"
```

### Port conflict

If a port is already in use:

```bash
# Find what's using port 5432
netstat -ano | findstr :5432     # Windows
lsof -i :5432                    # macOS/Linux

# Change the port in .env.docker or stop the conflicting service
```

### Out of memory

If Docker runs out of memory with all services:

1. Increase Docker Desktop memory (Settings > Resources > 10GB+)
2. Or reduce per-service heap: set `JAVA_OPTS=-Xms128m -Xmx256m` in `.env.docker`
3. Or run fewer services (use profile-less mode + IDE)

### Reset everything

```bash
./scripts/reset-local.sh --force
./scripts/start-local.sh
```

## Resource Requirements

| Mode | RAM | CPU | Disk |
|------|-----|-----|------|
| Infrastructure only | ~4 GB | 2 cores | 2 GB |
| Infrastructure + Services | ~10 GB | 4 cores | 5 GB |
| Full (+ simulator) | ~12 GB | 4 cores | 5 GB |
