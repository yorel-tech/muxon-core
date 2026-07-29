# Eraser MCP — Muxon End-to-End Architecture Diagram

Use this prompt with Eraser MCP `generate` after authenticating (`mcp_auth` in Cursor, or `npx @eraserlabs/eraser-mcp login`).

## Recommended Eraser parameters

| Parameter | Value |
|-----------|-------|
| `resource` | `file` |
| `diagramType` | `cloud-architecture-diagram` |
| `direction` | `right` (or `down` if layout is too wide) |
| `theme` | `light` |
| `colorMode` | `bold` |
| `styleMode` | `shadow` |
| `typeface` | `clean` |
| `format` | `png` |
| `imageQuality` | `3` |
| `title` | `Muxon Platform — End-to-End Architecture (Core + Nexus)` |

## Color legend (match UI themes)

- **Core (OSS / Muxon)**: fill/stroke **#0EA5E9** (primary), accent **#0284C7**, light bg **#E0F2FE**
- **Enterprise (Nexus)**: fill/stroke **#8B5CF6** (primary), accent **#7C3AED**, light bg **#EDE9FE**
- **Shared / infrastructure**: neutral gray **#64748B**, storage **#0891B2**, network **#16A34A**
- **Hypervisor / physical**: dark **#1E293B**
- Label every Core box with a small “CORE” badge; every Enterprise-only box with “NEXUS”

---

## Generation prompt (paste as `text`)

Create a large, high-resolution cloud architecture diagram for the **Muxon private-cloud control plane** (muxon-core OSS + muxon-enterprise Nexus). Show **end-to-end flow from end users to VM storage on hypervisors**. Use **left-to-right** layout with clear numbered flow arrows (1→2→3…). Group components in labeled regions. Differentiate Core vs Enterprise with the colors above.

### Region 1 — End users & clients (neutral)

- **End User** (browser)
- **Web UI** (Muxon / Muxon SPA — Core sky-blue theme; Nexus purple theme when enterprise)
- **API Clients** (CLI, automation, Terraform-style integrators)
- Arrows: User → Web UI → HTTPS; API Clients → HTTPS

### Region 2 — Enterprise edge (NEXUS purple #8B5CF6 only)

- **api-gateway** (:8080) — JWT decode, audience `muxon-api`, CORS, forwards to nexus-services
- Note: OSS mode bypasses gateway — dashed arrow “OSS direct” from clients to core-services

### Region 3 — Identity (shared, neutral)

- **OIDC IdP** (e.g. Keycloak) — users authenticate here first
- JWT Bearer flows to API plane

### Region 4 — API / control plane

**CORE (#0EA5E9):**
- **core-services** — REST `/api/v1`, JWT validation, RBAC (`AuthorizationService` + Caffeine), entity CRUD, `EntityEventProcessor` (sole VM state writer from worker events), gRPC **client** to orchestrator
- **muxon-initializer** — Flyway OSS, OIDC bootstrap

**NEXUS (#8B5CF6):**
- **nexus-services** — wraps core-services JAR + enterprise beans (`@Primary` authz, projects API)
- **nexus-initializer** — Flyway oss + enterprise
- **Redis** — enterprise permission/tenant cache
- Satellite stubs (lighter purple): usage-billing, director, net-advanced, audit-export

Arrows:
- Gateway → nexus-services (enterprise)
- Clients → core-services (OSS dashed)
- Both → PostgreSQL (read/write entity tables)
- Both → gRPC :9090 → orchestrator

### Region 5 — Orchestration plane (CORE #0EA5E9)

- **orchestrator** — gRPC server (`VMWorkflowService`, `ContentLibraryWorkflowService`, `JobQueryService`)
- **JobService** — creates `jobs`, enqueues commands
- **TaskEventProcessor** — job status from worker
- **UnifiedTaskPoller** + **core-worker** (`TaskRouter`, `VmTaskExecutor`, `ContentTaskExecutor`)
- Note on poller: disabled when `muxon.enterprise.enabled=true` (enterprise horizontal workers)

### Region 6 — Async transport (shared label, two implementations)

- **Queue SPI**: `CommandQueue`, `TaskEventQueue`, `EntityEventQueue`
- **OSS path (CORE)**: PostgreSQL `orchestrator_queue` (categories: COMMAND, TASK_EVENT, ENTITY_EVENT)
- **Enterprise path (NEXUS)**: **Kafka** topics `muxon.vm.commands`, `muxon.task.events`, `muxon.entity.events` (optional; can fall back to DB)

Show bidirectional flows:
- orchestrator → CommandQueue → worker
- worker → TaskEventQueue → orchestrator
- worker → EntityEventQueue → core-services EntityEventProcessor

### Region 7 — Infrastructure drivers (CORE, in-process SPI — NOT plugins)

Label: **VmProvider SPI (hypervisor drivers)**

- **TenantAwareVmProviderRegistry** — resolves provider via `tenant_datacenter_grants` → `providers`
- **LibvirtVmProvider** → KVM/libvirt hypervisors
- **ProxmoxVmProvider** → Proxmox VE API
- **MockVmProvider** (dev)
- **NetworkProvider SPI** (Libvirt full; Proxmox/K8s stubs) — `fabric_network`, VPC, subnets, security groups on vNICs
- **StorageProvider / StorageDiscoveryProvider** — block/object discovery into `provider_storage`

Arrow: worker executors → VmProvider → hypervisor APIs (never from core-services directly)

### Region 8 — Plugin framework (planned, CORE admin API + out-of-process)

Use a distinct border style (dashed) and label **Phase: plugin-extension-framework**

- **Plugin registry** (DB: `plugins`, `resource_type_definitions`)
- **Plugin processes** (gRPC, mTLS) — managed services, non-VM resource kinds
- **PluginResourceDispatcher** in stack engine — routes plugin kinds; **`vm` kind reserved** for VmProvider only
- **Plugin SDK** — reconcile via `EntityEventQueue` (no direct DB)
- External-managed vs platform-managed (Docker/K8s execution targets)

Do NOT merge VmProvider into plugin framework.

### Region 9 — Data & platform state (shared)

**PostgreSQL** (single source of truth) with callouts for table groups:
- Tenancy: tenants, datacenters, tenant_datacenter_grants
- Infra inventory: providers, node_clusters, nodes
- Compute: vms, compute_profiles, console_sessions
- Storage: storage_classes, provider_storage, volumes, snapshots, buckets
- Content library: content_libraries, content_items, distributions
- Networking (planned): vpc, subnet, fabric_network, public_ip_pool, security_groups, network_edge_node
- Orchestration: jobs, task_steps, task_logs, orchestrator_queue
- Identity/RBAC: identity_providers, idp_users, role_bindings
- Enterprise tables (purple): nexus_project, nexus_roles, nexus_permissions, nexus_role_permissions

### Region 10 — Auxiliary services (CORE)

- **console-proxy** — WebSocket bridge; token from `console_sessions` → hypervisor VNC/SPICE
- Flow callout: POST console API → VM_CONSOLE_RESOLVE command → worker getConsoleConnection → session token → browser WS to console-proxy → hypervisor

### Region 11 — Hypervisor & physical infrastructure (dark #1E293B)

Group **Execution / data plane**:

- **Hypervisor nodes** (KVM hosts, Proxmox cluster)
- **VM disks & images** on provider storage pools / Ceph / local ZFS / NFS
- **Content library artifacts** copied to hypervisor datastores for ISO/templates
- **Network datapath**: tenant overlay (VXLAN/bridge), fabric networks (TENANT_OVERLAY, STORAGE, MANAGEMENT, LIVE_MIGRATION roles), **network edge nodes** (NAT, floating IP, BGP/FRR)
- **Block/object storage backends** behind StorageProvider

Arrows from VmProvider into this region: createVm, attach ISO, NIC on subnet, power ops.

### Region 12 — Domain resources (logical, connect API to physical)

Show how control-plane entities map to runtime:
- Tenant → Grant → Provider → Node → VM
- Storage class → provider_storage mapping → volume on hypervisor
- Content library item → distribution → path on node for VM create
- VPC (tenant-global) → subnet (datacenter-scoped) → fabric_network segment → vNIC

### Numbered end-to-end flows (draw as numbered green arrows)

**Flow A — VM create (primary):**
1. User POST `/api/v1/tenants/{tid}/vms` + JWT
2. api-gateway (enterprise) or direct core-services (OSS) — authz, INSERT vm PENDING
3. gRPC createVM → orchestrator → INSERT job → CommandQueue VM_CREATE_COMMAND
4. Worker poll → VmTaskExecutor → VmProvider.createVm()
5. Hypervisor provisions VM + disks
6. TaskEventQueue → job COMPLETED; EntityEventQueue VM_CREATED → EntityEventProcessor → vm ACTIVE

**Flow B — Content library sync:** API → gRPC content workflow → ContentTaskExecutor → provider paths on nodes

**Flow C — Console:** API permission → VM_CONSOLE_RESOLVE command → worker → console_sessions → console-proxy WS → hypervisor

**Flow D — Enterprise authz:** JWT → nexus AuthorizationService → Redis + nexus_role_permissions (+ optional OPA)

**Flow E — Plugin resource (dashed future):** stack item kind ≠ vm → PluginResourceDispatcher → gRPC ReconcileResource → EntityEventQueue

### Deployment mode callouts (corner legend)

| Mode | API entry | Queue | Authz cache | Worker |
|------|-----------|-------|-------------|--------|
| Core OSS | core-services direct | PostgreSQL | Caffeine | UnifiedTaskPoller in orchestrator |
| Nexus Enterprise | api-gateway → nexus-services | Kafka or PostgreSQL | Redis | Poller disabled; scaled workers |

### Design rules (footnote box)

- Intent vs execution: API owns desired state; workers report facts via events
- core-services NEVER calls VmProvider for VM lifecycle (gRPC only)
- EntityEventProcessor is sole VM state machine writer
- Enterprise does not fork core REST API — composition + @Primary overrides

Make the diagram dense but readable at high zoom. Every box should have a short subtitle (port, protocol, or table group). Use solid arrows for implemented paths and dashed for planned (plugins, advanced networking enterprise features).
