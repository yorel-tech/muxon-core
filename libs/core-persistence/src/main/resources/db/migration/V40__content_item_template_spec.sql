-- V40__content_item_template_spec.sql
-- Add VM template spec JSONB column to content_items.
-- This is only populated when content_type='vm_template'. Other content types keep it NULL.

ALTER TABLE content_items
    ADD COLUMN IF NOT EXISTS template_spec JSONB;

-- Fast JSONB querying for vm_template template spec.
CREATE INDEX IF NOT EXISTS idx_content_items_template_spec_gin
    ON content_items USING GIN (template_spec jsonb_path_ops)
    WHERE content_type = 'vm_template' AND template_spec IS NOT NULL;

-- Common filter helpers (optional but cheap).
CREATE INDEX IF NOT EXISTS idx_content_items_template_os_family
    ON content_items ((template_spec->'metadata'->>'osFamily'))
    WHERE content_type = 'vm_template' AND template_spec IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_content_items_template_firmware
    ON content_items ((template_spec->'spec'->>'firmware'))
    WHERE content_type = 'vm_template' AND template_spec IS NOT NULL;

