# Capability-Based Storage Architecture

## Overview

The capability-based storage architecture provides an intelligent, intent-driven approach to storage management in Muxon. Instead of manually mapping storage classes to provider-specific backends, administrators define storage requirements using generic capabilities, and the system automatically selects the best available storage.

## Key Concepts

### Storage Classes (Intent)

Storage classes define **what users want** using generic capabilities:

```yaml
name: fast-ssd
capabilities:
  performance: high      # Performance tier
  media: ssd            # Storage media type
  shared: true          # Multi-node access
  redundancy: replicated # Data protection
constraints:
  min_iops: 10000       # Minimum IOPS requirement
  max_latency_ms: 2     # Maximum latency tolerance
```

### Provider Storage (Reality)

Provider storage represents **what exists** - discovered storage pools/classes from infrastructure providers, normalized into a common model:

```json
{
  "name": "ssd_pool",
  "provider_type": "libvirt",
  "storage_type": "rbd",
  "capabilities": {
    "performance": "high",
    "media": "ssd",
    "shared": true,
    "redundancy": "replicated"
  },
  "metrics": {
    "free_gb": 500,
    "total_gb": 1000,
    "estimated_iops": 50000
  }
}
```

### Capability Mappings (Translation)

Capability mappings translate between Muxon's generic terms and provider-specific terminology:

| Muxon Capability | Libvirt Equivalent | Proxmox Equivalent |
|-------------------|-------------------|-------------------|
| performance: high | pool_type: [rbd, nvme] | storage_type: [rbd, zfspool] |
| media: ssd | pool_type: [rbd, lvm-thin] | storage_type: [rbd, zfspool, lvmthin] |
| shared: true | pool_type: [rbd, nfs] | storage_type: [rbd, nfs, cifs] |

### Storage Scheduler (Intelligence)

The scheduler implements the workflow: **Filter → Score → Select**

1. **Filter**: Eliminate storage that doesn't match capabilities/constraints
2. **Score**: Rank candidates using scoring algorithm
3. **Select**: Choose highest scoring storage

**OSS Scoring Formula**:
```
score = 0.4 × normalized_iops + 0.4 × free_capacity_ratio - 0.2 × latency_penalty
```

### Storage Overrides (Manual Control)

Overrides allow administrators to bypass the scheduler and manually map storage classes to specific provider storage:

```json
{
  "storage_class_name": "fast-ssd",
  "provider_type": "libvirt",
  "provider_storage_names": ["ssd_pool", "nvme_pool"],
  "priority": 100
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     User Request                             │
│              (storage_class: "fast-ssd", 100GB)             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Storage Scheduler Service                       │
│  1. Check Overrides                                         │
│  2. Filter by Capabilities (performance, media, etc.)       │
│  3. Filter by Constraints (min_iops, capacity)              │
│  4. Score Candidates (IOPS, capacity, latency)              │
│  5. Select Best                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Provider Storage (Normalized)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Libvirt Pool │  │ Proxmox Stor │  │ K8s CSI      │     │
│  │ ssd_pool     │  │ ceph-ssd     │  │ fast-ssd     │     │
│  │ rbd          │  │ rbd          │  │ csi-rbd      │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema

### storage_classes
```sql
CREATE TABLE storage_classes (
  name VARCHAR(64) PRIMARY KEY,
  type VARCHAR(32) NOT NULL,           -- block, object, file
  tier VARCHAR(32) NOT NULL,           -- performance, balanced, capacity
  capabilities JSONB,                  -- Generic capabilities
  constraints JSONB,                   -- Requirements (min_iops, etc.)
  description TEXT,
  -- Legacy fields (kept for backward compatibility)
  features JSONB,
  qos JSONB,
  allowed_providers JSONB
);
```

### provider_storage
```sql
CREATE TABLE provider_storage (
  id UUID PRIMARY KEY,
  provider_id UUID NOT NULL,
  provider_type VARCHAR(32) NOT NULL,  -- libvirt, proxmox
  external_id VARCHAR(255) NOT NULL,   -- Pool name, storage ID
  name VARCHAR(255) NOT NULL,
  storage_type VARCHAR(64) NOT NULL,   -- rbd, lvm, zfs, etc.
  capabilities JSONB NOT NULL,         -- Normalized capabilities
  metrics JSONB NOT NULL,              -- free_gb, total_gb, iops
  node_id VARCHAR(255),
  datacenter_id UUID,
  enabled BOOLEAN DEFAULT true,
  synced_at TIMESTAMPTZ
);
```

### storage_capability_mappings
```sql
CREATE TABLE storage_capability_mappings (
  id UUID PRIMARY KEY,
  muxon_capability VARCHAR(64) NOT NULL,
  provider_type VARCHAR(32) NOT NULL,
  provider_capability VARCHAR(64) NOT NULL,
  value_mapping JSONB                  -- Translation map
);
```

### storage_overrides
```sql
CREATE TABLE storage_overrides (
  id UUID PRIMARY KEY,
  storage_class_name VARCHAR(64) NOT NULL,
  provider_type VARCHAR(32) NOT NULL,
  provider_storage_names TEXT[] NOT NULL,  -- Array of storage names
  priority INTEGER DEFAULT 100
);
```

## API Endpoints

### Provider Storage Discovery

```bash
# Sync storage for a provider
POST /api/v1/provider-storage/provider/{providerId}/sync

# Sync all providers
POST /api/v1/provider-storage/sync-all

# Get provider storage
GET /api/v1/provider-storage/provider/{providerId}
GET /api/v1/provider-storage/provider/{providerId}/enabled
```

### Storage Overrides

```bash
# List all overrides
GET /api/v1/storage-overrides

# Get overrides for storage class
GET /api/v1/storage-overrides/storage-class/{name}

# Create override
POST /api/v1/storage-overrides
{
  "storageClassName": "fast-ssd",
  "providerType": "libvirt",
  "providerStorageNames": ["ssd_pool", "nvme_pool"],
  "priority": 100
}

# Update override
PUT /api/v1/storage-overrides/{id}

# Delete override
DELETE /api/v1/storage-overrides/{id}
```

## Usage Examples

### 1. Create Storage Class with Capabilities

```java
StorageClassEntity storageClass = new StorageClassEntity();
storageClass.setName("fast-ssd");
storageClass.setType("block");
storageClass.setTier("performance");

Map<String, Object> capabilities = Map.of(
    "performance", "high",
    "media", "ssd",
    "shared", true,
    "redundancy", "replicated"
);
storageClass.setCapabilities(capabilities);

Map<String, Object> constraints = Map.of(
    "min_iops", 10000,
    "max_latency_ms", 2
);
storageClass.setConstraints(constraints);

storageClassRepository.save(storageClass);
```

### 2. Discover Provider Storage

```java
// Trigger storage discovery for a provider
int count = providerStorageDiscoveryService.discoverAndSyncStorage(providerId);
System.out.println("Discovered " + count + " storage entries");

// Get discovered storage
List<ProviderStorageEntity> storage = 
    providerStorageRepository.findByProviderIdAndEnabled(providerId, true);
```

### 3. Schedule Storage for Volume

```java
// Schedule storage using the scheduler
SchedulingResult result = storageSchedulerService.scheduleStorage(
    "fast-ssd",  // Storage class name
    providerId,  // Provider ID
    100L * 1024 * 1024 * 1024  // 100GB in bytes
);

if (result.isSuccess()) {
    ProviderStorageEntity selected = result.getSelectedStorage();
    Map<String, Object> metadata = result.getMetadata();
    
    System.out.println("Selected: " + selected.getName());
    System.out.println("Score: " + metadata.get("score"));
    System.out.println("Type: " + selected.getStorageType());
}
```

### 4. Create Storage Override

```java
StorageOverrideEntity override = new StorageOverrideEntity();
override.setStorageClassName("fast-ssd");
override.setProviderType("libvirt");
override.setProviderStorageNames(List.of("ssd_pool", "nvme_pool"));
override.setPriority(100);

storageOverrideRepository.save(override);
```

## Supported Storage Types

### Libvirt
- **rbd** (Ceph RBD) - High performance, shared, replicated
- **lvm** (LVM) - Local, good performance
- **lvm-thin** (LVM Thin) - Local, thin provisioning
- **zfs** (ZFS) - Local, snapshots, compression
- **dir** (Directory) - Simple file-based
- **nfs** (NFS) - Shared network storage
- **nvme** (NVMe) - Ultra-high performance

### Proxmox
- **rbd** (Ceph RBD) - High performance, shared, replicated
- **zfspool** (ZFS) - Snapshots, compression
- **lvmthin** (LVM Thin) - Thin provisioning
- **lvm** (LVM) - Local storage
- **dir** (Directory) - Simple file-based
- **nfs** (NFS) - Shared network storage
- **cifs** (CIFS/SMB) - Windows network storage

## Migration from Old Model

The old `provider_storage_mappings` table has been deprecated but preserved for backward compatibility:

```sql
-- Old table renamed
provider_storage_mappings → provider_storage_mappings_deprecated
```

To migrate:

1. **Run Migration**: Apply V17 migration to create new tables
2. **Sync Providers**: Trigger storage discovery for all providers
3. **Create Overrides**: Manually create overrides for critical mappings
4. **Test**: Verify volume provisioning works with new model
5. **Switch**: Enable capability-based model via feature flag
6. **Cleanup**: Remove deprecated table after validation

## Configuration

```yaml
# application.yml
storage:
  capability-based:
    enabled: true  # Enable new model (default: false for backward compatibility)
  discovery:
    auto-sync-on-registration: true  # Auto-discover when provider registered
    sync-interval-hours: 24  # Periodic sync interval
  scheduler:
    scoring:
      weight-iops: 0.4
      weight-capacity: 0.4
      weight-latency: 0.2
```

## Monitoring

Key metrics to monitor:

- **Discovery Success Rate**: Percentage of successful storage discoveries
- **Scheduler Selection Time**: Time to select storage
- **Override Usage**: Percentage of requests using overrides vs scheduler
- **Capacity Utilization**: Free capacity across provider storage
- **IOPS Distribution**: IOPS allocation across storage types

## Troubleshooting

### No storage matches capabilities

**Problem**: Scheduler returns "No storage matches capabilities"

**Solutions**:
1. Check storage class capabilities are realistic
2. Verify provider storage has been discovered (sync)
3. Check capability mappings are correct
4. Review constraints (min_iops may be too high)

### Discovery fails

**Problem**: Storage discovery fails for provider

**Solutions**:
1. Verify provider connection info is correct
2. Check provider API is accessible
3. Review discovery service logs
4. Ensure provider has storage configured

### Override not working

**Problem**: Override is ignored by scheduler

**Solutions**:
1. Verify provider_storage_names match discovered storage names
2. Check provider_type matches provider
3. Ensure storage names exist in provider_storage table
4. Verify override is not disabled

## Future Enhancements

### Enterprise Features (Planned)
- **Advanced Scheduler**: Multi-factor scoring with cost, locality, failure domains
- **Historical Performance**: Use past metrics instead of estimates
- **Tiered Storage**: Automatic migration between hot/warm/cold tiers
- **Storage Aggregation**: Combine multiple pools for higher throughput
- **Cost Optimization**: Select cheapest storage meeting requirements
- **Quota Management**: Per-workspace storage quotas

### UI Improvements (Planned)
- Visual capability editor
- Storage topology visualization
- Capacity planning dashboard
- Performance analytics
- Migration wizards

## References

- Design Document: `/plans/unified-storage/muxon_storage_design_mar25.md`
- Implementation Status: `/plans/unified-storage/capability-based-implementation-status.md`
- Migration Plan: `/.windsurf/plans/capability-based-storage-migration-1ff7a0.md`
