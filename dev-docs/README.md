# Developer documentation

Internal setup, architecture, and build documentation for the Muxon platform. For operator and end-user guides, see [`muxon-docs`](../../muxon-docs/).

## Setup and local development

| Document | Description |
|----------|-------------|
| [development_setup.md](development_setup.md) | Local JVM dev: Compose, services, web UI |
| [local-ghcr-compose-deploy.md](local-ghcr-compose-deploy.md) | Pull GHCR images and run Compose install kit locally |
| [initial-bootstrap.md](initial-bootstrap.md) | muxon-initializer, secrets, Flyway, generated config |
| [idp-settings-firsttime.md](idp-settings-firsttime.md) | First-time IdP / OIDC configuration |
| [integration-tests.md](integration-tests.md) | Integration test suite and CI notes |
| [truenas-registry-dogfooding.md](truenas-registry-dogfooding.md) | LAN registry on TrueNAS for dogfooding |

## Operations and debugging

| Document | Description |
|----------|-------------|
| [vm-orchestration-flow.md](vm-orchestration-flow.md) | VM lifecycle orchestration internals |
| [libvirt-register.md](libvirt-register.md) | Registering a libvirt provider |
| [flyway-repair.md](flyway-repair.md) | Flyway migration repair |
| [openapi-v2.md](openapi-v2.md) | OpenAPI v2 notes |
| [CAPABILITIES.md](CAPABILITIES.md) | Platform capability flags |
| [storage/capability-based-storage-README.md](storage/capability-based-storage-README.md) | Capability-based storage design |

## Architecture reference

See [`architecture/`](architecture/README.md) for system design docs (moved from `muxon-design`).

## Build system

See [`build/`](build/README.md) for image, Helm, and appliance builds (moved from `muxon-build`).

## UI development

| Document | Description |
|----------|-------------|
| [web-design-specification.md](web-design-specification.md) | Web UI design tokens and component spec |

## Internal notes

Scratch notes and design drafts in [`notes/`](notes/) — not published to end users.

## REST client collections

Bruno collections for manual API testing: [`rest-client-bruno/`](rest-client-bruno/).

## Public documentation

| Audience | Location |
|----------|----------|
| Concepts, admin guide, user guide | [`muxon-docs`](../../muxon-docs/) |
| Install Core | [`muxon-docs/admin-guide/install-core.md`](../../muxon-docs/admin-guide/install-core.md) |
| Install Nexus | [`muxon-docs/admin-guide/install-nexus.md`](../../muxon-docs/admin-guide/install-nexus.md) |
