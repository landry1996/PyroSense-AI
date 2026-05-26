#!/usr/bin/env bash
# ============================================================
# PyroSense - Start MVP 2 Local Development Environment
# ============================================================
# Starts only the services needed for MVP 2:
#   Infrastructure: PostgreSQL, Redis, Kafka, Keycloak, Prometheus, Grafana
#   Backend: API Gateway, Dashboard, Maintenance, Reporting, Notification, Alerting
#   Frontend: Angular Dashboard (nginx)
#
# Usage:
#   ./scripts/start-mvp2-local.sh              # Build + start all MVP2
#   ./scripts/start-mvp2-local.sh --no-build   # Start without rebuild
#   ./scripts/start-mvp2-local.sh --infra-only # Infrastructure only
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

ENV_FILE=".env.docker"
FLAG="${1:-}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   PyroSense MVP 2 - Local Development            ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║   Services: Gateway, Dashboard, Maintenance,     ║${NC}"
echo -e "${CYAN}║             Reporting, Notification, Alerting     ║${NC}"
echo -e "${CYAN}║   Frontend: Angular Dashboard (port 4200)        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}⚠  WARNING: Local development only. Not for production.${NC}"
echo ""

# Check prerequisites
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker is not installed or not in PATH${NC}"
    exit 1
fi

if ! docker info &> /dev/null 2>&1; then
    echo -e "${RED}✗ Docker daemon is not running${NC}"
    exit 1
fi

# Check env file
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}→ .env.docker not found, using defaults${NC}"
fi

if [[ "$FLAG" == "--infra-only" ]]; then
    echo -e "${BLUE}→ Starting infrastructure only (PostgreSQL, Redis, Kafka, Keycloak, Prometheus, Grafana)...${NC}"
    docker compose --env-file "$ENV_FILE" up -d
    echo ""
    echo -e "${GREEN}✓ Infrastructure started${NC}"
    echo ""
    echo -e "${GREEN}  PostgreSQL:  localhost:5432${NC}"
    echo -e "${GREEN}  Redis:       localhost:6379${NC}"
    echo -e "${GREEN}  Kafka:       localhost:29092${NC}"
    echo -e "${GREEN}  Keycloak:    http://localhost:8180${NC}"
    echo -e "${GREEN}  Prometheus:  http://localhost:9090${NC}"
    echo -e "${GREEN}  Grafana:     http://localhost:3000${NC}"
    echo ""
    echo -e "${BLUE}Run services in your IDE, then access via localhost ports.${NC}"
    exit 0
fi

# Build if not --no-build
if [[ "$FLAG" != "--no-build" ]]; then
    echo -e "${BLUE}→ Building MVP 2 services (this may take a few minutes)...${NC}"
    docker compose --env-file "$ENV_FILE" --profile mvp2 build
    echo -e "${GREEN}✓ Build complete${NC}"
    echo ""
fi

# Start MVP2 profile
echo -e "${BLUE}→ Starting MVP 2 services...${NC}"
docker compose --env-file "$ENV_FILE" --profile mvp2 up -d

echo ""
echo -e "${GREEN}✓ MVP 2 services started${NC}"
echo ""

# Wait for health checks
echo -e "${BLUE}→ Waiting for services to be healthy...${NC}"
sleep 8

# Show status
docker compose --env-file "$ENV_FILE" --profile mvp2 ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || docker compose --env-file "$ENV_FILE" --profile mvp2 ps

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   MVP 2 Services Available:                      ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║   Frontend:      http://localhost:4200           ║${NC}"
echo -e "${GREEN}║   API Gateway:   http://localhost:8080           ║${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║   Dashboard API: http://localhost:8088           ║${NC}"
echo -e "${GREEN}║   Alerting:      http://localhost:8086           ║${NC}"
echo -e "${GREEN}║   Maintenance:   http://localhost:8089           ║${NC}"
echo -e "${GREEN}║   Reporting:     http://localhost:8091           ║${NC}"
echo -e "${GREEN}║   Notification:  http://localhost:8087           ║${NC}"
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║   PostgreSQL:    localhost:5432                  ║${NC}"
echo -e "${GREEN}║   Redis:         localhost:6379                  ║${NC}"
echo -e "${GREEN}║   Kafka:         localhost:29092                 ║${NC}"
echo -e "${GREEN}║   Keycloak:      http://localhost:8180           ║${NC}"
echo -e "${GREEN}║   Prometheus:    http://localhost:9090           ║${NC}"
echo -e "${GREEN}║   Grafana:       http://localhost:3000           ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Logs:  docker compose --env-file .env.docker --profile mvp2 logs -f [service]${NC}"
echo -e "${BLUE}Stop:  ./scripts/stop-mvp2-local.sh${NC}"
echo -e "${BLUE}Reset: ./scripts/reset-mvp2-local.sh${NC}"
