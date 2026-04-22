#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"

cleanup() {
  rm -rf "$OUT"
}
trap cleanup EXIT

"$REPO_ROOT/gradlew" :services:muxon-initializer:bootRun --args="\
--initial-config $REPO_ROOT/services/muxon-initializer/src/main/resources/initial-config.yaml \
--output-folder $OUT \
--muxon-passphrase $HOME/.secrets/passphrase \
--muxon-oidc-secret $HOME/.secrets/muxon-oidc-secret \
--muxon-db-password $HOME/.secrets/passphrase"

cp "$OUT/core-services-application.yaml"  "$REPO_ROOT/services/core-services/src/main/resources/application.yaml"
cp "$OUT/orchestrator-application.yaml"   "$REPO_ROOT/services/orchestrator/src/main/resources/application.yaml"
cp "$OUT/console-proxy-application.yaml"  "$REPO_ROOT/services/console-proxy/src/main/resources/application.yaml"

echo "Done. Generated configs copied into each service's resources folder."

