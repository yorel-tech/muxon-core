-- Capability-Based Storage Architecture Migration
-- This migration introduces the new capability-based storage model
-- Includes provider_storage, storage_capability_mappings, and storage_overrides tables
-- Migrates existing storage_classes to use capabilities and constraints

-- ============================================================================
-- STEP 1: Modify storage_classes table to add capability-based fields
-- ============================================================================

ALTER TABLE storage_classes 
  ADD COLUMN capabilities JSONB,
  ADD COLUMN constraints JSONB,
  ADD COLUMN description TEXT;

-- Migrate existing data from features/qos to capabilities/constraints
UPDATE storage_classes SET
  capabilities = jsonb_build_object(
    'performance', CASE tier 
      WHEN 'performance' THEN 'high'
      WHEN 'balanced' THEN 'medium'
      WHEN 'capacity' THEN 'low'
      WHEN 'archive' THEN 'low'
    END,
    'media', CASE 
      WHEN name LIKE '%ssd%' OR name LIKE '%nvme%' THEN 'ssd'
      WHEN name LIKE '%hdd%' THEN 'hdd'
      ELSE 'any'
    END,
    'shared', CASE 
      WHEN features->>'replication' IS NOT NULL AND (features->>'replication')::int > 1 THEN true
      ELSE false
    END,
    'redundancy', CASE 
      WHEN features->>'replication' IS NOT NULL AND (features->>'replication')::int > 1 THEN 'replicated'
      ELSE 'none'
    END
  ),
  constraints = jsonb_build_object(
    'min_iops', COALESCE((qos->>'minIops')::int, 0),
    'max_latency_ms', 10
  ),
  description = CASE 
    WHEN tier = 'performance' THEN 'High-performance storage with low latency'
    WHEN tier = 'balanced' THEN 'Balanced storage for general workloads'
    WHEN tier = 'capacity' THEN 'High-capacity storage for large datasets'
    WHEN tier = 'archive' THEN 'Archive storage for long-term retention'
  END
WHERE capabilities IS NULL;

COMMENT ON COLUMN storage_classes.capabilities IS 'Storage capabilities (performance, media, shared, redundancy)';
COMMENT ON COLUMN storage_classes.constraints IS 'Storage constraints (min_iops, max_latency_ms, etc.)';

-- ============================================================================
-- STEP 2: Create provider_storage table
-- ============================================================================

CREATE TABLE provider_storage (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  provider_id UUID NOT NULL,
  provider_type VARCHAR(32) NOT NULL,
  external_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  storage_type VARCHAR(64) NOT NULL,
  capabilities JSONB NOT NULL,
  metrics JSONB NOT NULL,
  node_id VARCHAR(255),
  datacenter_id UUID,
  enabled BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  synced_at TIMESTAMPTZ,
  version BIGINT DEFAULT 0,
  
  CONSTRAINT uk_provider_external_id UNIQUE (provider_id, external_id)
);

CREATE INDEX idx_provider_storage_provider ON provider_storage(provider_id);
CREATE INDEX idx_provider_storage_type ON provider_storage(provider_type);
CREATE INDEX idx_provider_storage_datacenter ON provider_storage(datacenter_id);
CREATE INDEX idx_provider_storage_enabled ON provider_storage(enabled);
CREATE INDEX idx_provider_storage_capabilities ON provider_storage USING GIN (capabilities);
CREATE INDEX idx_provider_storage_name ON provider_storage(name);

CREATE TRIGGER trg_provider_storage_updated
BEFORE UPDATE ON provider_storage
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

COMMENT ON TABLE provider_storage IS 'Normalized provider storage pools/classes with capabilities and metrics';
COMMENT ON COLUMN provider_storage.external_id IS 'Provider-specific storage identifier (pool name, storage ID)';
COMMENT ON COLUMN provider_storage.storage_type IS 'Backend storage type (ceph-rbd, lvm, zfs, dir, nfs, etc.)';
COMMENT ON COLUMN provider_storage.capabilities IS 'Normalized storage capabilities';
COMMENT ON COLUMN provider_storage.metrics IS 'Current storage metrics (free_gb, total_gb, iops, etc.)';

-- ============================================================================
-- STEP 3: Create storage_capability_mappings table
-- ============================================================================

CREATE TABLE storage_capability_mappings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  infron_capability VARCHAR(64) NOT NULL,
  provider_type VARCHAR(32) NOT NULL,
  provider_capability VARCHAR(64) NOT NULL,
  value_mapping JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  
  CONSTRAINT uk_capability_provider UNIQUE (infron_capability, provider_type, provider_capability)
);

CREATE INDEX idx_scm_infron_capability ON storage_capability_mappings(infron_capability);
CREATE INDEX idx_scm_provider_type ON storage_capability_mappings(provider_type);

COMMENT ON TABLE storage_capability_mappings IS 'Maps Infron generic capabilities to provider-specific equivalents';
COMMENT ON COLUMN storage_capability_mappings.infron_capability IS 'Generic Infron capability name';
COMMENT ON COLUMN storage_capability_mappings.provider_capability IS 'Provider-specific capability name';
COMMENT ON COLUMN storage_capability_mappings.value_mapping IS 'Value translation map (e.g., high -> [rbd, nvme])';

-- Insert default capability mappings for Libvirt and Proxmox
INSERT INTO storage_capability_mappings (infron_capability, provider_type, provider_capability, value_mapping) VALUES
-- Performance mappings
('performance', 'libvirt', 'pool_type', '{"high": ["rbd", "nvme"], "medium": ["lvm-thin", "zfs"], "low": ["dir", "nfs"]}'),
('performance', 'proxmox', 'storage_type', '{"high": ["rbd", "zfspool"], "medium": ["lvmthin"], "low": ["dir", "nfs"]}'),
-- Media mappings
('media', 'libvirt', 'pool_type', '{"ssd": ["rbd", "lvm-thin"], "hdd": ["dir"], "nvme": ["nvme"], "any": ["*"]}'),
('media', 'proxmox', 'storage_type', '{"ssd": ["rbd", "zfspool", "lvmthin"], "hdd": ["dir"], "nvme": ["nvme"], "any": ["*"]}'),
-- Shared mappings
('shared', 'libvirt', 'pool_type', '{"true": ["rbd", "nfs"], "false": ["lvm", "zfs", "dir"]}'),
('shared', 'proxmox', 'storage_type', '{"true": ["rbd", "nfs", "cifs"], "false": ["lvmthin", "zfspool", "dir"]}'),
-- Redundancy mappings
('redundancy', 'libvirt', 'pool_type', '{"replicated": ["rbd"], "none": ["lvm", "zfs", "dir", "nfs"]}'),
('redundancy', 'proxmox', 'storage_type', '{"replicated": ["rbd"], "none": ["lvmthin", "zfspool", "dir", "nfs"]}');

-- ============================================================================
-- STEP 4: Create storage_overrides table
-- ============================================================================

CREATE TABLE storage_overrides (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  storage_class_name VARCHAR(64) NOT NULL,
  provider_type VARCHAR(32) NOT NULL,
  provider_storage_names TEXT[] NOT NULL,
  priority INTEGER DEFAULT 100,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version BIGINT DEFAULT 0,
  
  CONSTRAINT fk_storage_class FOREIGN KEY (storage_class_name) 
    REFERENCES storage_classes(name) ON DELETE CASCADE,
  CONSTRAINT uk_storage_class_provider UNIQUE (storage_class_name, provider_type)
);

CREATE INDEX idx_storage_overrides_class ON storage_overrides(storage_class_name);
CREATE INDEX idx_storage_overrides_provider ON storage_overrides(provider_type);

CREATE TRIGGER trg_storage_overrides_updated
BEFORE UPDATE ON storage_overrides
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

COMMENT ON TABLE storage_overrides IS 'Manual storage class to provider storage name mappings (bypasses scheduler)';
COMMENT ON COLUMN storage_overrides.provider_storage_names IS 'Array of provider storage names (pool names, storage IDs)';

-- ============================================================================
-- STEP 5: Update volumes table
-- ============================================================================

ALTER TABLE volumes 
  ADD COLUMN selected_storage_id UUID,
  ADD COLUMN scheduler_metadata JSONB;

ALTER TABLE volumes
  ADD CONSTRAINT fk_selected_storage 
  FOREIGN KEY (selected_storage_id) 
  REFERENCES provider_storage(id) ON DELETE SET NULL;

CREATE INDEX idx_volumes_selected_storage ON volumes(selected_storage_id);

COMMENT ON COLUMN volumes.selected_storage_id IS 'The provider_storage entry selected by scheduler';
COMMENT ON COLUMN volumes.scheduler_metadata IS 'Scheduler decision metadata (score, filters applied, etc.)';

-- ============================================================================
-- STEP 6: Deprecate provider_storage_mappings table
-- ============================================================================

ALTER TABLE provider_storage_mappings RENAME TO provider_storage_mappings_deprecated;

COMMENT ON TABLE provider_storage_mappings_deprecated IS 'DEPRECATED: Replaced by capability-based model. Will be removed in future version.';

-- ============================================================================
-- STEP 7: Create helper views
-- ============================================================================

-- View for storage class capabilities summary
CREATE OR REPLACE VIEW storage_class_capabilities AS
SELECT 
  sc.name,
  sc.type,
  sc.tier,
  sc.capabilities,
  sc.constraints,
  sc.description,
  COUNT(DISTINCT ps.id) as available_storage_count,
  COUNT(DISTINCT ps.provider_id) as provider_count
FROM storage_classes sc
LEFT JOIN provider_storage ps ON 
  ps.enabled = true AND
  ps.capabilities @> sc.capabilities
WHERE sc.capabilities IS NOT NULL
GROUP BY sc.name, sc.type, sc.tier, sc.capabilities, sc.constraints, sc.description;

COMMENT ON VIEW storage_class_capabilities IS 'Storage classes with capability matching statistics';

-- View for provider storage with capability details
CREATE OR REPLACE VIEW provider_storage_details AS
SELECT 
  ps.id,
  ps.provider_id,
  ps.provider_type,
  ps.external_id,
  ps.name,
  ps.storage_type,
  ps.capabilities,
  ps.metrics,
  ps.node_id,
  ps.datacenter_id,
  ps.enabled,
  ps.synced_at,
  COALESCE((ps.metrics->>'free_gb')::numeric, 0) as free_gb,
  COALESCE((ps.metrics->>'total_gb')::numeric, 0) as total_gb,
  CASE 
    WHEN COALESCE((ps.metrics->>'total_gb')::numeric, 0) > 0 
    THEN ROUND((COALESCE((ps.metrics->>'free_gb')::numeric, 0) / 
                COALESCE((ps.metrics->>'total_gb')::numeric, 1)) * 100, 2)
    ELSE 0 
  END as free_percentage
FROM provider_storage ps;

COMMENT ON VIEW provider_storage_details IS 'Provider storage with computed metrics';
