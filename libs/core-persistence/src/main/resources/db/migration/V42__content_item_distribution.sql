CREATE TABLE content_item_distribution (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id   UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    distribution_id   UUID NOT NULL REFERENCES content_library_distribution(id) ON DELETE CASCADE,
    status            TEXT NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','COPYING','READY','FAILED')),
    checksum_verified BOOLEAN NOT NULL DEFAULT FALSE,
    size_bytes        BIGINT,
    error_message     TEXT,
    retry_count       INT NOT NULL DEFAULT 0,
    last_updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (content_item_id, distribution_id)
);

CREATE INDEX idx_cid_distribution ON content_item_distribution(distribution_id);
CREATE INDEX idx_cid_content_item ON content_item_distribution(content_item_id);
