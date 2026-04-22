-- Guest customization columns on vms; no new tables required.
-- content_items.content_type is TEXT so 'script' items need no schema change.

ALTER TABLE vms
    ADD COLUMN IF NOT EXISTS customization       JSONB,
    ADD COLUMN IF NOT EXISTS customization_status JSONB,
    ADD COLUMN IF NOT EXISTS customization_seed_path VARCHAR(1024);

COMMENT ON COLUMN vms.customization IS
    'Encrypted guest customization spec (AES-GCM). Secrets (passwords) are field-level encrypted; non-secret fields stored plain.';

COMMENT ON COLUMN vms.customization_status IS
    'JSON object tracking customization phase, sub-phase, timestamps and last message. '
    'Phase values: NONE, PENDING, SEED_BUILT, WAITING_AGENT, IN_PROGRESS, COMPLETE, FAILED.';

COMMENT ON COLUMN vms.customization_seed_path IS
    'Provider-relative path of the seed ISO artifact. Cleared when customization reaches COMPLETE or FAILED.';

CREATE INDEX IF NOT EXISTS idx_vm_customization_phase
    ON vms ((customization_status->>'phase'))
    WHERE customization_status IS NOT NULL;
