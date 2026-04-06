ALTER TABLE content_libraries DROP COLUMN IF EXISTS datacenter_id;
DROP INDEX IF EXISTS idx_content_libraries_datacenter_id;

CREATE TABLE IF NOT EXISTS content_library_datacenter (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID NOT NULL REFERENCES content_libraries (id) ON DELETE CASCADE,
    datacenter_id UUID NOT NULL REFERENCES datacenter (id) ON DELETE CASCADE,
    replicate_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    last_replicated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (library_id, datacenter_id)
);

CREATE INDEX IF NOT EXISTS idx_cld_library_id ON content_library_datacenter (library_id);
CREATE INDEX IF NOT EXISTS idx_cld_datacenter_id ON content_library_datacenter (datacenter_id);

DROP TRIGGER IF EXISTS trg_content_library_datacenter_updated ON content_library_datacenter;
CREATE TRIGGER trg_content_library_datacenter_updated
    BEFORE UPDATE ON content_library_datacenter
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp_and_version();

ALTER TABLE content_items RENAME COLUMN fetch_status TO content_status;
ALTER TABLE content_items RENAME COLUMN last_fetched_at TO last_replicated_at;

UPDATE content_items SET content_status = 'replicating' WHERE content_status = 'fetching';

DROP INDEX IF EXISTS idx_content_items_fetch_status;
CREATE INDEX IF NOT EXISTS idx_content_items_content_status ON content_items (content_status);
