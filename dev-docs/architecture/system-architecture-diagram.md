# Muxon System Architecture — End-to-End

High-level view from end user to VM storage on hypervisors. **Core (OSS)** uses sky blue `#0EA5E9`; **Enterprise (Nexus)** uses purple `#8B5CF6` (matches `muxon-web` themes).

## Eraser diagram (generated)

| Link | URL |
|------|-----|
| **Workspace** | https://app.eraser.io/workspace/Gr4Gy624K6NwJdSJbd6E |
| **Diagram (canvas)** | https://app.eraser.io/workspace/Gr4Gy624K6NwJdSJbd6E?diagram=YqxpV54zquN9Lkee93Fo&layout=canvas |
| **PNG export** | https://storage.googleapis.com/second-petal-295822.appspot.com/elements/autoDiagram%3Aed97777af0541e36345c2cbe19654dc0e6bf401a62ce2e6ee3d61abf250388df.png |

Open the workspace link to zoom, pan, and edit. Core components use blue; Nexus (enterprise) uses purple; physical data plane uses dark styling per the prompt in [eraser-architecture-prompt.md](./eraser-architecture-prompt.md).

## Architecture diagram (Mermaid)

```mermaid
flowchart LR
    subgraph Users["End users & clients"]
        EU[End User]
        UI[Web UI<br/>Core #0EA5E9 / Nexus #8B5CF6]
        API[API Clients]
    end

    subgraph NexusEdge["NEXUS — Enterprise edge"]
        GW[api-gateway :8080<br/>JWT CORS]
    end

    subgraph Identity["Identity"]
        IDP[OIDC IdP<br/>Keycloak]
    end

    subgraph APIPlane["API / control plane"]
        CS[core-services<br/>REST /api/v1]
        NS[nexus-services<br/>core + ent beans]
        NI[nexus-initializer]
        CI[muxon-initializer]
        REDIS[(Redis<br/>authz cache)]
    end

    subgraph Orch["CORE — Orchestration"]
        ORCH[orchestrator<br/>gRPC :9090]
        JOB[JobService]
        POLL[UnifiedTaskPoller]
        WRK[core-worker<br/>VmTaskExecutor]
    end

    subgraph Queue["Async transport"]
        DBQ[(PostgreSQL<br/>orchestrator_queue)]
        KAFKA[(Kafka<br/>optional NEXUS)]
    end

    subgraph Drivers["CORE — Infrastructure SPI"]
        REG[TenantAwareVmProviderRegistry]
        LV[LibvirtVmProvider]
        PX[ProxmoxVmProvider]
        NET[NetworkProvider SPI]
        STO[StorageProvider SPI]
    end

    subgraph Plugins["Planned — Plugin framework"]
        PR[Plugin Registry]
        PP[Plugin processes gRPC]
        PD[PluginResourceDispatcher]
    end

    subgraph Data["Platform state"]
        PG[(PostgreSQL<br/>entities jobs queue)]
    end

    subgraph Aux["CORE — Auxiliary"]
        CPX[console-proxy<br/>WebSocket]
    end

    subgraph Physical["Hypervisor & storage"]
        HV[Hypervisor nodes<br/>KVM / Proxmox]
        VMSTORE[VM disks & images<br/>provider pools]
        FAB[fabric_network<br/>VPC subnets SGs]
        EDGE[network edge nodes<br/>NAT floating IP]
        CLIB[Content library paths<br/>on datastores]
    end

    EU --> UI
    UI --> GW
    UI -.->|OSS direct| CS
    API --> GW
    API -.-> CS
    EU & API --> IDP
    IDP -.->|JWT| GW
    IDP -.-> CS

    GW --> NS
    NS --> PG
    CS --> PG
    NS --> REDIS
    CS --> ORCH
    NS --> ORCH
    CI --> PG
    NI --> PG

    ORCH --> JOB
    JOB --> DBQ
    JOB -.-> KAFKA
    POLL --> DBQ
    POLL -.-> KAFKA
    POLL --> WRK
    WRK --> REG
    REG --> LV & PX
    WRK --> NET & STO
    WRK --> DBQ
    WRK -.-> KAFKA

    LV & PX --> HV
    NET --> FAB & EDGE
    STO --> VMSTORE
    WRK --> CLIB
    CLIB --> HV
    HV --> VMSTORE

    WRK -.->|entity events| PG
    CS -->|EntityEventProcessor| PG
    CS --> CPX
    CPX --> HV

    ORCH --> PD
    PD --> PP
    PR --> PP
    PP -.->|gRPC reconcile| WRK

    classDef core fill:#E0F2FE,stroke:#0EA5E9,stroke-width:2px,color:#0F172A
    classDef nexus fill:#EDE9FE,stroke:#8B5CF6,stroke-width:2px,color:#2E1065
    classDef shared fill:#F1F5F9,stroke:#64748B,stroke-width:1px
    classDef physical fill:#1E293B,stroke:#334155,color:#F8FAFC

    class CS,ORCH,JOB,POLL,WRK,REG,LV,PX,NET,STO,CI,CPX core
    class GW,NS,NI,REDIS,KAFKA nexus
    class EU,UI,API,IDP,DBQ,PG,PR,PP,PD shared
    class HV,VMSTORE,FAB,EDGE,CLIB physical
```

## VM create sequence

```mermaid
sequenceDiagram
    autonumber
    participant U as End User
    participant G as api-gateway
    participant A as core/nexus-services
    participant O as orchestrator
    participant Q as CommandQueue
    participant W as Worker
    participant P as VmProvider
    participant H as Hypervisor
    participant DB as PostgreSQL

    U->>G: POST /vms + JWT
    G->>A: forward X-User-Sub
    A->>A: RBAC + INSERT vm PENDING
    A->>O: gRPC createVM
    O->>DB: INSERT job
    O->>Q: VM_CREATE_COMMAND
    A-->>U: 202 jobId

    W->>Q: pollCommands
    W->>P: createVm()
    P->>H: provision VM + disks
    P-->>W: VmCreationResult
    W->>Q: TASK_COMPLETED + VM_CREATED event
    O->>DB: job COMPLETED
    A->>DB: vm ACTIVE
```

## Component ownership

| Layer | Core (#0EA5E9) | Enterprise (#8B5CF6) | Shared |
|-------|-----------------|----------------------|--------|
| Edge | — | api-gateway | — |
| API | core-services | nexus-services (+ core JAR) | PostgreSQL |
| Orchestration | orchestrator, core-worker | Kafka queue override | — |
| Drivers | VmProvider, NetworkProvider, StorageProvider | — | Hypervisors |
| Plugins | registry, gRPC plugins (planned) | marketplace (future) | — |
| Authz | Caffeine + RoleRegistry | Redis + nexus_roles + OPA | OIDC IdP |
| Console | console-proxy | — | console_sessions |

## Storage path to VM

1. **API**: storage classes, provider_storage discovery, volume/snapshot entities in PostgreSQL.
2. **Scheduling**: `StorageSchedulerService` maps class → provider pool via capabilities.
3. **VM create**: worker attaches volumes / selects datastore via provider context.
4. **Runtime**: VM disks live on hypervisor storage pools (Ceph, ZFS, NFS, Proxmox datastore).
5. **Content**: ISO/templates distributed via content library → node path → attachIso at create.

## Related diagrams

- **[VM tenant workflow](./vm-tenant-workflow.md)** — KVM infra, catalog, Stack VM deploy, networking ([Eraser canvas](https://app.eraser.io/workspace/gKrqip06PHJSE70oGeXN?diagram=2F9vQBQoTewG-wuYiRca&layout=canvas))
- **[Kubernetes plugin tenant workflow](./kubernetes-plugin-tenant-workflow.md)** — K8s service deploy, mixed Stack (VM + pod), networking on KVM ([Eraser canvas](https://app.eraser.io/workspace/xzVYBSY5zG47WAy2z6OH?diagram=Fy0uEDgDGtNoAWTO3d3q&layout=canvas))

## Sources

- [architecture.md](./architecture.md)
- [data-flow.md](./data-flow.md)
- [core-system.md](./core-system.md)
- [enterprise-system.md](./enterprise-system.md)
- [system-map.md](system-map.md)
- `openspec/changes/plugin-extension-framework/design.md`
- `openspec/changes/networking-infrastructure/design.md`
