# VM Orchestration Flow (Current Design)

This document describes the existing VM orchestration flow from the API layer through the database queue to the orchestrator and providers, including status transitions and error handling.

## Overview

Long-running VM operations (create, start, stop, restart, suspend, resume, delete) are decoupled from the API: the API enqueues a **command** into a queue, and a separate **orchestrator** service polls the queue and executes commands by invoking **VM providers**. For the OSS version, the queue is implemented as a PostgreSQL table (`queue_entry`).

## Components

| Component | Location | Role |
|-----------|----------|------|
| **VmsService** | `services/core-services/.../VmsService.java` | API-facing service; persists VM entity and enqueues command |
| **QueueEntry** | `libs/core-persistence/.../QueueEntry.java` | JPA entity for queue row |
| **QueueEntryRepository** | `libs/core-persistence/.../QueueEntryRepository.java` | DB access for poll, markCompleted, markFailed |
| **VmOrchestrator** | `services/orchestrator/.../VmOrchestrator.java` | Scheduled poller; dispatches by queueType and calls providers |
| **VmProviderRegistry** / **VmProvider** | `libs/core-provider/...` | SPI to backend (e.g. Libvirt, Mock) |

## End-to-End Flow

### 1. Producer (API → Queue)

1. **API** receives a VM operation (e.g. create VM, start VM).
2. **VmsService**:
   - **Create VM**: Validates tenant datacenter grant, creates `VmEntity` with status `PENDING`, saves it, then builds a **command** `QueueEntry` via `buildCreateCommand(vm, request)` and saves it with `queueEntryRepository.save(commandEntry)`.
   - **Other operations** (start, stop, restart, suspend, resume, delete): Loads `VmEntity`, validates state, then builds a command via `buildStartCommand(vm)` (or analogous) and saves with `queueEntryRepository.save(commandEntry)`.
3. **Command payload**:
   - **queueType**: e.g. `VM_CREATE_COMMAND`, `VM_START_COMMAND`, `VM_STOP_COMMAND`, etc.
   - **entityType**: `EntityType.VM`
   - **entityId**: VM UUID
   - **queueCategory**: `COMMAND`
   - **status**: `PENDING`
   - **payload**: For create: spec, tenantDatacenterGrantId, name, metadata, tags; for others: `{ "vmId": "<uuid>" }`
   - **metadata**: `source=api`, `requestId=<generated>`
   - **source**: `core-services`
   - **actorType**: `USER`, **actorUserId** from security context, **actorService**: `api`

No use of `QueueProducer`; `VmsService` uses `QueueEntryRepository` and `QueueEntry` directly.

### 2. Queue (Database)

- Rows live in table **queue_entry** (see `V4__vm_tables.sql`).
- **Polling index**: `idx_queue_polling` on `(status, created_at) WHERE status='PENDING'`.
- **QueueStatus** lifecycle in current code: entries are inserted as `PENDING`. The orchestrator does **not** set `PROCESSING`; it only calls `markCompleted` or `markFailed` (which set `COMPLETED` or `FAILED` and `processedAt`).

### 3. Consumer (Orchestrator)

1. **VmOrchestrator.pollVmQueue()** runs every **5 seconds** (`@Scheduled(fixedDelay = 5000)`).
2. It calls `queueEntryRepository.poll(EntityType.VM, null, POLL_BATCH_SIZE)`, then `.stream().limit(POLL_BATCH_SIZE).toList()`. The repository `poll` JPQL ignores `queueType` and `limit`; limiting is done in Java.
3. For each **QueueEntry**, **processQueueEntry(entry)** runs:
   - **Switch** on `entry.getQueueType()`:
     - `VM_CREATE_COMMAND` → **processVmCreateCommand**
     - `VM_START_COMMAND` → **processVmStartCommand**
     - `VM_STOP_COMMAND` → **processVmStopCommand**
     - `VM_RESTART_COMMAND` → **processVmRestartCommand**
     - `VM_SUSPEND_COMMAND` → **processVmSuspendCommand**
     - `VM_RESUME_COMMAND` → **processVmResumeCommand**
     - `VM_DELETE_COMMAND` → **processVmDeleteCommand**
     - **default** → **markFailed**(entry, "Unknown queue type")
4. On any **exception** in the switch: **markFailed**(entry, e.getMessage()).

Orchestrator does **not** use `QueueConsumer`; it uses `QueueEntryRepository` directly and never sets status to `PROCESSING`.

### 4. Command Handlers – Status Transitions and Error Handling

#### VM_CREATE_COMMAND (processVmCreateCommand)

- Load **VmEntity** by `entry.getEntityId()`; throw if not found.
- **VmEntity status**: `PENDING` → **PLANNED** (save); emit status event `VM_STATUS_CHANGED` (new QueueEntry, STATUS category).
- Resolve **VmProvider** via `providerRegistry.getProviderForTenantDatacenter(vm.getTenantDatacenterGrantId())`; throw if absent.
- **VmEntity status**: **PLANNED** → **PROVISIONING** (set providerId from provider, save); emit status event.
- Build **VmCreationRequest** (spec from VM entity or entry payload, correlationId from metadata.requestId or entry.id).
- **provider.createVm(createRequest).join()** (blocks until completion).
- **Success**: VM → `ACTIVE`, powerState `ON`, set externalId, startedAt, ipAddresses, hostname from result; save; emit `VM_STATUS_CHANGED`; **markCompleted(entry)**.
- **Failure**: VM → `ERROR`; save; **markFailed(entry, errorMessage)**.

#### VM_START_COMMAND (processVmStartCommand)

- Load VM; validate status is `STOPPED` or `SUSPENDED`; else **markFailed**.
- VM → `ACTIVE`, set startedAt/updatedAt; save; emit `VM_STATUS_CHANGED`; **markCompleted(entry)**.

#### VM_STOP_COMMAND (processVmStopCommand)

- Load VM; validate status is `ACTIVE`; else **markFailed**.
- VM → `STOPPED`, powerState `OFF`, stoppedAt, updatedAt; save; emit `VM_STATUS_CHANGED`; **markCompleted(entry)**.

#### VM_RESTART_COMMAND (processVmRestartCommand)

- Load VM; validate status is `ACTIVE`; else **markFailed**.
- Emit operation event `VM_OPERATION_COMPLETED`; **markCompleted(entry)** (no VM state change in this handler).

#### VM_SUSPEND_COMMAND (processVmSuspendCommand)

- Load VM; validate status is `ACTIVE`; else **markFailed**.
- VM → `SUSPENDED`, powerState `SUSPENDED`; save; emit `VM_STATUS_CHANGED`; **markCompleted(entry)**.

#### VM_RESUME_COMMAND (processVmResumeCommand)

- Load VM; validate status is `SUSPENDED`; else **markFailed**.
- VM → `ACTIVE`, powerState `ON`; save; emit `VM_STATUS_CHANGED`; **markCompleted(entry)**.

#### VM_DELETE_COMMAND (processVmDeleteCommand)

- Load VM; set status → `DELETING`; save; emit `VM_STATUS_CHANGED`; **markCompleted(entry)**.

### 5. Status and Operation Events

The orchestrator writes **status** and **operation** events as new **QueueEntry** rows (category **STATUS**, queueType e.g. `VM_STATUS_CHANGED`, `VM_OPERATION_COMPLETED`). These are stored in the same `queue_entry` table with `status=PENDING`. The orchestrator does not consume them; they are for audit or downstream consumers.

### 6. Stalled Entry Detection

- **VmOrchestrator.checkStalledEntries()** runs every **60 seconds**.
- Calls `queueEntryRepository.getStalledEntries(cutoff)` (cutoff = now − 10 minutes).
- Only **logs** a warning; it does **not** reset or requeue entries. Stalled commands remain `PENDING` indefinitely unless manually handled.

## Summary of Design Notes

- **Tight coupling**: Both VmsService and VmOrchestrator use `QueueEntry` and `QueueEntryRepository` directly; no transport abstraction.
- **Consumer semantics**: No `PROCESSING` transition; no use of `QueueConsumer` (which implements proper claim and stall reset).
- **Scaling**: Poll ignores limit in SQL; long-running `createVm` blocks the scheduler thread via `.join()`.
- **Reliability**: Stalled entries are only detected and logged; no automatic retry or reset.

This document is the baseline for the orchestration flow; the queue decoupling plan introduces a transport-agnostic port and adapters so that the same flow can run over a DB queue (OSS) or an external message bus (e.g. Redpanda) in enterprise.

---

## Queue backend configuration (after decoupling)

The open-source distribution uses the database-backed queue only via the transport-agnostic `CommandQueue` and `EventPublisher` interfaces:

- **DB (default, OSS)**: `muxon.queue.backend=db` or unset. Uses the `queue_entry` table and `DbCommandQueue` / `DbEventPublisher`.

Alternative backends (such as message buses) are provided by enterprise extensions (for example, muxon-nexus) and are not part of this repository.
