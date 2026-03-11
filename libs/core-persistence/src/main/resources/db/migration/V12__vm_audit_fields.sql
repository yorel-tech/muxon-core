-- V12__vm_audit_fields.sql
-- Add audit and versioning columns to vm table to align with VmEntity
-- - created_by: user who created the VM
-- - updated_by: user who last updated the VM
-- - version: optimistic locking/version field

ALTER TABLE vm
    ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES idp_user(id),
    ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES idp_user(id),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

