# Muxon Core

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

## Documentation

- **Public guides:** [`muxon-docs`](../muxon-docs/) — concepts, admin guide, user guide
- **Developer setup:** [`dev-docs`](dev-docs/) — local development, architecture, build system

## Installation (operators)

- [Install Muxon Core](../muxon-docs/admin-guide/install-core.md) — Docker Compose, Kubernetes/Helm, VM appliance, secrets, and `initial-config.yaml`
- [Install Muxon Nexus (enterprise)](../muxon-docs/admin-guide/install-nexus.md) — Helm `muxon-nexus` and nested `muxon-core` values

<!-- ci-gate-test: verify enterprise-compatibility status -->
