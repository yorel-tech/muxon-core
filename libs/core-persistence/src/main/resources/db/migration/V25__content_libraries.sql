CREATE TABLE IF NOT EXISTS content_libraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL,
    description TEXT,
    scope VARCHAR(16) NOT NULL,
    library_type VARCHAR(32) NOT NULL,
    access_mode VARCHAR(32) NOT NULL,
    tenant_id UUID NULL,
    source_config JSONB,
    storage_class_name VARCHAR(64),
    sync_status VARCHAR(32) NOT NULL DEFAULT 'never_synced',
    last_synced_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_content_libraries_scope ON content_libraries(scope);
CREATE INDEX IF NOT EXISTS idx_content_libraries_tenant_id ON content_libraries(tenant_id);

DROP TRIGGER IF EXISTS trg_content_libraries_updated ON content_libraries;
CREATE TRIGGER trg_content_libraries_updated
BEFORE UPDATE ON content_libraries
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

CREATE TABLE IF NOT EXISTS content_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID NOT NULL REFERENCES content_libraries(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    content_type VARCHAR(64) NOT NULL,
    version_label VARCHAR(128),
    size_bytes BIGINT,
    checksum VARCHAR(256),
    checksum_algorithm VARCHAR(32),
    source_url TEXT,
    source_item_id UUID NULL REFERENCES content_items(id) ON DELETE SET NULL,
    fetch_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    last_fetched_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_content_items_library_id ON content_items(library_id);
CREATE INDEX IF NOT EXISTS idx_content_items_content_type ON content_items(content_type);
CREATE INDEX IF NOT EXISTS idx_content_items_fetch_status ON content_items(fetch_status);

DROP TRIGGER IF EXISTS trg_content_items_updated ON content_items;
CREATE TRIGGER trg_content_items_updated
BEFORE UPDATE ON content_items
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_and_version();

ALTER TABLE vm
    ADD COLUMN IF NOT EXISTS content_item_id UUID NULL REFERENCES content_items(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_vm_content_item_id ON vm(content_item_id);
