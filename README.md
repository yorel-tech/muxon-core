# Infron Core

Open, source-available foundation for private cloud control.

## Modules
- `libs/core-api` — OpenAPI spec + DTOs
- `libs/core-proto` — gRPC definitions
- `libs/core-auth` — OIDC utilities
- `libs/core-spi` — Plugin interfaces
- `libs/core-commons` — Common utilities
- `services/core-services` — Spring Boot API
- `services/orchestrator` — Quarkus queue/gRPC workers
- `services/usage-billing` — Quarkus usage aggregator
- `services/console-proxy` — Netty/noVNC bridge
- `providers/*` — Provider adapters

## Quickstart (dev)
```
docker compose -f compose/dev-stack.yml up -d
./gradlew :services:core-services:bootRun
```

## Installation (operators)

- [Install Infron Core](docs/admin-guide/install-core.md) — Docker Compose, Kubernetes/Helm, VM appliance, secrets, and `initial-config.yaml`
- [Install Infron Nexus (enterprise)](docs/admin-guide/install-nexus.md) — Helm `infron-nexus` and nested `infron-core` values
