# VM Workloads — Tenant Workflow (KVM Infrastructure & Stacks)

How operators register **KVM infrastructure**, prepare **catalog items**, how **tenants deploy VMs in Stacks**, and how **VM networking** works on the fabric underlay.

Based on `openspec/specs/infrastructure`, `content-library`, `vm-orchestration`, `multi-tenancy`, and `openspec/changes/networking-infrastructure/`.

## Eraser diagram

| Link | URL |
|------|-----|
| **Workspace** | https://app.eraser.io/workspace/gKrqip06PHJSE70oGeXN |
| **Diagram (canvas)** | https://app.eraser.io/workspace/gKrqip06PHJSE70oGeXN?diagram=2F9vQBQoTewG-wuYiRca&layout=canvas |
| **PNG export** | https://storage.googleapis.com/second-petal-295822.appspot.com/elements/autoDiagram%3Af7cc57a1ce864b7a219b738f83d3813c4d8716275c307d06875272f03d368844.png |

## Four phases (single diagram)

### 1. Add infrastructure — KVM nodes

- Register **Provider** (`LIBVIRT` or `PROXMOX`) with endpoint and credentials.
- Create **node_cluster** → **datacenter** → **nodes** (KVM hosts, status `READY`).
- Optional **ProviderInventorySync** from hypervisor API.
- Operator sets up **fabric_network** roles (TENANT_OVERLAY, STORAGE, MANAGEMENT, LIVE_MIGRATION).
- Create **tenant**, **tenant_datacenter_grant**, and **TenantNetworkPolicy**.

### 2. Add catalog items

**Content library (templates / ISOs):**

- `content_storage` → `content_library` → upload `content_items` (qcow2 templates, ISOs, cloud-init).
- Publish to datacenter: `CONTENT_LIBRARY_SYNC` → files on hypervisor datastores.

**Compute & storage catalog:**

- **compute_profiles** per grant (vCPU/RAM/disk sizing).
- **storage_classes** + **provider_storage** discovery.

**Networking catalog (tenant admin):**

- Tenant-global **VPC**, datacenter **subnet**, **security_groups**, **subnet_rbac**.

### 3. Tenant deploys VMs in Stack

1. Create **Stack** (`POST .../stacks`).
2. Pick template, compute profile, subnet, security groups from catalog.
3. `POST .../vms` with `stackId`, `content_item_id`, `subnetId`, optional customization.
4. **core-services**: validate grant, `subnet_rbac` ATTACH, `vm:create` → INSERT `PENDING` → gRPC `createVM`.
5. **orchestrator** → `VM_CREATE_COMMAND` → **VmTaskExecutor** → **LibvirtVmProvider** → KVM node.
6. **EntityEventProcessor** sets VM `ACTIVE`; VMs listed under stack.

VMs use the **infrastructure path only** (`kind: vm` → `TenantAwareVmProviderRegistry`). No plugin involved.

### 4. VM networking on KVM

- VMs attach vNICs to **subnet** (tenant-visible); underlay is **fabric_network** (operator-managed).
- **security_groups** enforced at vNIC (nftables/OVS via `NetworkProvider`).
- East-west: VMs in same subnet over TENANT_OVERLAY fabric.
- North-south: **floating_ip** from **public_ip_pool** via **network_edge_node** NAT.
- STORAGE and LIVE_MIGRATION use separate fabric roles — isolated from tenant overlay.

## VM create flow (numbered)

1. Tenant submits VM create with stack + subnet + template.
2. API persists VM `PENDING`, calls orchestrator.
3. Job + command enqueued.
4. Worker selects READY node, builds libvirt XML from subnet + SG.
5. Hypervisor creates VM + disks from content library path.
6. Entity events → VM `ACTIVE` in stack.

## Related

- [kubernetes-plugin-tenant-workflow.md](./kubernetes-plugin-tenant-workflow.md) — K8s plugin variant (pods on node VMs)
- [system-architecture-diagram.md](./system-architecture-diagram.md) — full platform diagram
