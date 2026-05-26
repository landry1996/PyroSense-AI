#!/usr/bin/env bash
# ============================================================
# PyroSense - Stop MVP 2 Local Development Environment
# ============================================================
# Usage:
#   ./scripts/stop-mvp2-local.sh           # Stop all MVP2 services
#   ./scripts/stop-mvp2-local.sh --remove  # Stop and remove containers
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

ENV_FILE=".env.docker"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}→ Stopping PyroSense MVP 2 services...${NC}"

if [[ "${1:-}" == "--remove" ]]; then
    docker compose --env-file "$ENV_FILE" --profile mvp2 down --remove-orphans
    echo -e "${GREEN}✓ All MVP 2 containers stopped and removed${NC}"
else
    docker compose --env-file "$ENV_FILE" --profile mvp2 stop
    echo -e "${GREEN}✓ All MVP 2 containers stopped (data preserved)${NC}"
    echo -e "${BLUE}  Use --remove to also remove containers${NC}"
fi
