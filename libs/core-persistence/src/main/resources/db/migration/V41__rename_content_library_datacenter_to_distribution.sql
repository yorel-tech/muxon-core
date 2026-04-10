ALTER TABLE content_library_datacenter
    RENAME TO content_library_distribution;

ALTER TABLE content_library_distribution
    ADD COLUMN progress_percent INT NOT NULL DEFAULT 0
        CHECK (progress_percent BETWEEN 0 AND 100),
    ADD COLUMN error_message TEXT;
