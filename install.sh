#!/usr/bin/env bash
# Muxon Core Docker Compose installer (kit root entrypoint).
# Phases match INSTALL.md: prerequisites → secrets → render → compose → verify.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="compose/muxon.yml"
TEMPLATE_CONFIG="compose/initial-config.yaml.template"
REALM_TEMPLATE="compose/keycloak/realm-muxon-dev.json"
REALM_RUNTIME="compose/keycloak/realm-muxon-dev.runtime.json"
OUT_CONFIG="initial-config.yaml"
SECRETS_DIR="secrets"
VERSION_FILE="VERSION"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
HEALTH_TIMEOUT_SEC="${HEALTH_TIMEOUT_SEC:-180}"

PROFILE="internal"
NON_INTERACTIVE=0
REGENERATE_SECRETS=0
RENDER_ONLY=0
SKIP_HEALTH=0

MUXON_VERSION=""
INSTANCE_NAME=""
INSTANCE_ID=""
SYSTEM_ADMIN_USERNAME=""
TENANT_ADMIN_USERNAME=""
ISSUER_URI=""
CLIENT_ID=""
DATASOURCE_URL=""
DATASOURCE_USERNAME=""

usage() {
  cat <<'EOF'
Usage: ./install.sh [options]

  --profile internal|external   Bundled Postgres+Keycloak (default) or external only
  --version TAG                 Image tag (default: contents of VERSION)
  --admin-user EMAIL            System admin username (IdP user)
  --tenant-admin EMAIL          Tenant admin (default: same as --admin-user)
  --instance-name NAME          Default: docker
  --instance-id ID              Default: 1
  --issuer-uri URI              Required for external; internal default Keycloak URI
  --client-id ID                Default: muxon-api
  --datasource-url JDBC         Required for external; internal default postgres URL
  --datasource-user USER        Default: muxon
  --non-interactive             Fail if required inputs missing (no prompts)
  --regenerate-secrets          Overwrite existing secrets/*
  --render-only                 Secrets + templates only; do not start Compose
  --skip-health                 Do not wait for actuator health
  -h, --help                    Show this help
EOF
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

info() {
  echo "==> $*"
}

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

json_escape() {
  # Escape for embedding a string in JSON double quotes.
  printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/'"$(printf '\t')"'/\\t/g'
}

replace_all() {
  # Literal (non-regex) replace of all occurrences in a file.
  local file="$1" search="$2" replacement="$3"
  local tmp
  tmp="$(mktemp)"
  SEARCH="$search" REPL="$replacement" awk '
    BEGIN { s = ENVIRON["SEARCH"]; r = ENVIRON["REPL"] }
    {
      out = $0
      while ((i = index(out, s)) > 0) {
        out = substr(out, 1, i - 1) r substr(out, i + length(s))
      }
      print out
    }
  ' "$file" >"$tmp"
  mv "$tmp" "$file"
}

require_no_placeholder() {
  local file="$1"
  # Ignore comment lines; only fail on unresolved tokens in active content.
  if grep -vE '^\s*#' "$file" | grep -E '__[A-Z0-9_]+__|\$\{MUXON_API_CLIENT_SECRET\}' >/dev/null 2>&1; then
    die "Unresolved placeholder(s) remain in $file"
  fi
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --profile)
        PROFILE="${2:-}"; shift 2 || die "missing value for --profile"
        ;;
      --version)
        MUXON_VERSION="${2:-}"; shift 2 || die "missing value for --version"
        ;;
      --admin-user)
        SYSTEM_ADMIN_USERNAME="${2:-}"; shift 2 || die "missing value for --admin-user"
        ;;
      --tenant-admin)
        TENANT_ADMIN_USERNAME="${2:-}"; shift 2 || die "missing value for --tenant-admin"
        ;;
      --instance-name)
        INSTANCE_NAME="${2:-}"; shift 2 || die "missing value for --instance-name"
        ;;
      --instance-id)
        INSTANCE_ID="${2:-}"; shift 2 || die "missing value for --instance-id"
        ;;
      --issuer-uri)
        ISSUER_URI="${2:-}"; shift 2 || die "missing value for --issuer-uri"
        ;;
      --client-id)
        CLIENT_ID="${2:-}"; shift 2 || die "missing value for --client-id"
        ;;
      --datasource-url)
        DATASOURCE_URL="${2:-}"; shift 2 || die "missing value for --datasource-url"
        ;;
      --datasource-user)
        DATASOURCE_USERNAME="${2:-}"; shift 2 || die "missing value for --datasource-user"
        ;;
      --non-interactive)
        NON_INTERACTIVE=1; shift
        ;;
      --regenerate-secrets)
        REGENERATE_SECRETS=1; shift
        ;;
      --render-only)
        RENDER_ONLY=1; shift
        ;;
      --skip-health)
        SKIP_HEALTH=1; shift
        ;;
      -h|--help)
        usage; exit 0
        ;;
      *)
        die "unknown argument: $1 (try --help)"
        ;;
    esac
  done
}

prompt_if_empty() {
  local var_name="$1" prompt="$2" default="${3:-}"
  local current="${!var_name:-}"
  if [[ -n "$current" ]]; then
    return 0
  fi
  if [[ "$NON_INTERACTIVE" -eq 1 ]]; then
    if [[ -n "$default" ]]; then
      printf -v "$var_name" '%s' "$default"
      return 0
    fi
    die "missing required input: $var_name (use flags or unset --non-interactive)"
  fi
  local reply
  if [[ -n "$default" ]]; then
    read -r -p "$prompt [$default]: " reply || true
    printf -v "$var_name" '%s' "${reply:-$default}"
  else
    read -r -p "$prompt: " reply || true
    [[ -n "$reply" ]] || die "missing required input: $var_name"
    printf -v "$var_name" '%s' "$reply"
  fi
}

phase_prerequisites() {
  info "Phase: prerequisites"
  have_cmd openssl || die "openssl is required (used by scripts/create-secrets.sh)"
  have_cmd curl || die "curl is required (health check)"
  have_cmd awk || die "awk is required (template rendering)"
  [[ -f "$COMPOSE_FILE" ]] || die "missing $COMPOSE_FILE (run from kit / muxon-core root)"
  [[ -f "$TEMPLATE_CONFIG" ]] || die "missing $TEMPLATE_CONFIG"
  [[ -f "$REALM_TEMPLATE" ]] || die "missing $REALM_TEMPLATE"
  [[ -f "scripts/create-secrets.sh" ]] || die "missing scripts/create-secrets.sh"
  case "$PROFILE" in
    internal|external) ;;
    *) die "--profile must be internal or external" ;;
  esac
  if [[ "$RENDER_ONLY" -eq 0 ]]; then
    have_cmd docker || die "docker is required (install Docker Engine 24+)"
    docker info >/dev/null 2>&1 || die "cannot talk to Docker daemon (start Docker or fix permissions)"
    docker compose version >/dev/null 2>&1 || die "docker compose v2 is required"
  fi
}

collect_inputs() {
  info "Phase: collect inputs"
  if [[ -z "$MUXON_VERSION" && -f "$VERSION_FILE" ]]; then
    MUXON_VERSION="$(tr -d '[:space:]' <"$VERSION_FILE")"
  fi
  prompt_if_empty MUXON_VERSION "Image version (MUXON_VERSION)" "latest"
  prompt_if_empty INSTANCE_NAME "Instance name" "docker"
  prompt_if_empty INSTANCE_ID "Instance id" "1"
  prompt_if_empty SYSTEM_ADMIN_USERNAME "System admin username (IdP email/user)"
  prompt_if_empty TENANT_ADMIN_USERNAME "Tenant admin username" "$SYSTEM_ADMIN_USERNAME"
  prompt_if_empty CLIENT_ID "OIDC client id" "muxon-api"
  prompt_if_empty DATASOURCE_USERNAME "Datasource username" "muxon"

  if [[ "$PROFILE" == "internal" ]]; then
    prompt_if_empty ISSUER_URI "Issuer URI" "http://keycloak:8085/realms/muxon-dev"
    prompt_if_empty DATASOURCE_URL "JDBC URL" "jdbc:postgresql://postgres:5432/muxon"
  else
    prompt_if_empty ISSUER_URI "Issuer URI (required for external)"
    prompt_if_empty DATASOURCE_URL "JDBC URL (required for external)"
  fi

  [[ -n "$SYSTEM_ADMIN_USERNAME" ]] || die "system admin username is required"
  [[ -n "$ISSUER_URI" ]] || die "issuer URI is required"
  [[ -n "$DATASOURCE_URL" ]] || die "datasource URL is required"
}

phase_secrets() {
  info "Phase: create secrets"
  if [[ "$REGENERATE_SECRETS" -eq 1 ]]; then
    rm -f "${SECRETS_DIR}/muxon-passphrase" \
      "${SECRETS_DIR}/muxon-oidc-secret" \
      "${SECRETS_DIR}/muxon-db-password" \
      "${SECRETS_DIR}/keycloak-admin-password"
  fi
  # create-secrets.sh skips existing files
  sh scripts/create-secrets.sh
  [[ -f "${SECRETS_DIR}/muxon-oidc-secret" ]] || die "missing ${SECRETS_DIR}/muxon-oidc-secret"
  [[ -f "${SECRETS_DIR}/muxon-db-password" ]] || die "missing ${SECRETS_DIR}/muxon-db-password"
  [[ -f "${SECRETS_DIR}/muxon-passphrase" ]] || die "missing ${SECRETS_DIR}/muxon-passphrase"
  if [[ "$PROFILE" == "internal" ]]; then
    [[ -f "${SECRETS_DIR}/keycloak-admin-password" ]] || die "missing ${SECRETS_DIR}/keycloak-admin-password"
  fi
}

phase_render_templates() {
  info "Phase: render templates"
  local oidc_secret oidc_json admin_json
  oidc_secret="$(tr -d '\n\r' <"${SECRETS_DIR}/muxon-oidc-secret")"
  [[ -n "$oidc_secret" ]] || die "muxon-oidc-secret is empty"
  oidc_json="$(json_escape "$oidc_secret")"
  admin_json="$(json_escape "$SYSTEM_ADMIN_USERNAME")"

  cp "$TEMPLATE_CONFIG" "$OUT_CONFIG"
  replace_all "$OUT_CONFIG" "__INSTANCE_NAME__" "$INSTANCE_NAME"
  replace_all "$OUT_CONFIG" "__INSTANCE_ID__" "$INSTANCE_ID"
  replace_all "$OUT_CONFIG" "__SYSTEM_ADMIN_USERNAME__" "$SYSTEM_ADMIN_USERNAME"
  replace_all "$OUT_CONFIG" "__TENANT_ADMIN_USERNAME__" "$TENANT_ADMIN_USERNAME"
  replace_all "$OUT_CONFIG" "__ISSUER_URI__" "$ISSUER_URI"
  replace_all "$OUT_CONFIG" "__CLIENT_ID__" "$CLIENT_ID"
  replace_all "$OUT_CONFIG" "__DATASOURCE_URL__" "$DATASOURCE_URL"
  replace_all "$OUT_CONFIG" "__DATASOURCE_USERNAME__" "$DATASOURCE_USERNAME"
  require_no_placeholder "$OUT_CONFIG"

  cp "$REALM_TEMPLATE" "$REALM_RUNTIME"
  # Concrete OIDC secret (also exported into Keycloak env by muxon.yml).
  replace_all "$REALM_RUNTIME" '${MUXON_API_CLIENT_SECRET}' "$oidc_json"
  # Align primary admin identity with initial-config (first admin@muxon.dev only).
  local tmp
  tmp="$(mktemp)"
  ADMIN_JSON="$admin_json" awk '
    BEGIN { a = ENVIRON["ADMIN_JSON"]; n = 0 }
    {
      if (n < 2 && /"username": "admin@muxon.dev"/) {
        sub(/"username": "admin@muxon.dev"/, "\"username\": \"" a "\"")
        n++
      } else if (n < 2 && /"email": "admin@muxon.dev"/) {
        sub(/"email": "admin@muxon.dev"/, "\"email\": \"" a "\"")
        n++
      }
      print
    }
  ' "$REALM_RUNTIME" >"$tmp"
  mv "$tmp" "$REALM_RUNTIME"

  if grep -F '${MUXON_API_CLIENT_SECRET}' "$REALM_RUNTIME" >/dev/null 2>&1; then
    die "OIDC secret placeholder still present in $REALM_RUNTIME"
  fi
  if grep -F 'admin@muxon.dev' "$REALM_RUNTIME" >/dev/null 2>&1; then
    # Other seeded users may still use @muxon.dev; primary admin must be updated.
    if grep -E '"username": "admin@muxon.dev"|"email": "admin@muxon.dev"' "$REALM_RUNTIME" >/dev/null 2>&1; then
      die "primary admin@muxon.dev was not replaced in $REALM_RUNTIME"
    fi
  fi
  chmod 600 "$OUT_CONFIG" 2>/dev/null || true
  info "Wrote $OUT_CONFIG and $REALM_RUNTIME"
}

phase_compose_up() {
  info "Phase: start Compose (profile=$PROFILE, MUXON_VERSION=$MUXON_VERSION)"
  export MUXON_VERSION
  if [[ "$PROFILE" == "internal" ]]; then
    docker compose -f "$COMPOSE_FILE" --profile internal up -d
  else
    docker compose -f "$COMPOSE_FILE" up -d
  fi
}

phase_health() {
  if [[ "$SKIP_HEALTH" -eq 1 ]]; then
    info "Skipping health check (--skip-health)"
    return 0
  fi
  info "Phase: verify health ($HEALTH_URL, timeout ${HEALTH_TIMEOUT_SEC}s)"
  local elapsed=0
  while [[ "$elapsed" -lt "$HEALTH_TIMEOUT_SEC" ]]; do
    if curl -sf "$HEALTH_URL" >/dev/null 2>&1; then
      info "Health check OK"
      echo "Muxon Core is up at http://127.0.0.1:8080"
      echo "Logs: docker compose -f $COMPOSE_FILE logs -f core-services"
      return 0
    fi
    sleep 3
    elapsed=$((elapsed + 3))
  done
  echo "ERROR: health check did not succeed within ${HEALTH_TIMEOUT_SEC}s" >&2
  echo "Inspect: docker compose -f $COMPOSE_FILE logs -f core-services" >&2
  exit 1
}

main() {
  parse_args "$@"
  phase_prerequisites
  collect_inputs
  phase_secrets
  phase_render_templates
  if [[ "$RENDER_ONLY" -eq 1 ]]; then
    info "Render-only complete; not starting Compose"
    exit 0
  fi
  phase_compose_up
  phase_health
}

main "$@"
