-- Content storage backends (local path or NFS mount path on core-services host).
CREATE TABLE content_storage (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(128) NOT NULL,
    storage_type    VARCHAR(32) NOT NULL,
    config          JSONB NOT NULL DEFAULT '{}',
    is_default      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_content_storage_type CHECK (storage_type IN ('local', 'nfs'))
);

CREATE UNIQUE INDEX uq_content_storage_single_default ON content_storage (is_default)
    WHERE is_default = true;

CREATE INDEX idx_content_storage_name ON content_storage (name);

ALTER TABLE content_libraries
    ADD COLUMN content_storage_id UUID REFERENCES content_storage (id);

-- One default row; empty path means runtime falls back to java tmp / infron property.
INSERT INTO content_storage (name, storage_type, config, is_default)
VALUES (
    'Default content store',
    'local',
    '{"path":""}'::jsonb,
    true
);

UPDATE content_libraries cl
SET content_storage_id = (SELECT id FROM content_storage WHERE is_default = true LIMIT 1)
WHERE cl.content_storage_id IS NULL;

ALTER TABLE content_libraries
    ALTER COLUMN content_storage_id SET NOT NULL;
