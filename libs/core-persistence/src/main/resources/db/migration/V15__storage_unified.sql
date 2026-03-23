-- Unified Storage Schema Migration
-- This migration creates the complete database schema for Infron's unified storage architecture
-- Includes storage classes, provider mappings, volumes, attachments, snapshots, and buckets
-- Supports block and object storage with multi-provider abstraction

-- ============================================================================
-- STORAGE CLASSES TABLE
-- Storage classes provide user-facing abstraction over provider-specific storage
-- Users select storage classes like "fast-ssd" instead of provider-specific configs
-- ============================================================================

CREATE TABLE storage_classes (
    name VARCHAR(64) PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    tier VARCHAR(32) NOT NULL,
    features JSONB NOT NULL,
    qos JSONB,
    allowed_providers JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Create trigger function for timestamp updates (reused across tables)
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic updated_at on storage_classes
CREATE TRIGGER trg_storage_classes_updated
BEFORE UPDATE ON storage_classes
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

-- Insert default storage classes covering common use cases
INSERT INTO storage_classes (name, type, tier, features, qos, allowed_providers) VALUES
('ultra-iops', 'block', 'performance', 
 '{"thinProvisioning": true, "encryption": true, "replication": 3, "snapshotSupport": true, "cloneSupport": true}',
 '{"minIops": 50000, "maxIops": 100000, "burstIops": 200000}',
 '["libvirt", "proxmox", "kubernetes"]'),
('fast-ssd', 'block', 'performance',
 '{"thinProvisioning": true, "encryption": true, "replication": 3, "snapshotSupport": true, "cloneSupport": true}',
 '{"minIops": 5000, "maxIops": 50000, "burstIops": 100000}',
 '["libvirt", "proxmox", "kubernetes"]'),
('balanced', 'block', 'balanced',
 '{"thinProvisioning": true, "encryption": true, "replication": 2, "snapshotSupport": true, "cloneSupport": true}',
 '{"minIops": 3000, "maxIops": 10000, "burstIops": 20000}',
 '["libvirt", "proxmox", "kubernetes"]'),
('capacity-hdd', 'block', 'capacity',
 '{"thinProvisioning": true, "encryption": false, "replication": 2, "snapshotSupport": true, "cloneSupport": false}',
 '{"minIops": 1000, "maxIops": 3000, "burstIops": 5000}',
 '["libvirt", "proxmox", "kubernetes"]'),
('archive', 'object', 'archive',
 '{"thinProvisioning": false, "encryption": true, "replication": 1, "snapshotSupport": false, "cloneSupport": false}',
 NULL,
 '["libvirt", "proxmox", "kubernetes"]');

-- ============================================================================
-- PROVIDER STORAGE MAPPINGS TABLE
-- Maps storage classes to provider-specific storage backends
-- Enables multi-provider support with priority-based selection
-- ============================================================================

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
    version BIGINT DEFAULT 0
);

-- Create indexes for performance (names must be unique within the schema in PostgreSQL)
CREATE INDEX idx_psm_storage_class ON provider_storage_mappings(storage_class);
CREATE INDEX idx_psm_provider_id ON provider_storage_mappings(provider_id);

-- Add unique constraint to prevent duplicate mappings
ALTER TABLE provider_storage_mappings 
ADD CONSTRAINT uk_storage_class_provider 
UNIQUE (storage_class, provider_id);

-- Add foreign key constraint to storage_classes
ALTER TABLE provider_storage_mappings 
ADD CONSTRAINT fk_storage_class 
FOREIGN KEY (storage_class) 
REFERENCES storage_classes(name) 
ON DELETE CASCADE;

-- Create trigger for automatic updated_at and version increment
CREATE OR REPLACE FUNCTION trigger_set_timestamp_and_version()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  NEW.version = OLD.version + 1;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for updates on provider_storage_mappings
CREATE TRIGGER trg_provider_storage_mappings_updated
BEFORE UPDATE ON provider_storage_mappings
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

-- ============================================================================
-- VOLUMES TABLE
-- Primary storage abstraction for persistent block storage
-- Volumes can be attached to VMs, pods, containers, etc.
-- ============================================================================

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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_volumes_workspace_id ON volumes(workspace_id);
CREATE INDEX idx_volumes_provider_id ON volumes(provider_id);
CREATE INDEX idx_volumes_status ON volumes(status);
CREATE INDEX idx_volumes_storage_class ON volumes(storage_class);
CREATE INDEX idx_volumes_deleted_at ON volumes(deleted_at) WHERE deleted_at IS NULL;

-- Composite indexes for common query patterns
CREATE INDEX idx_volumes_workspace_storage ON volumes(workspace_id, storage_class) WHERE deleted_at IS NULL;
CREATE INDEX idx_volumes_provider_status ON volumes(provider_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_volumes_workspace_status ON volumes(workspace_id, status) WHERE deleted_at IS NULL;

-- Add foreign key constraints
ALTER TABLE volumes 
ADD CONSTRAINT fk_storage_class 
FOREIGN KEY (storage_class) 
REFERENCES storage_classes(name) 
ON DELETE RESTRICT;

-- Create trigger for updates on volumes
CREATE TRIGGER trg_volumes_updated
BEFORE UPDATE ON volumes
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

-- Add check constraints for data integrity
ALTER TABLE volumes 
ADD CONSTRAINT chk_size_bytes_positive 
CHECK (size_bytes > 0),
ADD CONSTRAINT chk_actual_size_bytes_positive 
CHECK (actual_size_bytes IS NULL OR actual_size_bytes >= 0),
ADD CONSTRAINT chk_status_valid 
CHECK (status IN ('creating', 'available', 'attaching', 'in_use', 'detaching', 'resizing', 'deleting', 'deleted'));

-- ============================================================================
-- VOLUME ATTACHMENTS TABLE
-- Tracks volume attachments to resources (VMs, pods, containers)
-- Maintains history of all attachments for audit purposes
-- ============================================================================

CREATE TABLE volume_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    volume_id UUID NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id UUID NOT NULL,
    device VARCHAR(64),
    attached_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    detached_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_va_volume_id ON volume_attachments(volume_id);
CREATE INDEX idx_resource ON volume_attachments(resource_type, resource_id);
CREATE INDEX idx_attachments_resource_active ON volume_attachments(resource_type, resource_id) 
WHERE detached_at IS NULL;

-- Add unique constraint to prevent duplicate active attachments
-- Allows historical tracking via detached_at
ALTER TABLE volume_attachments 
ADD CONSTRAINT uk_volume_resource_detached 
UNIQUE (volume_id, resource_id, detached_at);

-- Add foreign key constraint to volumes
ALTER TABLE volume_attachments 
ADD CONSTRAINT fk_volume 
FOREIGN KEY (volume_id) 
REFERENCES volumes(id) 
ON DELETE CASCADE;

-- Create trigger for version increment on update
CREATE OR REPLACE FUNCTION trigger_set_version()
RETURNS TRIGGER AS $$
BEGIN
  NEW.version = OLD.version + 1;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_volume_attachments_updated
BEFORE UPDATE ON volume_attachments
FOR EACH ROW
EXECUTE FUNCTION trigger_set_version();

-- Add check constraints
ALTER TABLE volume_attachments 
ADD CONSTRAINT chk_resource_type_valid 
CHECK (resource_type IN ('vm', 'pod', 'container', 'service')),
ADD CONSTRAINT chk_device_format 
CHECK (device IS NULL OR device ~ '^/dev/|^pvc-|^disk-');

-- ============================================================================
-- SNAPSHOTS TABLE
-- Point-in-time snapshots of volumes for backup, restore, and clone operations
-- Supports immutable snapshots for compliance/WORM requirements
-- ============================================================================

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
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_snapshots_volume_id ON snapshots(volume_id);
CREATE INDEX idx_snapshots_status ON snapshots(status);
CREATE INDEX idx_snapshots_immutable ON snapshots(immutable);
CREATE INDEX idx_snapshots_retention_until ON snapshots(retention_until) WHERE retention_until IS NOT NULL;
CREATE INDEX idx_snapshots_deleted_at ON snapshots(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_snapshots_created_at ON snapshots(created_at);

-- Composite index for common query patterns
CREATE INDEX idx_snapshots_volume_status ON snapshots(volume_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_snapshots_workspace_created ON snapshots USING btree (created_at DESC)
WHERE deleted_at IS NULL;

-- Add foreign key constraint to volumes
ALTER TABLE snapshots 
ADD CONSTRAINT fk_volume 
FOREIGN KEY (volume_id) 
REFERENCES volumes(id) 
ON DELETE CASCADE;

-- Create trigger for updates on snapshots
CREATE TRIGGER trg_snapshots_updated
BEFORE UPDATE ON snapshots
FOR EACH ROW
EXECUTE FUNCTION trigger_set_version();

-- Add check constraints for data integrity
ALTER TABLE snapshots 
ADD CONSTRAINT chk_size_bytes_positive 
CHECK (size_bytes > 0),
ADD CONSTRAINT chk_status_valid 
CHECK (status IN ('creating', 'available', 'deleting', 'deleted')),
ADD CONSTRAINT chk_retention_logic 
CHECK (
  (immutable = false AND retention_until IS NULL) OR
  (immutable = true AND retention_until IS NOT NULL)
);

-- ============================================================================
-- BUCKETS TABLE
-- S3-compatible object storage buckets for unstructured data
-- Supports versioning, encryption, ACLs, and lifecycle policies
-- ============================================================================

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
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_buckets_workspace_id ON buckets(workspace_id);
CREATE INDEX idx_buckets_storage_class ON buckets(storage_class);
CREATE INDEX idx_buckets_deleted_at ON buckets(deleted_at) WHERE deleted_at IS NULL;

-- Add unique constraint for bucket names within workspace
ALTER TABLE buckets 
ADD CONSTRAINT uk_name_workspace 
UNIQUE (name, workspace_id);

-- Add foreign key constraint to storage_classes
ALTER TABLE buckets 
ADD CONSTRAINT fk_storage_class 
FOREIGN KEY (storage_class) 
REFERENCES storage_classes(name) 
ON DELETE RESTRICT;

-- Create trigger for updates on buckets
CREATE TRIGGER trg_buckets_updated
BEFORE UPDATE ON buckets
FOR EACH ROW
EXECUTE FUNCTION trigger_set_version();

-- Add check constraints for S3 compliance
ALTER TABLE buckets 
ADD CONSTRAINT chk_name_format 
CHECK (name ~ '^[a-z0-9][a-z0-9.-]*[a-z0-9]$' AND length(name) >= 3 AND length(name) <= 63),
ADD CONSTRAINT chk_acl_valid 
CHECK (acl IN ('private', 'public-read', 'public-read-write', 'authenticated-read', 'bucket-owner-read', 'bucket-owner-full-control'));

-- ============================================================================
-- UTILITY FUNCTIONS AND VIEWS
-- Helper functions for business logic and reporting views
-- ============================================================================

-- Function to check if volume can be deleted (must not be attached)
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

-- Function to check if snapshot can be deleted (immutable retention check)
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

-- Function to generate unique volume name in workspace
CREATE OR REPLACE FUNCTION generate_volume_name(workspace_uuid UUID, base_name TEXT)
RETURNS TEXT AS $$
DECLARE
    counter INTEGER;
    new_name TEXT;
BEGIN
    -- Try base name first
    SELECT COUNT(*) INTO counter
    FROM volumes 
    WHERE workspace_id = workspace_uuid AND name = base_name AND deleted_at IS NULL;
    
    IF counter = 0 THEN
        RETURN base_name;
    END IF;
    
    -- Try with numeric suffix
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

-- ============================================================================
-- REPORTING VIEWS
-- Pre-computed views for storage analytics and reporting
-- ============================================================================

-- View for active volume attachments with volume details
CREATE OR REPLACE VIEW active_volume_attachments AS
SELECT 
    va.id,
    va.volume_id,
    va.resource_type,
    va.resource_id,
    va.device,
    va.attached_at,
    v.name as volume_name,
    v.workspace_id,
    v.provider_id,
    v.status as volume_status
FROM volume_attachments va
JOIN volumes v ON va.volume_id = v.id
WHERE va.detached_at IS NULL AND v.deleted_at IS NULL;

-- View for active snapshots with volume details and retention status
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
    v.name as volume_name,
    v.workspace_id,
    v.provider_id,
    v.storage_class,
    CASE 
        WHEN s.immutable = true AND s.retention_until > now() THEN 'protected'
        WHEN s.immutable = true AND s.retention_until <= now() THEN 'expired'
        ELSE 'normal'
    END as retention_status
FROM snapshots s
JOIN volumes v ON s.volume_id = v.id
WHERE s.deleted_at IS NULL AND v.deleted_at IS NULL;

-- View for active buckets with storage class details
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
    sc.type as storage_type,
    sc.tier as storage_tier,
    sc.features as storage_features
FROM buckets b
JOIN storage_classes sc ON b.storage_class = sc.name
WHERE b.deleted_at IS NULL;

-- View for storage usage statistics per workspace
CREATE OR REPLACE VIEW workspace_storage_usage AS
SELECT 
    v.workspace_id,
    COUNT(v.id) as volume_count,
    COALESCE(SUM(v.size_bytes), 0) as total_allocated_bytes,
    COALESCE(SUM(v.actual_size_bytes), 0) as total_used_bytes,
    COUNT(DISTINCT v.storage_class) as storage_classes_used,
    COUNT(DISTINCT v.provider_id) as providers_used,
    COUNT(s.id) as snapshot_count,
    COALESCE(SUM(s.size_bytes), 0) as total_snapshot_bytes
FROM volumes v
LEFT JOIN snapshots s ON v.id = s.volume_id AND s.deleted_at IS NULL
WHERE v.deleted_at IS NULL
GROUP BY v.workspace_id;

-- View for provider storage capacity and usage
CREATE OR REPLACE VIEW provider_storage_usage AS
SELECT 
    v.provider_id,
    COUNT(v.id) as volume_count,
    COALESCE(SUM(v.size_bytes), 0) as total_allocated_bytes,
    COALESCE(SUM(v.actual_size_bytes), 0) as total_used_bytes,
    COUNT(DISTINCT v.workspace_id) as workspaces_served,
    COUNT(DISTINCT v.storage_class) as storage_classes_served,
    COUNT(va.id) as active_attachments
FROM volumes v
LEFT JOIN volume_attachments va ON v.id = va.volume_id AND va.detached_at IS NULL
WHERE v.deleted_at IS NULL
GROUP BY v.provider_id;

-- View for storage class utilization statistics
CREATE OR REPLACE VIEW storage_class_utilization AS
SELECT 
    sc.name,
    sc.type,
    sc.tier,
    COUNT(v.id) as volume_count,
    COALESCE(SUM(v.size_bytes), 0) as total_allocated_bytes,
    COUNT(DISTINCT v.workspace_id) as workspaces_using,
    COUNT(DISTINCT v.provider_id) as providers_supporting,
    COUNT(DISTINCT psm.provider_id) as providers_configured
FROM storage_classes sc
LEFT JOIN volumes v ON sc.name = v.storage_class AND v.deleted_at IS NULL
LEFT JOIN provider_storage_mappings psm ON sc.name = psm.storage_class AND psm.enabled = true
GROUP BY sc.name, sc.type, sc.tier;

-- ============================================================================
-- TABLE COMMENTS
-- Add descriptive comments for documentation
-- ============================================================================

COMMENT ON TABLE storage_classes IS 'Storage class definitions providing user-facing storage capabilities';
COMMENT ON TABLE provider_storage_mappings IS 'Maps storage classes to provider-specific storage backends';
COMMENT ON TABLE volumes IS 'Persistent block storage volumes';
COMMENT ON TABLE volume_attachments IS 'Tracks volume attachments to resources (VMs, pods, containers)';
COMMENT ON TABLE snapshots IS 'Point-in-time snapshots of volumes';
COMMENT ON TABLE buckets IS 'S3-compatible object storage buckets';

COMMENT ON VIEW active_volume_attachments IS 'Currently active volume attachments with volume details';
COMMENT ON VIEW active_snapshots IS 'Active snapshots with volume details and retention status';
COMMENT ON VIEW active_buckets IS 'Active buckets with storage class details';
COMMENT ON VIEW workspace_storage_usage IS 'Storage usage statistics per workspace';
COMMENT ON VIEW provider_storage_usage IS 'Storage usage statistics per provider';
COMMENT ON VIEW storage_class_utilization IS 'Storage class utilization statistics';

-- ============================================================================
-- PERMISSIONS (Optional - uncomment and adjust for your environment)
-- These would typically be configured by your deployment scripts
-- ============================================================================

-- GRANT SELECT, INSERT, UPDATE, DELETE ON storage_classes, provider_storage_mappings, volumes, volume_attachments, snapshots, buckets TO infron_app;
-- GRANT SELECT ON ALL SEQUENCES TO infron_app;
-- GRANT EXECUTE ON FUNCTION can_delete_volume, can_delete_snapshot, generate_volume_name TO infron_app;
-- GRANT SELECT ON active_volume_attachments, active_snapshots, active_buckets, workspace_storage_usage, provider_storage_usage, storage_class_utilization TO infron_app;
