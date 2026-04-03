ALTER TABLE content_items
    ADD COLUMN IF NOT EXISTS provider_relative_path TEXT,
    ADD COLUMN IF NOT EXISTS infron_instance_segment VARCHAR(64);

COMMENT ON COLUMN content_items.provider_relative_path IS 'Relative path under provider storage pool (infron-.../content-libraries/...)';
COMMENT ON COLUMN content_items.infron_instance_segment IS 'infron-{instanceName}-{instanceId} segment at time path was assigned';
