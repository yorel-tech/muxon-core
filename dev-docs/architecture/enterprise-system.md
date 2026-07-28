# Muxon Enterprise (Nexus) System

muxon-enterprise (`com.yorel.muxon.ent:0.1.0`) extends muxon-core without forking the REST API. The primary pattern is **composition + override**: depend on core artifacts, scan core Spring packages, replace selected beans, and add enterprise-only controllers and migrations.

## Relationship to Core

```mermaid
flowchart LR
    subgraph Nexus["muxon-enterprise"]
        NS[nexus-services]
        NA[nexus-auth]
        NP[nexus-persistence]
        NQ[nexus-queue-kafka-autoconfig]
        ENT[ent.* override beans]
    end

    subgraph CoreArtifacts["muxon-core (Maven / includeBuild)"]
        CS[core-services JAR]
        CA[core-auth]
        CP[core-persistence]
        AA[auth-api]
    end

    NS --> CS
    NS --> CA
    NS --> CP
    NS --> AA
    NS --> NA
    NS --> NP
    NS --> ENT
    NQ -.->|@Primary queue beans| NS
```

Gradle `settings.gradle.kts`:

- `includeBuild("../muxon-core")` when local checkout exists
- Otherwise resolves `com.yorel.muxon:*:0.1.0` from Maven Local / GitHub Packages

## Module Map

```
muxon-enterprise/
├── libs/
│   ├── nexus-api              # Enterprise OpenAPI (Projects API, etc.)
│   ├── nexus-auth             # Enterprise Permission enum, @RequiresPermission
│   ├── nexus-persistence      # Enterprise JPA entities, Flyway enterprise migrations
│   ├── nexus-rbac             # RBAC service helpers
│   ├── nexus-billing          # Billing integration hooks
│   ├── nexus-branding         # White-label configuration
│   ├── nexus-queue-kafka      # Kafka queue implementations
│   └── nexus-queue-kafka-autoconfig  # Spring Boot auto-config for Kafka queues
├── services/
│   ├── nexus-services         # Unified API process (core + enterprise)
│   ├── nexus-initializer      # Enterprise bootstrap
│   ├── api-gateway            # Spring Cloud Gateway edge
│   ├── usage-billing          # Usage metering service
│   ├── director               # Multi-cluster director (stub)
│   ├── net-advanced           # Advanced networking (stub)
│   └── audit-export           # Audit log export (stub)
└── ui/themes                  # Enterprise UI themes
```

## nexus-services — Core Wrapper

`NexusServicesApplication` is the enterprise replacement for `CoreServicesApplication`:

```java
@ComponentScan(basePackages = {
    "com.yorel.muxon.config", "com.yorel.muxon.security", "com.yorel.muxon.controllers",
    "com.yorel.muxon.services", "com.yorel.muxon.events", "com.yorel.muxon.grpc",
    "com.yorel.muxon.info", "com.yorel.muxon.hateoas", "com.yorel.muxon.auth",
    "com.yorel.muxon.web", "com.yorel.muxon.db.resolver", "com.yorel.muxon.ent"
})
@EntityScan({ "com.yorel.muxon.ent.db.model", "com.yorel.muxon.db.model" })
@EnableJpaRepositories({ "com.yorel.muxon.ent.db.repository", "com.yorel.muxon.db.repository" })
```

Effects:

- All core REST controllers and services are active unchanged
- Enterprise controllers (`com.yorel.muxon.ent.controllers.*`) add endpoints (projects, roles, bindings)
- Enterprise entities join the same persistence unit as core entities
- `com.yorel.muxon.ent.*` beans override core defaults via `@Primary`

### Dependencies (build.gradle.kts)

```
com.yorel.muxon:core-api, core-auth, core-persistence, auth-api, core-services
project(:libs:nexus-api, nexus-auth, nexus-persistence)
+ spring-boot-starter-data-redis
```

## Enterprise Overrides

### 1. AuthorizationService (`@Primary`)

| | Core OSS | Enterprise |
|---|---|---|
| Class | `com.yorel.muxon.services.AuthorizationServiceImpl` | `com.yorel.muxon.ent.services.AuthorizationServiceImpl` |
| Tenant cache | Caffeine in-memory | Redis (`perm:tenants:{externalId}`) |
| Permission sources | `RoleRegistry` built-in roles only | Built-in roles **+** `nexus_role_permissions` DB catalog |
| ABAC | None | Optional OPA (`authz.opa.enabled`, `authz.opa.url`) |

Enterprise permission resolution merges:

1. Built-in role permissions from core `RoleRegistry`
2. Custom role permissions from `RolePermissionRepository.findPermissionActionsByRoleName()`

When OPA is enabled, explicit OPA **deny** overrides role grants; OPA **allow** grants access even without a matching role permission.

### 2. Queue backend (Kafka)

`NexusQueueKafkaAutoConfiguration` activates when `muxon.queue.backend=kafka`:

| Bean | Implementation |
|---|---|
| `CommandQueue` | `KafkaCommandQueue` |
| `TaskEventQueue` | `KafkaTaskEventQueue` |
| `EntityEventQueue` | `KafkaEntityEventQueue` |
| `EventPublisher` | `KafkaEventPublisher` |

Registered with `@AutoConfigureBefore(QueueDbConfiguration)` and `@ConditionalOnMissingBean` so Kafka wins over DB adapters.

Topics (defaults): `muxon.vm.commands`, `muxon.task.events`, `muxon.entity.events`, `muxon.vm.events`.

### 3. Worker scaling flag

`UnifiedTaskPoller` in orchestrator is disabled when `muxon.enterprise.enabled=true` (`@ConditionalOnProperty`). Enterprise deployments are expected to run horizontally scaled worker pollers (separate process or replicated orchestrator).

### 4. Capability advertisement

`EnterpriseCapabilityProvider` adds capabilities beyond core:

- `identity.rbac`, `identity.sso`
- `audit.logs`
- `cluster.ha`

`CapabilityRegistry` merges all `CapabilityProvider` beans; clients read the union via `/api/v1/info`.

### 5. Tenant-scoped JWT resolver (prepared)

`TenantAwareAuthenticationManagerResolver` resolves the identity provider by tenant ID from the request path (`TenantResolver` / `PathBasedTenantResolver`) instead of always using the system default.

**Status:** Implemented but **not registered as a Spring bean** yet. Comment in source warns that activating it alongside core `SecurityConfig` would duplicate `AuthenticationManagerResolver` beans — requires a Nexus-specific security configuration to wire explicitly.

### 6. Enterprise permission annotations

Enterprise controllers use `com.yorel.muxon.ent.auth.RequiresPermission` with `com.yorel.muxon.ent.auth.Permission`:

- `project:read`, `project:edit`, `project:manage`, `project:settings`

Core `PermissionInterceptor` only recognizes `auth-api` annotations (`com.yorel.muxon.auth.RequiresPermission`). Enterprise project controllers use the nexus variant; a unified interceptor bridge may be needed for full integration.

## Enterprise-Only Features

### Projects

- Controller: `NexusProjectsController` implements `ProjectsApi`
- Service: `NexusProjectService`
- Entity: `NexusProjectEntity` → `nexus_project` table (tenant-scoped, optional datacenter grant, resource limits JSONB, owner FK)

Project permissions are enterprise-only; core `RoleRegistry` returns `project.*` capabilities as `false` for OSS.

### Extended RBAC management

- `NexusRolesController` / `NexusRolesService` — CRUD for `nexus_roles`
- `NexusBindingsController` / `NexusBindingsService` — role-permission assignments
- Tables: `nexus_permissions`, `nexus_roles`, `nexus_role_permissions`

Enterprise RBAC is stored separately from core `role_bindings`. Bindings still reference role **names** that may map to either built-in (`RoleRegistry`) or custom (`nexus_roles`) definitions.

### Satellite services

| Service | Purpose |
|---|---|
| `api-gateway` | Reactive edge: JWT decode, audience validation (`muxon-api`), CORS, forwards to nexus-services |
| `usage-billing` | Usage aggregation and billing integration |
| `director` | Multi-site / federation control (scaffold) |
| `net-advanced` | Advanced SDN features (scaffold) |
| `audit-export` | Long-term audit export (scaffold) |

## api-gateway

Spring Cloud Gateway (`GatewayApp`) — scans only `com.yorel.muxon.gateway`.

**SecurityConfig:**

- Reactive `SecurityWebFilterChain`
- `JwtDecoder` from issuer URI with audience validator (`muxon-api`)
- Permits `/healthz`, Swagger, OPTIONS

**AuthzGlobalFilter:**

- Validates Bearer token via `JwtDecoder`
- Adds `X-User-Sub` header (downstream still validates token independently)
- Full RBAC delegation to core authz endpoint is stubbed for future Redis/OPA integration

## nexus-initializer

Extends core bootstrap (`CoreInitializerService`):

1. **Flyway** — runs both locations:
   - `classpath:db/migration/oss` (core schema)
   - `classpath:db/migration/enterprise` (nexus tables)
2. **Encryption key** — same derivation as core initializer
3. **Config generation** — writes `nexus-services-application.yaml` from template

Entry point: `NexusBootstrapApplication` → `NexusInitializerService.performEnterpriseInitialization()`.

## Enterprise Database Additions

See [architecture data sections](./data-flow.md#enterprise-schema) for table detail. Enterprise Flyway scripts live in `libs/nexus-persistence/src/main/resources/db/migration/enterprise/`.

Key additions:

| Table | Purpose |
|---|---|
| `nexus_permissions` | Enterprise permission catalog (action, scope) |
| `nexus_roles` | Custom roles with scope_type / scope_id |
| `nexus_role_permissions` | Many-to-many role ↔ permission |
| `nexus_project` | Tenant projects (JPA entity present; DDL may lag entity) |

Enterprise migrations reuse core enums (`role_scope`) and extensions.

## Deployment Topology (Enterprise)

Typical Nexus stack:

```
Client → api-gateway:8080 → nexus-services:8081 → PostgreSQL
                          ↘ orchestrator:9090 (gRPC)
                            ↘ Kafka (optional)
                            ↘ Redis (authz cache)
                            ↘ Keycloak / OIDC IdP
```

Orchestrator and console-proxy remain core services; only the API front-end swaps from `core-services` to `nexus-services`.

## Extension Guidelines for Enterprise

1. **Add REST endpoints** — define OpenAPI in `nexus-api`, implement controller in `nexus-services`
2. **Add permissions** — extend `nexus-auth.Permission`, seed rows in `nexus_permissions`
3. **Override behavior** — place `@Primary` `@Service` in `com.yorel.muxon.ent` and ensure component scan includes `ent`
4. **Add schema** — new Flyway script under `db/migration/enterprise`, entity in `nexus-persistence`
5. **Replace infrastructure** — provide `@Bean` + `@ConditionalOnMissingBean` or use existing Kafka auto-config properties

Do **not** modify core controllers for enterprise features; keep the core API stable and additive.
