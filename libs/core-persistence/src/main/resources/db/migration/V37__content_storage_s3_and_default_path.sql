-- Default path from Flyway placeholder (Spring: spring.flyway.placeholders.contentStorageDefaultPath)
UPDATE content_storage
SET config = jsonb_set(config, '{path}', to_jsonb('${contentStorageDefaultPath}'::text))
WHERE is_default = true
  AND (config->>'path' IS NULL OR config->>'path' = '');

ALTER TABLE content_storage
    DROP CONSTRAINT chk_content_storage_type;

ALTER TABLE content_storage
    ADD CONSTRAINT chk_content_storage_type
        CHECK (storage_type IN ('local', 'nfs', 's3'));
