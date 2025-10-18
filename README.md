# Infron Core

Open, source-available foundation for private cloud control.

## Modules
- `libs/core-api` — OpenAPI spec + DTOs
- `libs/core-proto` — gRPC definitions
- `libs/core-auth` — OIDC utilities
- `libs/core-spi` — Plugin interfaces
- `libs/core-commons` — Common utilities
- `services/api-gateway` — Spring Boot WebFlux API
- `services/orchestrator` — Quarkus Kafka/gRPC workers
- `services/usage-billing` — Quarkus usage aggregator
- `services/agent` — Micronaut/Quarkus native daemon
- `services/console-proxy` — Netty/noVNC bridge
- `providers/*` — Provider adapters

## Quickstart (dev)
```
docker compose -f compose/dev-stack.yml up -d
./gradlew :services:api-gateway:bootRun
```
