# Capability Model

This document describes how backend capabilities are modeled and exposed
through the `/api/v1/info` endpoint.

## Providers

- **Core** (infron-core): `CoreCapabilityProvider` supplies core capabilities and modules.
- **Enterprise** (infron-nexus): `EnterpriseCapabilityProvider` supplies enterprise capabilities and modules (separate methods).
- **License**: `LicensedCapabilityProvider` supplies capabilities from the configured license.
- **Plugins**: Provision for plugins to expose their own capabilities can be added later.

## Core principles

- Capabilities are **strings** (e.g. `identity.rbac`, `compute.vm.snapshot`).
- Capabilities represent **advanced, optional, or gated features**.
- **Basic CRUD operations are implicit** in modules and MUST NOT be modeled
  as capabilities.

## Modules vs capabilities

- A **module** represents a concrete subsystem or integration such as:
  - `compute.kvm`, `compute.vsphere`
  - `storage.ceph`
  - `network.ovs`
  - `identity.ldap`
- Basic behavior provided by a module (for example, VM create/edit/delete
  for compute modules) is assumed to exist whenever that module is present.
  The UI and other services MUST NOT look for capabilities like:
  - `compute.vm.create`
  - `compute.vm.edit`
  - `compute.vm.delete`
  - `compute.vm.import`

Capabilities are reserved for **non-basic** features such as:

- `compute.vm.snapshot`
- `compute.vm.live_migrate`
- `compute.vm.clone`
- `network.vlan.manage`
- `network.security_groups`
- `cluster.ha`
- `cluster.multi_region`
- `identity.rbac`
- `identity.sso`
- `backup.create`, `backup.schedule`, `backup.restore`

## Naming conventions

- Use **namespaces** separated by dots:
  - `compute.*`
  - `network.*`
  - `storage.*`
  - `identity.*`
  - `cluster.*`
  - `backup.*`
  - `monitoring.*`
- Capability names should be:
  - Descriptive of the feature, not the transport or implementation.
  - Stable over time to avoid breaking UI or configuration.

## Validation rules

When adding new capabilities:

- Do **NOT** introduce generic CRUD-style capabilities such as:
  - `*.create`
  - `*.update`
  - `*.edit`
  - `*.delete`
  - `*.import`
- Instead, model the higher-level feature that is actually gated, e.g.:
  - `backup.create` instead of `backup.job.create`
  - `compute.vm.snapshot` instead of `snapshot.create`

If a change requires gating a basic operation (for example, disabling VM
create entirely), prefer modeling it as:

- A module configuration flag; or
- A more specific capability that reflects the feature intent, not the
  low-level CRUD verb.

