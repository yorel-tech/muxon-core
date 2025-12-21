#!/bin/sh
set -e
java -jar bootstrap.jar --initial-config /etc/infron/initial-config.yaml --output-folder /etc/infron --infron-passphrase /run/secrets/infron-passphrase --infron-oidc-secret /run/secrets/infron-oidc-secret --infron-db-password /run/secrets/infron-db-password
exec java -jar app.jar
