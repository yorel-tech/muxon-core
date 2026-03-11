-- Add audit and error columns to queue_entry (entity expects created_by, updated_by, error_message)
ALTER TABLE queue_entry
    ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS error_message TEXT;
