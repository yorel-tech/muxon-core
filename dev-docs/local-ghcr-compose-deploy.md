# Deploy GHCR images locally with Docker Compose

How to pull Muxon images from GitHub Container Registry (GHCR) and run them on a single host with the Compose install kit (bundled Postgres + Keycloak).

This complements:

- Operator guide: [`muxon-docs/admin-guide/install-core.md`](../../muxon-docs/admin-guide/install-core.md)
- Kit checklist: [`INSTALL.md`](../INSTALL.md) / `./install.sh`
- Local JVM day-to-day: [development_setup.md](development_setup.md) (`compose/dev-stack.yml`)
- LAN registry dogfooding: [truenas-registry-dogfooding.md](truenas-registry-dogfooding.md)

---

## What you get

| Piece | Role |
|-------|------|
| `ghcr.io/yorel-tech/yorel/core-services:<tag>` | Muxon Core (bootstrap + API) |
| `postgres:18` | Bundled DB (`--profile internal`) |
| `quay.io/keycloak/keycloak:26.4.0` | Bundled IdP (`--profile internal`) |
| Secret files under `secrets/` | Passphrase, OIDC secret, DB password, Keycloak admin |
| `initial-config.yaml` | Non-secret bootstrap config (rendered from template) |
| `compose/keycloak/realm-muxon-dev.runtime.json` | Realm import with OIDC secret + admin identity filled in |

Passwords used at runtime come from **secret files** (`/run/secrets/...`), not from `initial-config.yaml`. The YAML `clientSecret` / `datasource.password` fields are placeholders only.

---

## Prerequisites

- Docker Engine 24+ and Compose v2
- OpenSSL, curl
- GitHub account with access to `yorel-tech` packages
- Token scopes including **`read:packages`** (and org SSO authorized if required)

---

## 1. Get the install kit

**From an OSS release** (preferred for trials): download `muxon-compose-install-<version>.zip` / `.tar.gz` from the GitHub Release, unpack, `cd` into the folder.

**From this repo** (developers):

```bash
cd muxon-core
# or package like CI:
cd ../muxon-core-build
VERSION=0.1.0-local ./scripts/package-compose-install.sh
# → dist/compose-install/muxon-compose-install-0.1.0-local.zip
```

Kit layout:

```text
muxon-compose-install-<version>/
  INSTALL.md
  install.sh
  VERSION
  compose/muxon.yml
  compose/initial-config.yaml.template
  compose/keycloak/realm-muxon-dev.json
  scripts/create-secrets.sh
```

Runtime outputs (not in the zip): `secrets/`, `initial-config.yaml`, `compose/keycloak/realm-muxon-dev.runtime.json`.

---

## 2. Authenticate Docker to GHCR

`gh auth login` alone does **not** log Docker into GHCR.

```bash
gh auth status   # must include read:packages
# if missing:
gh auth refresh -h github.com -s read:packages

gh auth token | docker login ghcr.io -u "$(gh api user -q .login)" --password-stdin
```

Without `read:packages`, pulls often fail with **403 Forbidden** (Docker may phrase it as “failed to resolve reference”).

---

## 3. Pick a tag that exists

Compose pulls:

```text
ghcr.io/yorel-tech/yorel/core-services:${MUXON_VERSION}
```

`main` may not exist. List tags:

```bash
# quote the URL so the shell does not eat '?'
gh api "orgs/yorel-tech/packages/container/yorel%2Fcore-services/versions?per_page=20" \
  --jq '.[].metadata.container.tags'
```

Example tags seen in practice: `latest`, `main.6` (not necessarily `main`).

Confirm pull:

```bash
docker pull ghcr.io/yorel-tech/yorel/core-services:latest
```

---

## 4. Run the install

```bash
cd muxon-compose-install-<version>   # or muxon-core/

./install.sh --non-interactive \
  --admin-user admin@example.com \
  --version latest
```

What the script does (same order as `INSTALL.md`):

1. Check Docker / Compose / openssl / curl (Docker only required when not `--render-only`)
2. Create `secrets/` via `scripts/create-secrets.sh` (skips existing unless `--regenerate-secrets`)
3. Render `initial-config.yaml` and `realm-muxon-dev.runtime.json` from inputs + `muxon-oidc-secret`
4. `docker compose -f compose/muxon.yml --profile internal up -d` with `MUXON_VERSION`
5. Poll `http://127.0.0.1:8080/actuator/health`

Useful flags: `--render-only`, `--profile external`, `--regenerate-secrets`, `--help`.

---

## 5. Secrets and placeholders (summary)

| Value | Source |
|-------|--------|
| DB password | `secrets/muxon-db-password` → Postgres `POSTGRES_PASSWORD_FILE` and bootstrap `--muxon-db-password` |
| OIDC client secret | `secrets/muxon-oidc-secret` → bootstrap + realm runtime + Keycloak env |
| Encryption passphrase | `secrets/muxon-passphrase` |
| Keycloak admin console | `secrets/keycloak-admin-password` |
| System admin username | Install input → `initial-config.yaml` + primary user in realm runtime |
| IdP user login passwords | Still in realm JSON for local trials (e.g. seeded users) |

---

## 6. Known Compose / runtime pitfalls

### Postgres 18 volume path

`postgres:18` must mount the volume at **`/var/lib/postgresql`**, not `/var/lib/postgresql/data`. If you see an error about unused mount `/var/lib/postgresql/data`, use the current `compose/muxon.yml`, then:

```bash
docker compose -f compose/muxon.yml --profile internal down
docker volume rm muxon_postgres_data
docker compose -f compose/muxon.yml --profile internal up -d
```

### “Skipped: optional dependency”

`depends_on: required: false` lets the same compose file run without bundled Postgres/Keycloak (`--profile` omitted). With `--profile internal`, those services still start; the “Skipped” line means Compose did not treat that wait as fatal (e.g. Keycloak never becomes “healthy”).

### Keycloak slow / unhealthy

Keycloak often starts in ~20s and imports the realm, but the Compose healthcheck (`/health/ready`) may never go healthy in `start-dev`. Compose then waits a long time, skips the optional Keycloak dependency, and starts `core-services` anyway. Deprecation warnings for `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` are rename hints only (`KC_BOOTSTRAP_ADMIN_*`).

### Check status and logs

```bash
docker compose -f compose/muxon.yml --profile internal ps -a
docker compose -f compose/muxon.yml --profile internal logs postgres
docker compose -f compose/muxon.yml --profile internal logs keycloak
docker compose -f compose/muxon.yml --profile internal logs core-services
```

---

## 7. Application image bug (SubnetRbac / Hibernate 7)

If bootstrap dies with:

```text
Operand of 'member of' operator must be a plural path
... :permission MEMBER OF r.permissions
```

that is a **code** issue in older images: `permissions` is a Postgres `TEXT[]`, and JPQL `MEMBER OF` is invalid for it under Hibernate 7. Fixed in-repo in `SubnetRbacRepository` (native `= ANY(...)` / `cardinality(...)`).

Until you rebuild and push (or retag a local image), GHCR `latest` may still fail. After publishing a fixed image:

```bash
docker compose -f compose/muxon.yml --profile internal pull core-services
docker compose -f compose/muxon.yml --profile internal up -d --force-recreate core-services
```

---

## 8. Publish / rebuild images yourself (optional)

From the monorepo build tree (needs `write:packages` and a successful Java/Node build):

```bash
gh auth token | docker login ghcr.io -u "$(gh api user -q .login)" --password-stdin
cd muxon-core-build
REGISTRY=ghcr.io/yorel-tech VERSION=<tag> VARIANT=oss ./scripts/publish-images-to-ghcr.sh
```

Or trigger CI: `gh workflow run build-images.yml`, then use the tags that workflow pushes (`main.<n>`, sometimes `latest` / `main`).

OSS releases also attach the Compose install kit via `package-compose-install` in `.github/workflows/oss-release.yml`. Weekly `build-images.yml` does **not** attach that kit.

---

## Quick reference

```bash
# Auth
gh auth refresh -h github.com -s read:packages
gh auth token | docker login ghcr.io -u "$(gh api user -q .login)" --password-stdin

# Tags
gh api "orgs/yorel-tech/packages/container/yorel%2Fcore-services/versions?per_page=20" \
  --jq '.[].metadata.container.tags'
docker pull ghcr.io/yorel-tech/yorel/core-services:latest

# Install
./install.sh --non-interactive --admin-user admin@example.com --version latest

# Health
curl -sS http://127.0.0.1:8080/actuator/health
```
