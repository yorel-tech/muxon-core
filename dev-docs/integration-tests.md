# Integration tests

## Local

```bash
cd muxon-core/integration-tests
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock ../gradlew test --tests "com.scal.muxon.tests.IntegrationTestSuite"
```

This runs tests in order:

1. InfraDeployer — starts infrastructure (Testcontainers)
2. TenantTests — integration tests
3. InfraCleanup — stops infrastructure

Or run all tests with ordering:

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock ../gradlew test
```

### Prerequisites

```bash
./gradlew :services:core-services:bootJar --no-daemon
./gradlew :services:muxon-initializer:bootJar --no-daemon
docker build -t muxon-core-services:test -f services/core-services/Dockerfile .
```

Prefer pulling a CI-published image when available:

```bash
docker pull ghcr.io/<owner>/scal/core-services:main
docker tag ghcr.io/<owner>/scal/core-services:main muxon-core-services:test
```

### Debugging

```bash
podman system service --time=0 --log-level debug
```

## GitHub Actions

Canonical workflow: `.github/workflows/integration-tests.yml` (monorepo root).

Configured by `.github/ci-config.yaml`:

| `integration.mode` | Behavior |
|--------------------|----------|
| `manual` (default) | Trigger via Actions UI or `gh workflow run integration-tests.yml` |
| `immediate` | After every push to `main` |
| `nightly` | Daily cron |
| `weekly` | Weekly cron |

Runs on a **self-hosted** runner with labels `self-hosted` + `proxmox-lab` (must reach your Proxmox lab). See `muxon-docs/admin-guide/ci.md`.
