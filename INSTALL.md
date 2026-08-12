# Muxon Core — Docker Compose install kit

This kit installs Muxon Core with Docker Compose. You can use the guided script or the manual steps below (same order).

**Requirements:** 64-bit x86 host, Docker Engine 24+, Docker Compose v2, OpenSSL, curl. About 4 GB RAM recommended for the internal (bundled Postgres + Keycloak) profile.

## Guided install (recommended)

```bash
./install.sh
```

Non-interactive example (internal profile):

```bash
./install.sh --non-interactive \
  --profile internal \
  --admin-user admin@example.com \
  --version "$(cat VERSION)"
```

Flags: `--help` lists all options. `--render-only` creates secrets and rendered config/realm without starting Compose. `--regenerate-secrets` overwrites existing secret files.

## Manual steps

### 1. Prerequisites

```bash
docker version
docker compose version
openssl version
curl --version
```

### 2. Create secrets

```bash
./scripts/create-secrets.sh
```

Creates `secrets/` (mode 600 files):

| File | Purpose |
|------|---------|
| `muxon-passphrase` | Encryption passphrase |
| `muxon-oidc-secret` | OIDC client secret for `muxon-api` |
| `muxon-db-password` | PostgreSQL / app DB password |
| `keycloak-admin-password` | Keycloak admin console (internal profile) |

Do **not** put these values into `initial-config.yaml` for runtime auth.

### 3. Render templates (placeholders)

Copy and substitute placeholders, or run `./install.sh --render-only`.

**`compose/initial-config.yaml.template` → `initial-config.yaml` (kit root)**

| Placeholder | Source |
|-------------|--------|
| `__INSTANCE_NAME__` | Operator input (default `docker`) |
| `__INSTANCE_ID__` | Operator input (default `1`) |
| `__SYSTEM_ADMIN_USERNAME__` | Operator input (must exist in IdP) |
| `__TENANT_ADMIN_USERNAME__` | Operator input (default: same as system admin) |
| `__ISSUER_URI__` | Internal: `http://keycloak:8085/realms/muxon-dev`; else your IdP issuer |
| `__CLIENT_ID__` | Default `muxon-api` |
| `__DATASOURCE_URL__` | Internal: `jdbc:postgresql://postgres:5432/muxon` |
| `__DATASOURCE_USERNAME__` | Default `muxon` |
| `clientSecret` / `password` | Leave as placeholders — real values from secret files |

**`compose/keycloak/realm-muxon-dev.json` → `compose/keycloak/realm-muxon-dev.runtime.json`**

| Value in template | Replaced with |
|-------------------|---------------|
| `${MUXON_API_CLIENT_SECRET}` | Contents of `secrets/muxon-oidc-secret` |
| Primary admin `username` / `email` (`admin@muxon.dev`) | Same as `__SYSTEM_ADMIN_USERNAME__` |

Seeded Keycloak user **login passwords** in the realm JSON (e.g. `Infr0n@1234`) remain in that file for local trials. Keycloak **admin console** password is only in `secrets/keycloak-admin-password`.

### 4. Start Compose

Bundled Postgres + Keycloak:

```bash
export MUXON_VERSION="$(cat VERSION)"
docker compose -f compose/muxon.yml --profile internal up -d
```

External DB / IdP only:

```bash
export MUXON_VERSION="$(cat VERSION)"
docker compose -f compose/muxon.yml up -d
```

### 5. Verify

```bash
docker compose -f compose/muxon.yml ps
curl -sS http://127.0.0.1:8080/actuator/health
docker compose -f compose/muxon.yml logs -f core-services
```

### 6. Uninstall

```bash
docker compose -f compose/muxon.yml --profile internal down
# add -v to destroy the Postgres volume (destructive)
```

## Layout

```text
.
  INSTALL.md
  install.sh
  VERSION                 # default image tag (MUXON_VERSION)
  compose/muxon.yml
  compose/initial-config.yaml.template
  compose/keycloak/realm-muxon-dev.json          # pristine
  compose/keycloak/realm-muxon-dev.runtime.json  # generated
  scripts/create-secrets.sh
  secrets/                                       # generated
  initial-config.yaml                            # generated
```
