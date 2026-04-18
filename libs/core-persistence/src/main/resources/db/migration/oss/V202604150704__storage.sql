-- Unified + capability-based storage (merged V15, V16, V18 without deprecate rename, V20, V46 naming; no V17 data migration)

CREATE OR REPLACE FUNCTION trigger_set_timestamp_and_version()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  NEW.version = OLD.version + 1;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trigger_set_version()
RETURNS TRIGGER AS $$
BEGIN
  NEW.version = OLD.version + 1;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE storage_classes (
    name VARCHAR(64) PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    tier VARCHAR(32) NOT NULL,
    features JSONB NOT NULL,
    qos JSONB,
    allowed_providers JSONB,
    capabilities JSONB NOT NULL,
    constraints JSONB NOT NULL,
    description TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_storage_classes_updated
BEFORE UPDATE ON storage_classes
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

INSERT INTO storage_classes (name, type, tier, features, qos, allowed_providers, capabilities, constraints, description) VALUES
('ultra-iops', 'block', 'performance',
 '{"thinProvisioning": true, "encryption": true, "replication": 3, "snapshotSupport": true, "cloneSupport": true}',
 '{"minIops": 50000, "maxIops": 100000, "burstIops": 200000}',
 '["libvirt", "proxmox"]',
 '{"performance": "high", "media": "ssd", "shared": true, "redundancy": "replicated"}',
 '{"min_iops": 50000, "max_latency_ms": 10}',
 'High-performance storage with low latency'),
('fast-ssd', 'block', 'performance',
 '{"thinProvisioning": true, "encryption": true, "replication": 3, "snapshotSupport": true, "cloneSupport": true}',
 '{"minIops": 5000, "maxIops": 50000, "burstIops": 100000}',
 '["libvirt", "proxmox"]',
 '{"performance": "high", "media": "ssd", "shared": true, "redundancy": "replicated"}',
 '{"min_iops": 5000, "max_latency_ms": 10}',
 'High-performance storage with low latency'),
('balanced', 'block', 'balanced',
 '{"thinProvisioning": true, "encryption": true, "replication": 2, "snapshotSupport": true, "cloneSupport": true}',
 '{"minIops": 3000, "maxIops": 10000, "burstIops": 20000}',
 '["libvirt", "proxmox"]',
 '{"performance": "medium", "media": "any", "shared": true, "redundancy": "replicated"}',
 '{"min_iops": 3000, "max_latency_ms": 10}',
 'Balanced storage for general workloads'),
('capacity-hdd', 'block', 'capacity',
 '{"thinProvisioning": true, "encryption": false, "replication": 2, "snapshotSupport": true, "cloneSupport": false}',
 '{"minIops": 1000, "maxIops": 3000, "burstIops": 5000}',
 '["libvirt", "proxmox"]',
 '{"performance": "low", "media": "hdd", "shared": false, "redundancy": "none"}',
 '{"min_iops": 1000, "max_latency_ms": 10}',
 'High-capacity storage for large datasets'),
('archive', 'object', 'archive',
 '{"thinProvisioning": false, "encryption": true, "replication": 1, "snapshotSupport": false, "cloneSupport": false}',
 NULL,
 '["libvirt", "proxmox"]',
 '{"performance": "low", "media": "any", "shared": false, "redundancy": "none"}',
 '{"min_iops": 0, "max_latency_ms": 10}',
 'Archive storage for long-term retention');

COMMENT ON COLUMN storage_classes.capabilities IS 'Storage capabilities (performance, media, shared, redundancy)';
COMMENT ON COLUMN storage_classes.constraints IS 'Storage constraints (min_iops, max_latency_ms, etc.)';
COMMENT ON COLUMN storage_classes.version IS 'Optimistic locking version (JPA @Version)';

CREATE TABLE provider_storage_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_class VARCHAR(64) NOT NULL,
    provider_id UUID NOT NULL,
    backend_type VARCHAR(64) NOT NULL,
    backend_config JSONB NOT NULL,
    priority INTEGER DEFAULT 100,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_psm_storage_class_provider UNIQUE (storage_class, provider_id),
    CONSTRAINT fk_psm_storage_class FOREIGN KEY (storage_class) REFERENCES storage_classes(name) ON DELETE CASCADE
);

CREATE INDEX idx_psm_storage_class ON provider_storage_mappings (storage_class);
CREATE INDEX idx_psm_provider_id ON provider_storage_mappings (provider_id);

CREATE TRIGGER trg_provider_storage_mappings_updated
BEFORE UPDATE ON provider_storage_mappings
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

CREATE TABLE volumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    workspace_id UUID NOT NULL,
    storage_class VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    actual_size_bytes BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'creating',
    provider_id UUID NOT NULL,
    provider_volume_id VARCHAR(255) NOT NULL,
    encrypted BOOLEAN DEFAULT false,
    encryption_key_id VARCHAR(64),
    thin_provisioned BOOLEAN DEFAULT true,
    tags JSONB,
    selected_storage_id UUID,
    scheduler_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_volumes_storage_class FOREIGN KEY (storage_class) REFERENCES storage_classes(name) ON DELETE RESTRICT,
    CONSTRAINT chk_volumes_size_bytes_positive CHECK (size_bytes > 0),
    CONSTRAINT chk_volumes_actual_size_bytes_positive CHECK (actual_size_bytes IS NULL OR actual_size_bytes >= 0),
    CONSTRAINT chk_volumes_status_valid CHECK (status IN ('creating', 'available', 'attaching', 'in_use', 'detaching', 'resizing', 'deleting', 'deleted'))
);

CREATE INDEX idx_volumes_workspace_id ON volumes (workspace_id);
CREATE INDEX idx_volumes_provider_id ON volumes (provider_id);
CREATE INDEX idx_volumes_status ON volumes (status);
CREATE INDEX idx_volumes_storage_class ON volumes (storage_class);
CREATE INDEX idx_volumes_deleted_at ON volumes (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_volumes_workspace_storage ON volumes (workspace_id, storage_class) WHERE deleted_at IS NULL;
CREATE INDEX idx_volumes_provider_status ON volumes (provider_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_volumes_workspace_status ON volumes (workspace_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_volumes_selected_storage ON volumes (selected_storage_id);

CREATE TRIGGER trg_volumes_updated
BEFORE UPDATE ON volumes
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

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

CREATE INDEX idx_provider_storage_provider ON provider_storage (provider_id);
CREATE INDEX idx_provider_storage_type ON provider_storage (provider_type);
CREATE INDEX idx_provider_storage_datacenter ON provider_storage (datacenter_id);
CREATE INDEX idx_provider_storage_enabled ON provider_storage (enabled);
CREATE INDEX idx_provider_storage_capabilities ON provider_storage USING GIN (capabilities);
CREATE INDEX idx_provider_storage_name ON provider_storage (name);

CREATE TRIGGER trg_provider_storage_updated
BEFORE UPDATE ON provider_storage
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

ALTER TABLE volumes
    ADD CONSTRAINT fk_volumes_selected_storage
    FOREIGN KEY (selected_storage_id) REFERENCES provider_storage(id) ON DELETE SET NULL;

COMMENT ON TABLE provider_storage IS 'Normalized provider storage pools/classes with capabilities and metrics';
COMMENT ON COLUMN provider_storage.external_id IS 'Provider-specific storage identifier (pool name, storage ID)';
COMMENT ON COLUMN provider_storage.storage_type IS 'Backend storage type (ceph-rbd, lvm, zfs, dir, nfs, etc.)';
COMMENT ON COLUMN provider_storage.capabilities IS 'Normalized storage capabilities';
COMMENT ON COLUMN provider_storage.metrics IS 'Current storage metrics (free_gb, total_gb, iops, etc.)';
COMMENT ON COLUMN volumes.selected_storage_id IS 'The provider_storage entry selected by scheduler';
COMMENT ON COLUMN volumes.scheduler_metadata IS 'Scheduler decision metadata (score, filters applied, etc.)';

CREATE TABLE storage_capability_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    muxon_capability VARCHAR(64) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    provider_capability VARCHAR(64) NOT NULL,
    value_mapping JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_scm_capability_provider UNIQUE (muxon_capability, provider_type, provider_capability)
);

CREATE INDEX idx_scm_muxon_capability ON storage_capability_mappings (muxon_capability);
CREATE INDEX idx_scm_provider_type ON storage_capability_mappings (provider_type);

COMMENT ON TABLE storage_capability_mappings IS 'Maps Muxon generic capabilities to provider-specific equivalents';
COMMENT ON COLUMN storage_capability_mappings.muxon_capability IS 'Generic Muxon capability name';
COMMENT ON COLUMN storage_capability_mappings.provider_capability IS 'Provider-specific capability name';
COMMENT ON COLUMN storage_capability_mappings.value_mapping IS 'Value translation map (e.g., high -> [rbd, nvme])';

INSERT INTO storage_capability_mappings (muxon_capability, provider_type, provider_capability, value_mapping)
VALUES
('performance', 'libvirt', 'pool_type', '{"high": ["rbd", "nvme"], "medium": ["lvm-thin", "zfs"], "low": ["dir", "nfs"]}'),
('performance', 'proxmox', 'storage_type', '{"high": ["rbd", "zfspool"], "medium": ["lvmthin"], "low": ["dir", "nfs"]}'),
('media', 'libvirt', 'pool_type', '{"ssd": ["rbd", "lvm-thin"], "hdd": ["dir"], "nvme": ["nvme"], "any": ["*"]}'),
('media', 'proxmox', 'storage_type', '{"ssd": ["rbd", "zfspool", "lvmthin"], "hdd": ["dir"], "nvme": ["nvme"], "any": ["*"]}'),
('shared', 'libvirt', 'pool_type', '{"true": ["rbd", "nfs"], "false": ["lvm", "zfs", "dir"]}'),
('shared', 'proxmox', 'storage_type', '{"true": ["rbd", "nfs", "cifs"], "false": ["lvmthin", "zfspool", "dir"]}'),
('redundancy', 'libvirt', 'pool_type', '{"replicated": ["rbd"], "none": ["lvm", "zfs", "dir", "nfs"]}'),
('redundancy', 'proxmox', 'storage_type', '{"replicated": ["rbd"], "none": ["lvmthin", "zfspool", "dir", "nfs"]}')
ON CONFLICT (muxon_capability, provider_type, provider_capability) DO NOTHING;

CREATE TABLE storage_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_class_name VARCHAR(64) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    provider_storage_names TEXT[] NOT NULL,
    priority INTEGER DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_storage_overrides_class FOREIGN KEY (storage_class_name) REFERENCES storage_classes(name) ON DELETE CASCADE,
    CONSTRAINT uk_storage_overrides_class_provider UNIQUE (storage_class_name, provider_type)
);

CREATE INDEX idx_storage_overrides_class ON storage_overrides (storage_class_name);
CREATE INDEX idx_storage_overrides_provider ON storage_overrides (provider_type);

CREATE TRIGGER trg_storage_overrides_updated
BEFORE UPDATE ON storage_overrides
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

COMMENT ON TABLE storage_overrides IS 'Manual storage class to provider storage name mappings (bypasses scheduler)';
COMMENT ON COLUMN storage_overrides.provider_storage_names IS 'Array of provider storage names (pool names, storage IDs)';

CREATE TABLE volume_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    volume_id UUID NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id UUID NOT NULL,
    device VARCHAR(64),
    attached_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    detached_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_va_volume_resource_detached UNIQUE (volume_id, resource_id, detached_at),
    CONSTRAINT fk_va_volume FOREIGN KEY (volume_id) REFERENCES volumes(id) ON DELETE CASCADE,
    CONSTRAINT chk_va_resource_type_valid CHECK (resource_type IN ('vm', 'pod', 'container', 'service')),
    CONSTRAINT chk_va_device_format CHECK (device IS NULL OR device ~ '^/dev/|^pvc-|^disk-')
);

CREATE INDEX idx_va_volume_id ON volume_attachments (volume_id);
CREATE INDEX idx_va_resource ON volume_attachments (resource_type, resource_id);
CREATE INDEX idx_va_attachments_resource_active ON volume_attachments (resource_type, resource_id) WHERE detached_at IS NULL;

CREATE TRIGGER trg_volume_attachments_updated
BEFORE UPDATE ON volume_attachments
FOR EACH ROW
EXECUTE FUNCTION trigger_set_version();

CREATE TABLE snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    volume_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'creating',
    provider_snapshot_id VARCHAR(255) NOT NULL,
    immutable BOOLEAN DEFAULT false,
    retention_until TIMESTAMPTZ,
    backup_policy_id VARCHAR(64),
    tags JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_snapshots_volume FOREIGN KEY (volume_id) REFERENCES volumes(id) ON DELETE CASCADE,
    CONSTRAINT chk_snapshots_size_bytes_positive CHECK (size_bytes > 0),
    CONSTRAINT chk_snapshots_status_valid CHECK (status IN ('creating', 'available', 'deleting', 'deleted')),
    CONSTRAINT chk_snapshots_retention_logic CHECK (
        (immutable = false AND retention_until IS NULL) OR
        (immutable = true AND retention_until IS NOT NULL))
);

CREATE INDEX idx_snapshots_volume_id ON snapshots (volume_id);
CREATE INDEX idx_snapshots_status ON snapshots (status);
CREATE INDEX idx_snapshots_immutable ON snapshots (immutable);
CREATE INDEX idx_snapshots_retention_until ON snapshots (retention_until) WHERE retention_until IS NOT NULL;
CREATE INDEX idx_snapshots_deleted_at ON snapshots (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_snapshots_created_at ON snapshots (created_at);
CREATE INDEX idx_snapshots_volume_status ON snapshots (volume_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_snapshots_workspace_created ON snapshots USING btree (created_at DESC) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_snapshots_updated
BEFORE UPDATE ON snapshots
FOR EACH ROW
EXECUTE FUNCTION trigger_set_version();

CREATE TABLE buckets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    workspace_id UUID NOT NULL,
    storage_class VARCHAR(64) NOT NULL,
    region VARCHAR(64),
    versioning BOOLEAN DEFAULT false,
    encryption JSONB,
    acl VARCHAR(32) DEFAULT 'private',
    lifecycle_rules JSONB,
    provider_bucket_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_buckets_name_workspace UNIQUE (name, workspace_id),
    CONSTRAINT fk_buckets_storage_class FOREIGN KEY (storage_class) REFERENCES storage_classes(name) ON DELETE RESTRICT,
    CONSTRAINT chk_buckets_name_format CHECK (name ~ '^[a-z0-9][a-z0-9.-]*[a-z0-9]$' AND length(name) >= 3 AND length(name) <= 63),
    CONSTRAINT chk_buckets_acl_valid CHECK (acl IN ('private', 'public-read', 'public-read-write', 'authenticated-read', 'bucket-owner-read', 'bucket-owner-full-control'))
);

CREATE INDEX idx_buckets_workspace_id ON buckets (workspace_id);
CREATE INDEX idx_buckets_storage_class ON buckets (storage_class);
CREATE INDEX idx_buckets_deleted_at ON buckets (deleted_at) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_buckets_updated
BEFORE UPDATE ON buckets
FOR EACH ROW
EXECUTE FUNCTION trigger_set_version();

CREATE OR REPLACE FUNCTION can_delete_volume(volume_uuid UUID)
RETURNS BOOLEAN AS $$
DECLARE
    attachment_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO attachment_count
    FROM volume_attachments
    WHERE volume_id = volume_uuid AND detached_at IS NULL;
    RETURN attachment_count = 0;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION can_delete_snapshot(snapshot_uuid UUID)
RETURNS BOOLEAN AS $$
DECLARE
    snapshot_record RECORD;
BEGIN
    SELECT s.immutable, s.retention_until INTO snapshot_record
    FROM snapshots s
    WHERE s.id = snapshot_uuid AND s.deleted_at IS NULL;
    IF NOT FOUND THEN
        RETURN false;
    END IF;
    IF snapshot_record.immutable AND snapshot_record.retention_until > now() THEN
        RETURN false;
    END IF;
    RETURN true;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_volume_name(workspace_uuid UUID, base_name TEXT)
RETURNS TEXT AS $$
DECLARE
    counter INTEGER;
    new_name TEXT;
BEGIN
    SELECT COUNT(*) INTO counter
    FROM volumes
    WHERE workspace_id = workspace_uuid AND name = base_name AND deleted_at IS NULL;
    IF counter = 0 THEN
        RETURN base_name;
    END IF;
    FOR counter IN 1..999 LOOP
        new_name := base_name || '-' || counter;
        SELECT COUNT(*) INTO counter
        FROM volumes
        WHERE workspace_id = workspace_uuid AND name = new_name AND deleted_at IS NULL;
        IF counter = 0 THEN
            RETURN new_name;
        END IF;
    END LOOP;
    RAISE EXCEPTION 'Could not generate unique volume name for base %', base_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW active_volume_attachments AS
SELECT
    va.id,
    va.volume_id,
    va.resource_type,
    va.resource_id,
    va.device,
    va.attached_at,
    v.name AS volume_name,
    v.workspace_id,
    v.provider_id,
    v.status AS volume_status
FROM volume_attachments va
JOIN volumes v ON va.volume_id = v.id
WHERE va.detached_at IS NULL AND v.deleted_at IS NULL;

CREATE OR REPLACE VIEW active_snapshots AS
SELECT
    s.id,
    s.volume_id,
    s.name,
    s.size_bytes,
    s.status,
    s.immutable,
    s.retention_until,
    s.backup_policy_id,
    s.created_at,
    v.name AS volume_name,
    v.workspace_id,
    v.provider_id,
    v.storage_class,
    CASE
        WHEN s.immutable = true AND s.retention_until > now() THEN 'protected'
        WHEN s.immutable = true AND s.retention_until <= now() THEN 'expired'
        ELSE 'normal'
    END AS retention_status
FROM snapshots s
JOIN volumes v ON s.volume_id = v.id
WHERE s.deleted_at IS NULL AND v.deleted_at IS NULL;

CREATE OR REPLACE VIEW active_buckets AS
SELECT
    b.id,
    b.name,
    b.workspace_id,
    b.storage_class,
    b.region,
    b.versioning,
    b.acl,
    b.created_at,
    sc.type AS storage_type,
    sc.tier AS storage_tier,
    sc.features AS storage_features
FROM buckets b
JOIN storage_classes sc ON b.storage_class = sc.name
WHERE b.deleted_at IS NULL;

CREATE OR REPLACE VIEW workspace_storage_usage AS
SELECT
    v.workspace_id,
    COUNT(v.id) AS volume_count,
    COALESCE(SUM(v.size_bytes), 0) AS total_allocated_bytes,
    COALESCE(SUM(v.actual_size_bytes), 0) AS total_used_bytes,
    COUNT(DISTINCT v.storage_class) AS storage_classes_used,
    COUNT(DISTINCT v.provider_id) AS providers_used,
    COUNT(s.id) AS snapshot_count,
    COALESCE(SUM(s.size_bytes), 0) AS total_snapshot_bytes
FROM volumes v
LEFT JOIN snapshots s ON v.id = s.volume_id AND s.deleted_at IS NULL
WHERE v.deleted_at IS NULL
GROUP BY v.workspace_id;

CREATE OR REPLACE VIEW provider_storage_usage AS
SELECT
    v.provider_id,
    COUNT(v.id) AS volume_count,
    COALESCE(SUM(v.size_bytes), 0) AS total_allocated_bytes,
    COALESCE(SUM(v.actual_size_bytes), 0) AS total_used_bytes,
    COUNT(DISTINCT v.workspace_id) AS workspaces_served,
    COUNT(DISTINCT v.storage_class) AS storage_classes_served,
    COUNT(va.id) AS active_attachments
FROM volumes v
LEFT JOIN volume_attachments va ON v.id = va.volume_id AND va.detached_at IS NULL
WHERE v.deleted_at IS NULL
GROUP BY v.provider_id;

CREATE OR REPLACE VIEW storage_class_utilization AS
SELECT
    sc.name,
    sc.type,
    sc.tier,
    COUNT(v.id) AS volume_count,
    COALESCE(SUM(v.size_bytes), 0) AS total_allocated_bytes,
    COUNT(DISTINCT v.workspace_id) AS workspaces_using,
    COUNT(DISTINCT v.provider_id) AS providers_supporting,
    COUNT(DISTINCT psm.provider_id) AS providers_configured
FROM storage_classes sc
LEFT JOIN volumes v ON sc.name = v.storage_class AND v.deleted_at IS NULL
LEFT JOIN provider_storage_mappings psm ON sc.name = psm.storage_class AND psm.enabled = true
GROUP BY sc.name, sc.type, sc.tier;

CREATE OR REPLACE VIEW storage_class_capabilities AS
SELECT
    sc.name,
    sc.type,
    sc.tier,
    sc.capabilities,
    sc.constraints,
    sc.description,
    COUNT(DISTINCT ps.id) AS available_storage_count,
    COUNT(DISTINCT ps.provider_id) AS provider_count
FROM storage_classes sc
LEFT JOIN provider_storage ps ON
    ps.enabled = true AND
    ps.capabilities @> sc.capabilities
WHERE sc.capabilities IS NOT NULL
GROUP BY sc.name, sc.type, sc.tier, sc.capabilities, sc.constraints, sc.description;

COMMENT ON VIEW storage_class_capabilities IS 'Storage classes with capability matching statistics';

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
    COALESCE((ps.metrics->>'free_gb')::numeric, 0) AS free_gb,
    COALESCE((ps.metrics->>'total_gb')::numeric, 0) AS total_gb,
    CASE
        WHEN COALESCE((ps.metrics->>'total_gb')::numeric, 0) > 0
        THEN ROUND((COALESCE((ps.metrics->>'free_gb')::numeric, 0) /
            COALESCE((ps.metrics->>'total_gb')::numeric, 1)) * 100, 2)
        ELSE 0
    END AS free_percentage
FROM provider_storage ps;

COMMENT ON VIEW provider_storage_details IS 'Provider storage with computed metrics';

CREATE OR REPLACE FUNCTION create_storage_mapping(
    p_storage_class VARCHAR(64),
    p_provider_id UUID,
    p_backend_type VARCHAR(64),
    p_backend_config JSONB,
    p_priority INTEGER DEFAULT 100,
    p_enabled BOOLEAN DEFAULT true
)
RETURNS UUID AS $$
DECLARE
    v_mapping_id UUID;
BEGIN
    IF EXISTS (
        SELECT 1 FROM provider_storage_mappings
        WHERE storage_class = p_storage_class AND provider_id = p_provider_id
    ) THEN
        RAISE EXCEPTION 'Mapping already exists for storage class % and provider %',
            p_storage_class, p_provider_id;
    END IF;
    INSERT INTO provider_storage_mappings (
        storage_class, provider_id, backend_type, backend_config, priority, enabled
    ) VALUES (
        p_storage_class, p_provider_id, p_backend_type, p_backend_config, p_priority, p_enabled
    ) RETURNING id INTO v_mapping_id;
    RETURN v_mapping_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_storage_mapping_config(p_mapping_id UUID, p_backend_config JSONB)
RETURNS VOID AS $$
BEGIN
    UPDATE provider_storage_mappings SET backend_config = p_backend_config WHERE id = p_mapping_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Mapping not found: %', p_mapping_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_storage_mapping_enabled(p_mapping_id UUID, p_enabled BOOLEAN)
RETURNS VOID AS $$
BEGIN
    UPDATE provider_storage_mappings SET enabled = p_enabled WHERE id = p_mapping_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Mapping not found: %', p_mapping_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW storage_mapping_summary AS
SELECT
    psm.id,
    psm.storage_class,
    sc.type AS storage_type,
    sc.tier AS storage_tier,
    psm.provider_id,
    psm.backend_type,
    psm.backend_config,
    psm.priority,
    psm.enabled,
    psm.created_at,
    psm.updated_at
FROM provider_storage_mappings psm
JOIN storage_classes sc ON psm.storage_class = sc.name;

COMMENT ON TABLE storage_classes IS 'Storage class definitions providing user-facing storage capabilities';
COMMENT ON TABLE provider_storage_mappings IS 'Maps storage classes to provider-specific storage backends';
COMMENT ON TABLE volumes IS 'Persistent block storage volumes';
COMMENT ON TABLE volume_attachments IS 'Tracks volume attachments to resources (VMs, pods, containers)';
COMMENT ON TABLE snapshots IS 'Point-in-time snapshots of volumes';
COMMENT ON TABLE buckets IS 'S3-compatible object storage buckets';
