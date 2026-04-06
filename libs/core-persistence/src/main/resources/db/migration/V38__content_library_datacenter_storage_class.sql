ALTER TABLE content_library_datacenter
    ADD COLUMN IF NOT EXISTS storage_class_name VARCHAR(128);

COMMENT ON COLUMN content_library_datacenter.storage_class_name IS
    'Datacenter storage class used to select provider pools for replication; set at publish time.';
