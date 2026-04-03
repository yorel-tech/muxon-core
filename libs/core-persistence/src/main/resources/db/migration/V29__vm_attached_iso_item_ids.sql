ALTER TABLE vm ADD COLUMN IF NOT EXISTS attached_iso_item_ids UUID[];

CREATE INDEX IF NOT EXISTS idx_vm_attached_iso_item_ids ON vm USING GIN(attached_iso_item_ids);

COMMENT ON COLUMN vm.attached_iso_item_ids IS 'Content library ISO items currently attached to this VM';
