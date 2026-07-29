End-to-end flow (browser → DB) — example: create VM

1. Browser (client)

    - User clicks "Create VM" → client sends HTTPS request to POST /vms on your API domain with JWT.

2. API Gateway (edge)

    - Validate TLS, check rate limits, verify/parse JWT (signature, expiry).

    - Optionally perform a coarse permission check from Redis cache (e.g., user has vm:create permission at tenant scope) to reject obvious denies early.

    - Add context headers (e.g., x-user-id, x-tenant-id, trace/span ids).

    - Route request to appropriate core service (e.g., core-vm-service) over internal network.

3. Core Service (business controller)

    - Request arrives at core-vm-service controller method. In-process PermissionAspect intercepts method (reads @RequiresPermission("vm:create")).

    - AuthorizationServiceImpl runs:

        - fetch cached permissions (Redis/cache)

        - wildcard match

        - if ABAC enabled: evaluate OPA policy (local sidecar or central OPA) with input {user, resource, action}

        - apply precedence (OPA deny → deny; role grants + ABAC allow → allow; default deny)

    - If allowed, controller validates request body, calls service layer (domain logic), and persists via repository.

4. Domain / Persistence layer

    - Repositories write to DB (RDBMS, etc.). Transactions handled within service.

    - On permission/role changes, service evicts Redis cache keys or publishes invalidation.

5. Response & Observability

    - Core returns response to gateway; gateway sets extra headers if needed and forwards to client.

    - Logging, audit events (who did what), and metrics are recorded (central log/AuditService).



Roles of each module/service (concise)

api-gateway
    - Edge responsibilities only: TLS termination, JWT validation, rate limit, IP allowlist, routing, request/response shaping, CORS, circuit breakers, retries.

    - Optional: coarse permission checks using cached permissions (fast), blocking obvious denies.

    - Do not host domain controllers or AOP enforcement.

auth / identity service (AuthN)

    - Issues and verifies JWTs / sessions (OAuth2/OpenID Connect).

    - Manages users, groups, credentials.

    - Could be Keycloak / Okta / custom service.

core-services (domain services)

    - Business controllers and service layers (e.g., vm-service, project-service, tenant-service).

    - Host PermissionAspect, AuthorizationServiceImpl, ABAC/OPA integration, audit logging, transactional logic.

    - Access DBs, resource repositories.

    - Expose internal endpoint /internal/authz/check if gateway should call for authoritative decisions.

authz-api (shared library)

    - Shared interface and DTOs: AuthorizationService interface, RequiresPermission annotation, UserPrincipal, Scope.

    - Shared artifact used by gateway and core for compile-time compatibility.

policy engine (OPA)

    - Evaluate ABAC policies. Best placed near core (sidecar or central service).

    - Core is authoritative; OPA returns allow/deny for complex conditions.

Redis / cache

    - Cache computed permissions per user (permissions:user:{id}).

    - Use for fast gateway checks and to avoid frequent DB lookups.

    - Use pub/sub or an event bus for eviction notifications.

database(s)

    - Persistent data (users, roles, role_bindings, resources, domain data).

    - Owned by core services.

audit/logging/metrics

    - Central store for audit events and logs (ELK/Tempo/Prometheus).


Practical considerations & checklist

    - Keep DTOs/shared models in an internal artifact so you don’t duplicate types.

    - Secure internal calls: mTLS or internal-only networks and service tokens for /internal/authz/check.

    - Cache eviction: when role-binding changes, call @CacheEvict or publish invalidation event.

    - Audit: log allow/deny decisions with context (user, action, resource, reason).

    - Fail behavior: decide default on OPA/core unreachable — deny-by-default is safest.

    - Tests: unit tests for AuthorizationService, integration tests for aspect behavior, e2e tests for gateway→core flow.

    - Monitoring: latency dashboards for gateway and core, and OPA error rates.

    - Backwards compatibility: keep old gateway endpoints until migration is complete.


Options & when to use them

1. Kubernetes (recommended): no Eureka

Use K8s Service DNS names (e.g. http://core-vm-service.default.svc.cluster.local or simply http://core-vm-service inside the same namespace).

Gateway can call http://core-vm-service directly — K8s load-balances for you.

Pros: zero extra discovery service, simple, production-grade.

Cons: ties you to K8s DNS semantics (not a con if you’re on K8s).

2. Docker Compose / plain docker (light): no Eureka

Docker Compose provides service name DNS (e.g. http://core-vm-service:8081).

Gateway and services on same Docker network can use service names.

Pros: simple, no extra component.

3. Standalone VMs / mixed infra / advanced client-side LB: use Eureka (or Consul)

Use if you need dynamic discovery outside K8s and want client-side load balancing enabled by Spring Cloud load balancer.

Pros: dynamic registration, central registry if you run many instances across hosts.

Cons: extra service to run and maintain; extra latency for registry calls; operational cost.

4. Local IntelliJ dev

You can run everything on localhost with different ports and point gateway at http://localhost:8081. No discovery required.

Alternatively use hosts file or Docker network if running via Compose.


Security note (important)

When you accept forwarded headers (e.g., X-User-Id) from the gateway, do not blindly trust them unless gateway → core channel is secure/trusted. In production:
Use mTLS between gateway and core, or
use signed tokens (JWT) passed through, or
have gateway call internal authz and core still validate tokens.