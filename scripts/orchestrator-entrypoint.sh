#!/bin/sh
set -e
if [ "${MUXON_SKIP_BOOTSTRAP:-0}" != "1" ]; then
  java -jar bootstrap.jar --initial-config /etc/muxon/initial-config.yaml --output-folder /etc/muxon --muxon-passphrase /run/secrets/muxon-passphrase --muxon-oidc-secret /run/secrets/muxon-oidc-secret --muxon-db-password /run/secrets/muxon-db-password
fi
exec java -jar app.jar --spring.config.additional-location=optional:file:/etc/muxon/orchestrator-application.yaml

