ALTER TABLE content_libraries
    ADD COLUMN IF NOT EXISTS datacenter_id UUID NULL REFERENCES datacenter (id);

CREATE INDEX IF NOT EXISTS idx_content_libraries_datacenter_id ON content_libraries (datacenter_id);
