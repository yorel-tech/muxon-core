-- Content libraries, items, distribution (merged V25–V27, V32–V34 schema, V36–V38, V40–V43, V46 content rename; before VM due to vm.content_item_id FK)

CREATE TABLE content_storage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL,
    storage_type VARCHAR(32) NOT NULL,
    config JSONB NOT NULL DEFAULT '{}',
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_content_storage_type CHECK (storage_type IN ('local', 'nfs', 's3'))
);

CREATE UNIQUE INDEX uq_content_storage_single_default ON content_storage (is_default) WHERE is_default = true;
CREATE INDEX idx_content_storage_name ON content_storage (name);

INSERT INTO content_storage (name, storage_type, config, is_default)
VALUES (
    'Default content store',
    'local',
    jsonb_build_object('path', '${contentStorageDefaultPath}'),
    true
);

CREATE TABLE content_libraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL,
    description TEXT,
    library_type VARCHAR(32) NOT NULL,
    access_mode VARCHAR(32) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    source_config JSONB,
    storage_class_name VARCHAR(64),
    sync_status VARCHAR(32) NOT NULL DEFAULT 'never_synced',
    last_synced_at TIMESTAMPTZ,
    metadata JSONB,
    content_storage_id UUID NOT NULL REFERENCES content_storage (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_content_libraries_tenant_id ON content_libraries (tenant_id);

CREATE TRIGGER trg_content_libraries_updated
BEFORE UPDATE ON content_libraries
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

CREATE TABLE content_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID NOT NULL REFERENCES content_libraries (id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    content_type VARCHAR(64) NOT NULL,
    version_label VARCHAR(128),
    size_bytes BIGINT,
    checksum VARCHAR(256),
    checksum_algorithm VARCHAR(32),
    source_url TEXT,
    source_item_id UUID NULL REFERENCES content_items (id) ON DELETE SET NULL,
    content_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    last_replicated_at TIMESTAMPTZ,
    provider_relative_path TEXT,
    muxon_instance_segment VARCHAR(64),
    template_spec JSONB,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_content_items_library_id ON content_items (library_id);
CREATE INDEX idx_content_items_content_type ON content_items (content_type);
CREATE INDEX idx_content_items_content_status ON content_items (content_status);
CREATE INDEX idx_content_items_template_spec_gin
    ON content_items USING GIN (template_spec jsonb_path_ops)
    WHERE content_type = 'vm_template' AND template_spec IS NOT NULL;
CREATE INDEX idx_content_items_template_os_family
    ON content_items ((template_spec->'metadata'->>'osFamily'))
    WHERE content_type = 'vm_template' AND template_spec IS NOT NULL;
CREATE INDEX idx_content_items_template_firmware
    ON content_items ((template_spec->'spec'->>'firmware'))
    WHERE content_type = 'vm_template' AND template_spec IS NOT NULL;

CREATE TRIGGER trg_content_items_updated
BEFORE UPDATE ON content_items
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

COMMENT ON COLUMN content_items.provider_relative_path IS 'Relative path under provider storage pool (muxon-.../content-libraries/...)';
COMMENT ON COLUMN content_items.muxon_instance_segment IS 'muxon-{instanceName}-{instanceId} segment at time path was assigned';

CREATE TABLE content_library_distribution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID NOT NULL REFERENCES content_libraries (id) ON DELETE CASCADE,
    datacenter_id UUID NOT NULL REFERENCES datacenter (id) ON DELETE CASCADE,
    replicate_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    last_replicated_at TIMESTAMPTZ,
    storage_class_name VARCHAR(128),
    progress_percent INT NOT NULL DEFAULT 0
        CHECK (progress_percent BETWEEN 0 AND 100),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (library_id, datacenter_id)
);

CREATE INDEX idx_cld_library_id ON content_library_distribution (library_id);
CREATE INDEX idx_cld_datacenter_id ON content_library_distribution (datacenter_id);

COMMENT ON COLUMN content_library_distribution.storage_class_name IS
    'Datacenter storage class used to select provider pools for replication; set at publish time.';

CREATE TRIGGER trg_content_library_distribution_updated
BEFORE UPDATE ON content_library_distribution
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

CREATE TABLE content_item_distribution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id UUID NOT NULL REFERENCES content_items (id) ON DELETE CASCADE,
    distribution_id UUID NOT NULL REFERENCES content_library_distribution (id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COPYING', 'READY', 'FAILED')),
    checksum_verified BOOLEAN NOT NULL DEFAULT FALSE,
    size_bytes BIGINT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (content_item_id, distribution_id)
);

CREATE INDEX idx_cid_distribution ON content_item_distribution (distribution_id);
CREATE INDEX idx_cid_content_item ON content_item_distribution (content_item_id);

COMMENT ON COLUMN content_item_distribution.version IS 'Optimistic locking version (JPA @Version)';
