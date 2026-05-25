#!/usr/bin/env bash
# ============================================================
# PyroSense - Start Local Development Environment
# ============================================================
# Usage:
#   ./scripts/start-local.sh              # Infrastructure only
#   ./scripts/start-local.sh services     # Infrastructure + all services
#   ./scripts/start-local.sh full         # Everything (services + simulator)
#   ./scripts/start-local.sh simulator    # Infrastructure + simulator
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

PROFILE="${1:-}"
ENV_FILE=".env.docker"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   PyroSense AI Platform - Local Development     ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
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

# Build services if profile requires it
if [[ "$PROFILE" == "services" || "$PROFILE" == "full" ]]; then
    echo -e "${BLUE}→ Building application services (this may take a few minutes)...${NC}"
    docker compose --env-file "$ENV_FILE" --profile "$PROFILE" build
    echo -e "${GREEN}✓ Build complete${NC}"
    echo ""
fi

# Start services
if [ -z "$PROFILE" ]; then
    echo -e "${BLUE}→ Starting infrastructure services...${NC}"
    docker compose --env-file "$ENV_FILE" up -d
else
    echo -e "${BLUE}→ Starting with profile: ${PROFILE}...${NC}"
    docker compose --env-file "$ENV_FILE" --profile "$PROFILE" up -d
fi

echo ""
echo -e "${GREEN}✓ Services started successfully${NC}"
echo ""

# Wait for health checks
echo -e "${BLUE}→ Waiting for services to be healthy...${NC}"
sleep 5

# Show status
docker compose --env-file "$ENV_FILE" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || docker compose --env-file "$ENV_FILE" ps

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Services Available:                            ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║   PostgreSQL:    localhost:5432                  ║${NC}"
echo -e "${GREEN}║   Redis:         localhost:6379                  ║${NC}"
echo -e "${GREEN}║   Kafka:         localhost:29092                 ║${NC}"
echo -e "${GREEN}║   MQTT:          localhost:1883                  ║${NC}"
echo -e "${GREEN}║   Keycloak:      http://localhost:8180           ║${NC}"
echo -e "${GREEN}║   Prometheus:    http://localhost:9090           ║${NC}"
echo -e "${GREEN}║   Grafana:       http://localhost:3000           ║${NC}"
echo -e "${GREEN}║   Loki:          http://localhost:3100           ║${NC}"
echo -e "${GREEN}║   OTEL:          localhost:4317 (gRPC)           ║${NC}"
echo -e "${GREEN}║                  localhost:4318 (HTTP)           ║${NC}"

if [[ "$PROFILE" == "services" || "$PROFILE" == "full" ]]; then
echo -e "${GREEN}║                                                  ║${NC}"
echo -e "${GREEN}║   API Gateway:   http://localhost:8080           ║${NC}"
echo -e "${GREEN}║   Identity:      http://localhost:8081           ║${NC}"
echo -e "${GREEN}║   Device:        http://localhost:8082           ║${NC}"
echo -e "${GREEN}║   Ingestion:     http://localhost:8083           ║${NC}"
echo -e "${GREEN}║   Analysis:      http://localhost:8084           ║${NC}"
echo -e "${GREEN}║   Scoring:       http://localhost:8085           ║${NC}"
echo -e "${GREEN}║   Alerting:      http://localhost:8086           ║${NC}"
echo -e "${GREEN}║   Notification:  http://localhost:8087           ║${NC}"
echo -e "${GREEN}║   Maintenance:   http://localhost:8089           ║${NC}"
echo -e "${GREEN}║   Reporting:     http://localhost:8091           ║${NC}"
fi

echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Logs:  docker compose --env-file .env.docker logs -f [service]${NC}"
echo -e "${BLUE}Stop:  ./scripts/stop-local.sh${NC}"
echo -e "${BLUE}Reset: ./scripts/reset-local.sh${NC}"
