#!/bin/sh
# Create secret files for Docker Compose and Kubernetes examples.
# Run from the infron-core directory:
#   ./scripts/create-secrets.sh
#
# Writes to ./secrets/ — add that directory to .gitignore and restrict permissions.

set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SECRETS_DIR="${ROOT_DIR}/secrets"
mkdir -p "$SECRETS_DIR"
umask 077

gen() {
  if [ ! -f "$1" ]; then
    openssl rand -base64 32 | tr -d '\n' > "$1"
    echo "Created $1"
  else
    echo "Skipped (exists): $1"
  fi
}

gen "${SECRETS_DIR}/infron-passphrase"
gen "${SECRETS_DIR}/infron-oidc-secret"
gen "${SECRETS_DIR}/infron-db-password"
gen "${SECRETS_DIR}/keycloak-admin-password"

chmod 600 "${SECRETS_DIR}"/* 2>/dev/null || true

echo ""
echo "Done. Keep ${SECRETS_DIR} private."
echo "Align infron-oidc-secret with your Keycloak client's secret (see realm import / admin console)."
