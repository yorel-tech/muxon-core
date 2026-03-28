-- Align storage_classes with StorageClassEntity: @Version maps to column "version"
ALTER TABLE storage_classes
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN storage_classes.version IS 'Optimistic locking version (JPA @Version)';
