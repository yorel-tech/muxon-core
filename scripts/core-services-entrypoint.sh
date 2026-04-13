#!/bin/sh
set -e
if [ "${INFRON_SKIP_BOOTSTRAP:-0}" != "1" ]; then
  java -jar bootstrap.jar --initial-config /etc/infron/initial-config.yaml --output-folder /etc/infron --infron-passphrase /run/secrets/infron-passphrase --infron-oidc-secret /run/secrets/infron-oidc-secret --infron-db-password /run/secrets/infron-db-password
fi
exec java -jar app.jar --spring.config.additional-location=optional:file:/etc/infron/
