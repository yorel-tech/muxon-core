# Kubernetes Plugin — Tenant Workflow (K8s on KVM)

Conceptual workflow for how the **kubernetes** plugin deploys a managed cluster, how tenants use it, how **pods** join a **Stack** alongside **VMs** and **containers**, and how **networking** connects workloads on KVM.

Based on `openspec/changes/plugin-extension-framework/` and `openspec/changes/networking-infrastructure/`.

## Eraser diagram

| Link | URL |
|------|-----|
| **Workspace** | https://app.eraser.io/workspace/xzVYBSY5zG47WAy2z6OH |
| **Diagram (canvas)** | https://app.eraser.io/workspace/xzVYBSY5zG47WAy2z6OH?diagram=Fy0uEDgDGtNoAWTO3d3q&layout=canvas |
| **PNG export** | https://storage.googleapis.com/second-petal-295822.appspot.com/elements/autoDiagram%3A52f9efc2e6fbff53e5a63f04978929140531b695d616c96e0957223e5bec6ce7.png |

## Four phases (single diagram)

### 1. Deploy Kubernetes managed service

- System admin activates the **kubernetes** plugin (`ACTIVE`, published to tenant).
- Plugin registers resource kinds: `pod`, `deployment`.
- Catalog contribution: `service-offering` **kubernetes-cluster**.
- Plugin runs out-of-process (gRPC + mTLS); no direct DB access.

### 2. Tenant provisions a cluster

- Tenant admin orders **kubernetes-cluster** from catalog (`ValidateCatalogConfig` → `ProvisionCatalogItem`).
- **K8s nodes are real VMs**: plugin drives provisioning through the normal Muxon VM path (`orchestrator` → `VmTaskExecutor` → **LibvirtVmProvider** → **KVM**), not by calling the hypervisor directly.
- Disks land on provider storage (Ceph/ZFS/NFS). Service instance becomes `READY`; kubeconfig in UI.

### 3. Tenant use — mixed Stack

- Stack `production-app` groups workloads:
  - `kind: vm` → **VmProvider** path (KVM)
  - `kind: container` → runtime path
  - `kind: pod` → **PluginResourceDispatcher** → kubernetes plugin `ReconcileResource`
- Stack engine uses **two independent dispatch paths**; `vm` is reserved and never handled by plugins.
- `subnet_rbac`: stack needs `ATTACH` on a subnet before workloads attach.

### 4. Networking (VM ↔ pod on KVM)

- **VPC** (tenant-global) → **Subnet** (datacenter-scoped) → **fabric_network** underlay on KVM hosts.
- VM `web-01` and K8s **worker node VM** both have vNICs on the same subnet.
- **ipam_prefix_delegation**: e.g. `10.0.2.0/24` delegated to worker node VM for CNI (Cilium/Calico).
- Route table: delegated prefix → node NIC IP so VMs can reach pod IPs.
- Control plane (Muxon API/plugin gRPC) is management-only; data plane is KVM + fabric + CNI.

## Quick reference

| Workload | Stack `kind` | Dispatch | Runs on |
|----------|--------------|----------|---------|
| VM | `vm` | `TenantAwareVmProviderRegistry` | KVM hypervisor |
| Container | `container` | Runtime plugin / VM | KVM / VM |
| Pod | `pod` | Kubernetes plugin | K8s on node VMs (KVM) |

## Related

- [system-architecture-diagram.md](./system-architecture-diagram.md) — platform-wide diagram
- [eraser-architecture-prompt.md](./eraser-architecture-prompt.md) — main platform Eraser prompt
