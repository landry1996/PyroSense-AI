#!/usr/bin/env bash
# ============================================================
# PyroSense AI Platform - Keycloak Realm Initialization Script
# ============================================================
# This script waits for Keycloak to be ready, imports the realm,
# and sets test user passwords from environment variables.
#
# Environment variables (with safe local-dev defaults):
#   KEYCLOAK_HOST          - Keycloak host (default: localhost)
#   KEYCLOAK_PORT          - Keycloak port (default: 8180)
#   KEYCLOAK_ADMIN         - Admin username (default: admin)
#   KEYCLOAK_ADMIN_PASSWORD- Admin password (default: admin_local_dev)
#   PILOT_ADMIN_PASSWORD   - Password for admin@pyrosense.io (default: changeme)
#   PILOT_MANAGER_PASSWORD - Password for manager@pyrosense.io (default: changeme)
#   PILOT_ELEC_PASSWORD    - Password for electricien@pyrosense.io (default: changeme)
#
# Usage:
#   ./init-keycloak.sh
# ============================================================

set -euo pipefail

# Configuration with safe defaults for local development ONLY
KEYCLOAK_HOST="${KEYCLOAK_HOST:-localhost}"
KEYCLOAK_PORT="${KEYCLOAK_PORT:-8180}"
KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin_local_dev}"

PILOT_ADMIN_PASSWORD="${PILOT_ADMIN_PASSWORD:-changeme}"
PILOT_MANAGER_PASSWORD="${PILOT_MANAGER_PASSWORD:-changeme}"
PILOT_ELEC_PASSWORD="${PILOT_ELEC_PASSWORD:-changeme}"

KEYCLOAK_URL="http://${KEYCLOAK_HOST}:${KEYCLOAK_PORT}"
KCADM="/opt/keycloak/bin/kcadm.sh"
REALM_FILE="/opt/keycloak/data/import/realm-export.json"

MAX_RETRIES=30
RETRY_INTERVAL=5

# ============================================================
# Functions
# ============================================================

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

wait_for_keycloak() {
  log "Waiting for Keycloak to be ready at ${KEYCLOAK_URL}..."
  local retries=0

  while [ $retries -lt $MAX_RETRIES ]; do
    if curl -sf "${KEYCLOAK_URL}/health/ready" > /dev/null 2>&1; then
      log "Keycloak is ready."
      return 0
    fi

    retries=$((retries + 1))
    log "Keycloak not ready yet (attempt ${retries}/${MAX_RETRIES}). Retrying in ${RETRY_INTERVAL}s..."
    sleep $RETRY_INTERVAL
  done

  log "ERROR: Keycloak did not become ready within $((MAX_RETRIES * RETRY_INTERVAL)) seconds."
  exit 1
}

authenticate_admin() {
  log "Authenticating as admin..."
  ${KCADM} config credentials \
    --server "${KEYCLOAK_URL}" \
    --realm master \
    --user "${KEYCLOAK_ADMIN}" \
    --password "${KEYCLOAK_ADMIN_PASSWORD}"
  log "Admin authentication successful."
}

import_realm() {
  # Check if realm already exists
  if ${KCADM} get realms/pyrosense > /dev/null 2>&1; then
    log "Realm 'pyrosense' already exists. Skipping import."
    return 0
  fi

  log "Importing realm from ${REALM_FILE}..."
  if [ ! -f "${REALM_FILE}" ]; then
    log "ERROR: Realm export file not found at ${REALM_FILE}"
    exit 1
  fi

  ${KCADM} create realms -f "${REALM_FILE}"
  log "Realm 'pyrosense' imported successfully."
}

set_user_password() {
  local username="$1"
  local password="$2"
  local temporary="${3:-false}"

  log "Setting password for user '${username}'..."

  local user_id
  user_id=$(${KCADM} get users -r pyrosense -q "username=${username}" --fields id --format csv --noquotes 2>/dev/null | head -1)

  if [ -z "${user_id}" ]; then
    log "WARNING: User '${username}' not found in realm. Skipping password reset."
    return 1
  fi

  ${KCADM} set-password -r pyrosense \
    --username "${username}" \
    --new-password "${password}" \
    --temporary="${temporary}"

  log "Password set for '${username}' (temporary=${temporary})."
}

# ============================================================
# Main
# ============================================================

main() {
  log "============================================================"
  log "PyroSense Keycloak Initialization"
  log "============================================================"

  wait_for_keycloak
  authenticate_admin
  import_realm

  log "Setting pilot user passwords from environment variables..."
  set_user_password "admin@pyrosense.io" "${PILOT_ADMIN_PASSWORD}" "false"
  set_user_password "manager@pyrosense.io" "${PILOT_MANAGER_PASSWORD}" "false"
  set_user_password "electricien@pyrosense.io" "${PILOT_ELEC_PASSWORD}" "false"

  log "============================================================"
  log "SUCCESS: Keycloak realm 'pyrosense' initialized."
  log ""
  log "  Realm URL: ${KEYCLOAK_URL}/realms/pyrosense"
  log "  Admin Console: ${KEYCLOAK_URL}/admin/master/console/"
  log ""
  log "  Test Users:"
  log "    - admin@pyrosense.io       (PLATFORM_ADMIN)"
  log "    - manager@pyrosense.io     (TENANT_ADMIN, PROPERTY_MANAGER)"
  log "    - electricien@pyrosense.io  (ELECTRICIAN)"
  log ""
  log "  Clients:"
  log "    - pyrosense-frontend (public, PKCE)"
  log "    - pyrosense-backend  (confidential, service account)"
  log "============================================================"
}

main "$@"
