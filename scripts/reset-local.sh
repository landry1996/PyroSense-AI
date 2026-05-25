#!/usr/bin/env bash
# ============================================================
# PyroSense - Reset Local Development Environment
# ============================================================
# WARNING: This will destroy ALL local data (databases, volumes, etc.)
#
# Usage:
#   ./scripts/reset-local.sh          # Interactive confirmation
#   ./scripts/reset-local.sh --force  # Skip confirmation
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

ENV_FILE=".env.docker"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${RED}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║   ⚠  DESTRUCTIVE OPERATION                      ║${NC}"
echo -e "${RED}║   This will destroy ALL local data:              ║${NC}"
echo -e "${RED}║   - PostgreSQL databases                         ║${NC}"
echo -e "${RED}║   - Redis cache                                  ║${NC}"
echo -e "${RED}║   - Kafka topics and messages                    ║${NC}"
echo -e "${RED}║   - Grafana dashboards (custom)                  ║${NC}"
echo -e "${RED}║   - Prometheus metrics history                   ║${NC}"
echo -e "${RED}║   - Loki logs                                    ║${NC}"
echo -e "${RED}╚══════════════════════════════════════════════════╝${NC}"
echo ""

if [[ "${1:-}" != "--force" ]]; then
    read -p "Are you sure? Type 'yes' to confirm: " CONFIRM
    if [[ "$CONFIRM" != "yes" ]]; then
        echo -e "${BLUE}→ Cancelled${NC}"
        exit 0
    fi
fi

echo ""
echo -e "${BLUE}→ Stopping all containers...${NC}"
docker compose --env-file "$ENV_FILE" --profile full down --remove-orphans 2>/dev/null || true

echo -e "${BLUE}→ Removing named volumes...${NC}"
docker volume rm \
    pyrosense-pgdata \
    pyrosense-redis-data \
    pyrosense-kafka-data \
    pyrosense-mosquitto-data \
    pyrosense-mosquitto-log \
    pyrosense-prometheus-data \
    pyrosense-grafana-data \
    pyrosense-loki-data \
    2>/dev/null || true

echo -e "${BLUE}→ Removing dangling images...${NC}"
docker image prune -f --filter "label=com.pyrosense" 2>/dev/null || true

echo ""
echo -e "${GREEN}✓ Environment reset complete${NC}"
echo -e "${BLUE}  Run ./scripts/start-local.sh to start fresh${NC}"
