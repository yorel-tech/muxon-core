#!/usr/bin/env bash
set -euo pipefail

# kcadm helper script to create roles, users, clients and audience mappers for infron-dev
# Usage:
#   KC_URL=http://localhost:8080/auth KC_ADMIN_USER=admin KC_ADMIN_PASS=admin ./kcadm-create-infron.sh
#
# Requirements:
#  - kcadm.sh available (set KC_BIN to Keycloak bin directory if not on PATH)
#  - jq installed

# --------------------------
# Config - edit if needed
# --------------------------
: "${KC_BIN:=./}"                     # path where kcadm.sh lives (default: current directory)
: "${KC_URL:=http://localhost:8080/auth}"
: "${KC_ADMIN_USER:=admin}"
: "${KC_ADMIN_PASS:=admin}"
: "${REALM:=infron-dev}"

# Users to create: username:password:role
USERS=(
  "admin@infron.dev:Admin123!:admin"
  "dev@infron.dev:Dev123!:member"
  "tenantadmin@infron.dev:Tenant123!:tenantadmin"
  "projectadmin@infron.dev:Project123!:projectadmin"
  "workload-operator@infron.dev:Operator123!:workload-operator"
  "workload-user@infron.dev:User123!:workload-user"
)

# Roles to ensure exist
ROLES=(admin member tenantadmin projectadmin workload-operator workload-user)

# Clients to create (clientId|publicClient|serviceAccountsEnabled|redirectUris|webOrigins)
# redirectUris and webOrigins should be JSON arrays
CLIENTS=(
  'infron-web|true|false|["http://localhost:4000/*"]|["http://localhost:4000"]'
  'infron-api|false|true|[]|[]'
  'infron-test|true|false|["http://localhost:8081/*"]|["http://localhost:8081"]'
)

# Audience target (the API client id/name to be added to aud)
AUD_TARGET="infron-api"

# --------------------------
# Helpers
# --------------------------
kcadm() {
  # wrapper for kcadm.sh
  # example: kcadm config credentials ... or kcadm get clients -r realm
  "${KC_BIN%/}/kcadm.sh" "$@"
}

require_jq() {
  if ! command -v jq >/dev/null 2>&1; then
    echo "ERROR: jq is required but not installed. Install jq and re-run." >&2
    exit 2
  fi
}

# --------------------------
# Start
# --------------------------
echo "Using Keycloak at: ${KC_URL}, realm: ${REALM}"
require_jq

echo "Authenticate to Keycloak using kcadm..."
kcadm config credentials --server "${KC_URL}" --realm master --user "${KC_ADMIN_USER}" --password "${KC_ADMIN_PASS}"
echo "Authenticated."

# Ensure realm exists (if you are importing realm JSON instead, skip)
if ! kcadm get realms/"${REALM}" >/dev/null 2>&1; then
  echo "Realm ${REALM} does not exist. Creating..."
  kcadm create realms -s realm="${REALM}" -s enabled=true
  echo "Created realm ${REALM}."
else
  echo "Realm ${REALM} already exists."
fi

# Create roles if not exist
for role in "${ROLES[@]}"; do
  if kcadm get roles -r "${REALM}" --fields name | jq -e --arg r "$role" 'map(select(.name == $r)) | length > 0' >/dev/null 2>&1; then
    echo "Role '${role}' already exists."
  else
    echo "Creating role '${role}'..."
    kcadm create roles -r "${REALM}" -s name="${role}"
  fi
done

# Create users and assign realm roles
for u in "${USERS[@]}"; do
  IFS=':' read -r username password role <<<"$u"
  echo "Processing user ${username} (role: ${role})..."
  # check if user exists
  existing_json="$(kcadm get users -r "${REALM}" -q username="${username}" || true)"
  user_id=$(echo "$existing_json" | jq -r '.[0].id // empty')

  if [ -n "$user_id" ]; then
    echo "User ${username} exists (id: ${user_id})."
  else
    echo "Creating user ${username}..."
    kcadm create users -r "${REALM}" -s username="${username}" -s enabled=true
    existing_json="$(kcadm get users -r "${REALM}" -q username="${username}")"
    user_id=$(echo "$existing_json" | jq -r '.[0].id')
    echo "Created user id=${user_id}."
  fi

  # set password (non-temporary)
  echo "Setting password for ${username}..."
  # The set-password command takes the user id
  kcadm set-password -r "${REALM}" --uid "${user_id}" --new-password "${password}"

  # assign realm role
  # fetch role representation
  role_json="$(kcadm get roles/"${role}" -r "${REALM}")"
  if [ -z "$role_json" ]; then
    echo "ERROR: role ${role} not found (unexpected)." >&2
    exit 1
  fi

  # add realm role to user (idempotent-ish)
  echo "Assigning realm role ${role} to user ${username}..."
  kcadm add-roles -r "${REALM}" --uid "${user_id}" --rolename "${role}" || true
done

# Create clients if missing and add audience mappers for infron-web and infron-test
for c in "${CLIENTS[@]}"; do
  IFS='|' read -r clientId publicClient serviceAccountsEnabled redirectUris webOrigins <<<"$c"
  echo "Processing client ${clientId}..."

  # check if client exists
  existing_clients_json="$(kcadm get clients -r "${REALM}" -q clientId="${clientId}" || true)"
  client_uuid=$(echo "$existing_clients_json" | jq -r '.[0].id // empty')

  if [ -n "$client_uuid" ]; then
    echo "Client ${clientId} already exists (id: ${client_uuid})."
  else
    echo "Creating client ${clientId}..."
    # create client with relevant properties
    # use -s for scalar props; pass redirectUris and webOrigins as JSON strings
    kcadm create clients -r "${REALM}" \
      -s clientId="${clientId}" \
      -s publicClient="${publicClient}" \
      -s serviceAccountsEnabled="${serviceAccountsEnabled}" \
      -s protocol="openid-connect" \
      -s 'redirectUris='"${redirectUris}" \
      -s 'webOrigins='"${webOrigins}"
    existing_clients_json="$(kcadm get clients -r "${REALM}" -q clientId="${clientId}")"
    client_uuid=$(echo "$existing_clients_json" | jq -r '.[0].id')
    echo "Created client ${clientId} id=${client_uuid}."
  fi

  # If client is infron-web or infron-test, add audience mapper to include AUD_TARGET in access tokens
  if [ "${clientId}" = "infron-web" ] || [ "${clientId}" = "infron-test" ]; then
    echo "Ensuring audience mapper exists on client ${clientId} (aud -> ${AUD_TARGET})..."

    # check if a mapper already exists that includes the aud
    mapper_exists=$(kcadm get clients/"${client_uuid}"/protocol-mappers/models -r "${REALM}" | \
      jq -e --arg aud "${AUD_TARGET}" '.[] | select(.protocolMapper == "oidc-audience-mapper" and .config["included.client.audience"] == $aud) | .name' >/dev/null 2>&1 || true)

    if kcadm get clients/"${client_uuid}"/protocol-mappers/models -r "${REALM}" | jq -e --arg aud "${AUD_TARGET}" '.[] | select(.protocolMapper == "oidc-audience-mapper" and (.config["included.client.audience"] == $aud))' >/dev/null 2>&1; then
      echo "Audience mapper for '${AUD_TARGET}' already present on client ${clientId}."
    else
      echo "Creating audience mapper on client ${clientId}..."
      # create a temporary mapper json file
      mapper_json="$(mktemp)"
      cat >"${mapper_json}" <<EOF
{
  "name": "audience-${AUD_TARGET}",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-audience-mapper",
  "consentRequired": false,
  "config": {
    "included.client.audience": "${AUD_TARGET}",
    "id.token.claim": "false",
    "access.token.claim": "true",
    "claim.name": "aud",
    "jsonType.label": "String"
  }
}
EOF
      kcadm create clients/"${client_uuid}"/protocol-mappers/models -r "${REALM}" -f "${mapper_json}"
      rm -f "${mapper_json}"
      echo "Audience mapper created."
    fi
  fi
done

echo "All done. You can verify by logging into Keycloak Admin console or fetching tokens for clients and inspecting the 'aud' claim."
