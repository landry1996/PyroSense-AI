#!/usr/bin/env bash
# ============================================================
# PyroSense - Stop Local Development Environment
# ============================================================
# Usage:
#   ./scripts/stop-local.sh           # Stop all services
#   ./scripts/stop-local.sh --remove  # Stop and remove containers
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

echo -e "${BLUE}→ Stopping PyroSense services...${NC}"

if [[ "${1:-}" == "--remove" ]]; then
    docker compose --env-file "$ENV_FILE" --profile full down --remove-orphans
    echo -e "${GREEN}✓ All containers stopped and removed${NC}"
else
    docker compose --env-file "$ENV_FILE" --profile full stop
    echo -e "${GREEN}✓ All containers stopped (data preserved)${NC}"
    echo -e "${BLUE}  Use --remove to also remove containers${NC}"
fi
