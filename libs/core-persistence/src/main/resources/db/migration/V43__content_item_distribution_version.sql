-- Align content_item_distribution with ContentItemDistributionEntity: @Version maps to column "version"
ALTER TABLE content_item_distribution
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN content_item_distribution.version IS 'Optimistic locking version (JPA @Version)';
