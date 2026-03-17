-- Sample Provider Storage Backend Configurations
-- This migration adds example storage mappings for common deployment scenarios
-- Administrators should customize these mappings based on their infrastructure

-- ============================================================================
-- SAMPLE CEPH RBD MAPPINGS
-- Example: Map storage classes to Ceph RBD pools
-- ============================================================================

-- Note: These are example mappings. Actual provider IDs should be replaced
-- with real provider UUIDs from the providers table after provider registration.

-- Example mapping for ultra-iops storage class to Ceph NVMe pool
-- Uncomment and customize after registering your Libvirt provider:
/*
INSERT INTO provider_storage_mappings (
    storage_class,
    provider_id,
    backend_type,
    backend_config,
    priority,
    enabled
) VALUES (
    'ultra-iops',
    '00000000-0000-0000-0000-000000000001'::uuid,  -- Replace with actual provider ID
    'ceph-rbd',
    '{
        "pool": "nvme_pool",
        "monitors": ["192.168.1.10:6789", "192.168.1.11:6789", "192.168.1.12:6789"],
        "user": "admin",
        "secretRef": "ceph-admin-secret"
    }'::jsonb,
    100,
    true
);
*/

-- Example mapping for fast-ssd storage class to Ceph SSD pool
/*
INSERT INTO provider_storage_mappings (
    storage_class,
    provider_id,
    backend_type,
    backend_config,
    priority,
    enabled
) VALUES (
    'fast-ssd',
    '00000000-0000-0000-0000-000000000001'::uuid,  -- Replace with actual provider ID
    'ceph-rbd',
    '{
        "pool": "ssd_pool",
        "monitors": ["192.168.1.10:6789", "192.168.1.11:6789", "192.168.1.12:6789"],
        "user": "admin",
        "secretRef": "ceph-admin-secret"
    }'::jsonb,
    100,
    true
);
*/

-- ============================================================================
-- SAMPLE LVM MAPPINGS
-- Example: Map storage classes to LVM volume groups
-- ============================================================================

-- Example mapping for balanced storage class to LVM with thin provisioning
/*
INSERT INTO provider_storage_mappings (
    storage_class,
    provider_id,
    backend_type,
    backend_config,
    priority,
    enabled
) VALUES (
    'balanced',
    '00000000-0000-0000-0000-000000000001'::uuid,  -- Replace with actual provider ID
    'lvm',
    '{
        "volumeGroup": "vg_storage",
        "thinPool": "thin_pool"
    }'::jsonb,
    90,
    true
);
*/

-- Example mapping for capacity-hdd storage class to LVM
/*
INSERT INTO provider_storage_mappings (
    storage_class,
    provider_id,
    backend_type,
    backend_config,
    priority,
    enabled
) VALUES (
    'capacity-hdd',
    '00000000-0000-0000-0000-000000000001'::uuid,  -- Replace with actual provider ID
    'lvm',
    '{
        "volumeGroup": "vg_capacity"
    }'::jsonb,
    80,
    true
);
*/

-- ============================================================================
-- SAMPLE ZFS MAPPINGS
-- Example: Map storage classes to ZFS pools
-- ============================================================================

-- Example mapping for fast-ssd storage class to ZFS with compression
/*
INSERT INTO provider_storage_mappings (
    storage_class,
    provider_id,
    backend_type,
    backend_config,
    priority,
    enabled
) VALUES (
    'fast-ssd',
    '00000000-0000-0000-0000-000000000002'::uuid,  -- Replace with actual provider ID
    'zfs',
    '{
        "pool": "tank/vms",
        "compression": "lz4",
        "dedup": false
    }'::jsonb,
    100,
    true
);
*/

-- Example mapping for capacity-hdd storage class to ZFS with deduplication
/*
INSERT INTO provider_storage_mappings (
    storage_class,
    provider_id,
    backend_type,
    backend_config,
    priority,
    enabled
) VALUES (
    'capacity-hdd',
    '00000000-0000-0000-0000-000000000002'::uuid,  -- Replace with actual provider ID
    'zfs',
    '{
        "pool": "tank/archive",
        "compression": "zstd",
        "dedup": true
    }'::jsonb,
    90,
    true
);
*/

-- ============================================================================
-- HELPER FUNCTION: Create Storage Mapping
-- Convenience function for creating storage mappings via SQL
-- ============================================================================

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
    -- Check if mapping already exists
    IF EXISTS (
        SELECT 1 FROM provider_storage_mappings
        WHERE storage_class = p_storage_class
        AND provider_id = p_provider_id
    ) THEN
        RAISE EXCEPTION 'Mapping already exists for storage class % and provider %',
            p_storage_class, p_provider_id;
    END IF;

    -- Create the mapping
    INSERT INTO provider_storage_mappings (
        storage_class,
        provider_id,
        backend_type,
        backend_config,
        priority,
        enabled
    ) VALUES (
        p_storage_class,
        p_provider_id,
        p_backend_type,
        p_backend_config,
        p_priority,
        p_enabled
    ) RETURNING id INTO v_mapping_id;

    RETURN v_mapping_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- HELPER FUNCTION: Update Storage Mapping Configuration
-- Update backend configuration for an existing mapping
-- ============================================================================

CREATE OR REPLACE FUNCTION update_storage_mapping_config(
    p_mapping_id UUID,
    p_backend_config JSONB
)
RETURNS VOID AS $$
BEGIN
    UPDATE provider_storage_mappings
    SET backend_config = p_backend_config
    WHERE id = p_mapping_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Mapping not found: %', p_mapping_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- HELPER FUNCTION: Enable/Disable Storage Mapping
-- Toggle mapping enabled status
-- ============================================================================

CREATE OR REPLACE FUNCTION set_storage_mapping_enabled(
    p_mapping_id UUID,
    p_enabled BOOLEAN
)
RETURNS VOID AS $$
BEGIN
    UPDATE provider_storage_mappings
    SET enabled = p_enabled
    WHERE id = p_mapping_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Mapping not found: %', p_mapping_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- HELPER VIEW: Storage Mapping Summary
-- View showing all storage mappings with human-readable information
-- ============================================================================

CREATE OR REPLACE VIEW storage_mapping_summary AS
SELECT 
    psm.id,
    psm.storage_class,
    sc.type as storage_type,
    sc.tier as storage_tier,
    psm.provider_id,
    psm.backend_type,
    psm.backend_config,
    psm.priority,
    psm.enabled,
    psm.created_at,
    psm.updated_at
FROM provider_storage_mappings psm
JOIN storage_classes sc ON psm.storage_class = sc.name
ORDER BY psm.storage_class, psm.priority DESC;

-- ============================================================================
-- TABLE COMMENTS
-- ============================================================================

COMMENT ON FUNCTION create_storage_mapping IS 'Create a new provider storage mapping';
COMMENT ON FUNCTION update_storage_mapping_config IS 'Update backend configuration for a storage mapping';
COMMENT ON FUNCTION set_storage_mapping_enabled IS 'Enable or disable a storage mapping';
COMMENT ON VIEW storage_mapping_summary IS 'Summary view of all storage mappings with storage class details';

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Example 1: Create a Ceph RBD mapping for fast-ssd storage class
-- SELECT create_storage_mapping(
--     'fast-ssd',
--     '00000000-0000-0000-0000-000000000001'::uuid,
--     'ceph-rbd',
--     '{"pool": "ssd_pool", "monitors": ["192.168.1.10:6789"], "user": "admin"}'::jsonb,
--     100,
--     true
-- );

-- Example 2: Create an LVM mapping for balanced storage class
-- SELECT create_storage_mapping(
--     'balanced',
--     '00000000-0000-0000-0000-000000000001'::uuid,
--     'lvm',
--     '{"volumeGroup": "vg_storage", "thinPool": "thin_pool"}'::jsonb,
--     90,
--     true
-- );

-- Example 3: Create a ZFS mapping for capacity-hdd storage class
-- SELECT create_storage_mapping(
--     'capacity-hdd',
--     '00000000-0000-0000-0000-000000000002'::uuid,
--     'zfs',
--     '{"pool": "tank/archive", "compression": "zstd", "dedup": true}'::jsonb,
--     80,
--     true
-- );

-- Example 4: Update backend configuration
-- SELECT update_storage_mapping_config(
--     '00000000-0000-0000-0000-000000000003'::uuid,
--     '{"pool": "new_pool", "monitors": ["192.168.1.20:6789"]}'::jsonb
-- );

-- Example 5: Disable a mapping
-- SELECT set_storage_mapping_enabled('00000000-0000-0000-0000-000000000003'::uuid, false);

-- Example 6: View all storage mappings
-- SELECT * FROM storage_mapping_summary;
