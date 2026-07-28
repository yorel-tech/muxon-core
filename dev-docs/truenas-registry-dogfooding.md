# Dogfooding Muxon with a TrueNAS local registry

Use TrueNAS on your LAN as a **persistent Docker Registry v2** (and optionally ChartMuseum) so you can build Muxon on a dev machine, push images to the NAS, and deploy to a k3s/Kubernetes cluster for dogfooding.

This guide complements [development_setup.md](development_setup.md) (local JVM dev) and the operator install guides in [`muxon-docs`](../../muxon-docs/admin-guide/).

---

## Architecture

```text
Dev machine (muxon-build)          TrueNAS (LAN)              k3s / Kubernetes
─────────────────────────          ─────────────              ────────────────
make images-oss  ──docker push──►  registry:2 :5000  ◄──pull──  muxon namespace
make helm-oss    ──helm install────────────────────────────────►  Helm release
```

**Why TrueNAS instead of `make registry-up` on localhost?**

- Images survive reboots (ZFS-backed storage).
- Your cluster pulls from one stable LAN address, not your laptop.
- The build system already supports a custom registry via the `REGISTRY` environment variable.

Images are published as:

```text
<truenas-host>:5000/scal/<service>:<version>
```

Default namespace is `scal` (`REGISTRY_NAMESPACE`).

---

## Prerequisites

| Component | Requirement |
|-----------|-------------|
| TrueNAS | **SCALE** (25.04+; Compose `include:` workflow documented for 25.10+) with Apps enabled |
| Dev machine | Docker or Podman, `muxon-build` checkout, network access to TrueNAS |
| Cluster | k3s or Kubernetes that can reach TrueNAS on port 5000 |
| Tools on dev machine | `make`, `helm`, `kubectl` |

---

## Part 1 — Deploy containers on TrueNAS

TrueNAS SCALE runs apps in Docker containers. You can deploy the registry (and ChartMuseum) in two ways.

### Option A — Custom App wizard (single container)

Good for a quick registry-only setup.

1. **Create a dataset** for app data, e.g. `/mnt/tank/apps/muxon-registry`.
2. Go to **Apps → Discover Apps → Custom App**.
3. Use **Install via YAML** and paste a minimal registry service, or configure manually:
   - Image: `registry:2`
   - Publish port: `5000` → container `5000`
   - Host path: `/mnt/tank/apps/muxon-registry` → `/var/lib/registry`
   - Env: `REGISTRY_STORAGE_DELETE_ENABLED=true`
4. Deploy and verify from any LAN machine:

   ```bash
   curl http://<truenas-ip-or-hostname>:5000/v2/_catalog
   ```

### Option B — Docker Compose on TrueNAS (recommended)

**Yes — you can run Docker Compose on TrueNAS SCALE.** TrueNAS does not use `docker compose` from the shell as the primary workflow; instead you deploy a **Custom App via YAML** that references a Compose file on a dataset.

This matches how `muxon-build/registry-compose.yml` is structured (registry + ChartMuseum).

#### 1. Create directories on TrueNAS

On the TrueNAS shell or via the UI file manager:

```bash
mkdir -p /mnt/tank/apps/muxon-registry/data/registry
mkdir -p /mnt/tank/apps/muxon-registry/data/charts
```

#### 2. Copy the Compose file to TrueNAS

From your dev machine, copy `muxon-build/registry-compose.yml` to the NAS, e.g.:

```bash
scp muxon-build/registry-compose.yml \
  root@<truenas-ip>:/mnt/tank/apps/muxon-registry/compose.yml
```

Adjust volume paths in the Compose file so they point at the TrueNAS dataset (not `./data/...` relative paths):

```yaml
services:
  docker-registry:
    image: registry:2
    container_name: muxon-docker-registry
    ports:
      - "5000:5000"
    volumes:
      - /mnt/tank/apps/muxon-registry/data/registry:/var/lib/registry
    environment:
      REGISTRY_STORAGE_DELETE_ENABLED: "true"
    restart: unless-stopped

  chartmuseum:
    image: ghcr.io/helm/chartmuseum:v0.16.0
    container_name: muxon-chartmuseum
    user: "0:0"
    ports:
      - "8081:8080"
    environment:
      STORAGE: local
      STORAGE_LOCAL_ROOTDIR: /charts
      DISABLE_API: "false"
      ALLOW_OVERWRITE: "true"
    volumes:
      - /mnt/tank/apps/muxon-registry/data/charts:/charts
    restart: unless-stopped
```

#### 3. Install as a Custom App (include external Compose)

**Apps → Discover Apps → Custom App → Install via YAML**

On TrueNAS **25.10+**, the UI requires a top-level `services` key even when using `include`. Use:

```yaml
include:
  - path: /mnt/tank/apps/muxon-registry/compose.yml
services: {}
```

On older SCALE versions, `include` alone may be enough:

```yaml
include:
  - /mnt/tank/apps/muxon-registry/compose.yml
```

Click **Save** and wait for both containers to show as running.

#### 4. Verify

```bash
# Docker registry
curl http://<truenas-host>:5000/v2/_catalog

# ChartMuseum (optional — for helm repo add from LAN)
curl http://<truenas-host>:8081/health
```

#### Updating the stack

Edit `compose.yml` on the dataset, then **redeploy** the Custom App from the TrueNAS UI (same as updating any other app). You can also manage the file with Code Server or SSH.

#### Can I run `docker compose up` directly on TrueNAS?

- **Supported path:** Custom App + Compose file on a dataset (above).
- **Shell `docker compose`:** Not the normal TrueNAS workflow; the Apps subsystem owns container lifecycle. Prefer the Custom App approach so restarts, logging, and upgrades go through the UI.

---

## Part 2 — Configure the build machine

The helper `muxon-build/scripts/ensure-insecure-local-registry.sh` only auto-configures `localhost:5000`. For a TrueNAS HTTP registry, configure your engine manually.

### Docker Engine

Add to `/etc/docker/daemon.json` (merge with existing keys):

```json
{
  "insecure-registries": ["<truenas-host>:5000"]
}
```

Restart Docker:

```bash
sudo systemctl restart docker
```

### Podman

Create `~/.config/containers/registries.conf.d/99-truenas-muxon.conf`:

```toml
[[registry]]
location = "<truenas-host>:5000"
insecure = true
```

Replace `<truenas-host>` with a stable hostname or IP (e.g. `truenas.lan`, `192.168.1.50`).

---

## Part 3 — Build and push images

From the monorepo root, use `muxon-build`:

```bash
cd muxon-build

# OSS dogfood build
make images-oss \
  REGISTRY=<truenas-host>:5000 \
  REGISTRY_NAMESPACE=scal \
  VERSION=0.1.0-dogfood
```

This compiles muxon-core + muxon-web, builds Docker images, and pushes:

| Image | Tag example |
|-------|-------------|
| core-services | `<truenas-host>:5000/scal/core-services:0.1.0-dogfood` |
| orchestrator | `<truenas-host>:5000/scal/orchestrator:0.1.0-dogfood` |
| console-proxy | `<truenas-host>:5000/scal/console-proxy:0.1.0-dogfood` |
| muxon-initializer | `<truenas-host>:5000/scal/muxon-initializer:0.1.0-dogfood` |
| web | `<truenas-host>:5000/scal/web:0.1.0-dogfood` |

Verify on TrueNAS:

```bash
curl http://<truenas-host>:5000/v2/_catalog
curl http://<truenas-host>:5000/v2/scal/core-services/tags/list
```

**Enterprise:** use `make images-enterprise` with the same `REGISTRY` and `VERSION`.

---

## Part 4 — Package and deploy with Helm

### Package the chart

```bash
make helm-oss \
  REGISTRY=<truenas-host>:5000 \
  REGISTRY_NAMESPACE=scal \
  VERSION=0.1.0-dogfood \
  HELM_REPO=http://<truenas-host>:8081
```

Output: `dist/helm/muxon-core-<version>.tgz`

**Note:** `package-helm.sh` rewrites `deploy/helm/muxon-core/values.yaml` with your registry. That dirties the working tree; restore with `git checkout -- deploy/helm/` when done, or pass overrides at install time (below).

**ChartMuseum upload:** `package-helm.sh` auto-uploads only when `HELM_REPO` matches `http://localhost:*`. With ChartMuseum on TrueNAS, upload manually:

```bash
curl -X POST --data-binary "@dist/helm/muxon-core-0.1.0-dogfood.tgz" \
  http://<truenas-host>:8081/api/charts
```

Or skip ChartMuseum and install directly from the `.tgz`.

### Configure the cluster to pull from TrueNAS (HTTP)

On **each k3s node**, create `/etc/rancher/k3s/registries.yaml`:

```yaml
mirrors:
  "<truenas-host>:5000":
    endpoint:
      - "http://<truenas-host>:5000"
```

Restart k3s:

```bash
sudo systemctl restart k3s          # server
sudo systemctl restart k3s-agent    # agents
```

For vanilla Kubernetes/containerd, add a `hosts.toml` under `/etc/containerd/certs.d/<truenas-host>:5000/`.

### Install Muxon

```bash
kubectl create namespace muxon

# Create muxon-secrets first — see deploy/helm/muxon-core/values.yaml
# kubectl create secret generic muxon-secrets -n muxon ...

helm install muxon ../dist/helm/muxon-core-0.1.0-dogfood.tgz \
  -n muxon \
  --set image.repository=<truenas-host>:5000/scal/core-services \
  --set image.tag=0.1.0-dogfood \
  --set postgres.enabled=true \
  --set keycloak.enabled=true
```

Adjust `postgres` / `keycloak` for your dogfood scenario (bundled vs external DB/IdP).

### Optional: Helm repo from ChartMuseum on TrueNAS

```bash
helm repo add muxon-local http://<truenas-host>:8081
helm repo update
helm search repo muxon-local
```

---

## Part 5 — Day-to-day iteration

```bash
# 1. Change code
# 2. Rebuild and push
make images-oss REGISTRY=<truenas-host>:5000 VERSION=0.1.0-dogfood

# 3. Upgrade release
helm upgrade muxon dist/helm/muxon-core-0.1.0-dogfood.tgz -n muxon \
  --set image.repository=<truenas-host>:5000/scal/core-services \
  --set image.tag=0.1.0-dogfood
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `http: server gave HTTP response to HTTPS client` on push | Add TrueNAS host to `insecure-registries` (Docker) or `registries.conf.d` (Podman) |
| k3s `ImagePullBackOff` | Add `registries.yaml` mirror on all nodes; confirm `curl http://<truenas>:5000/v2/` from the node |
| Custom App YAML error: missing `services` key (25.10+) | Add `services: {}` under `include:` (see Part 1) |
| ChartMuseum permission errors on volume | Run ChartMuseum as `user: "0:0"` or fix dataset ownership |
| `package-helm.sh` dirty git tree | `git checkout -- deploy/helm/muxon-core/` or use `--set` at install time |

---

## Optional upgrades

| Goal | Approach |
|------|----------|
| TLS + auth + UI | Deploy **Harbor** on TrueNAS (Custom App or catalog app); point `REGISTRY` at Harbor |
| Appliance dogfood | `make appliance-oss REGISTRY=<truenas-host>:5000` so the VM preloads from TrueNAS |
| Air-gapped LAN | TrueNAS registry + local `.tgz` Helm install; no external pulls after initial image load |

---

## Quick reference

```bash
# TrueNAS: deploy registry (+ optional ChartMuseum) via Custom App + compose.yml on dataset

# Dev machine: build and push
cd muxon-build
make images-oss REGISTRY=truenas.lan:5000 VERSION=0.1.0-dogfood
make helm-oss REGISTRY=truenas.lan:5000 HELM_REPO=http://truenas.lan:8081 VERSION=0.1.0-dogfood

# Cluster: install
helm install muxon dist/helm/muxon-core-0.1.0-dogfood.tgz -n muxon \
  --set image.repository=truenas.lan:5000/scal/core-services \
  --set image.tag=0.1.0-dogfood
```

Related files in the monorepo:

- `muxon-build/Makefile` — build orchestration
- `muxon-build/registry-compose.yml` — local registry + ChartMuseum (adapt for TrueNAS)
- `muxon-build/scripts/build-images.sh`, `push-images.sh`, `package-helm.sh` — shared scripts used by local and CI builds
