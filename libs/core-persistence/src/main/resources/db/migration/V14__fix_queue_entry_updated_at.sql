-- Fix queue_entry table to add missing updated_at column
-- This fixes the error: record "new" has no field "updated_at"

-- Add the missing updated_at column
ALTER TABLE queue_entry 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Create an index on updated_at for better query performance
CREATE INDEX IF NOT EXISTS idx_queue_updated ON queue_entry(updated_at);

-- Update the trigger to ensure it works correctly
-- The trigger_set_timestamp function should already exist from V1__init.sql
-- But we'll recreate it to be safe
DROP FUNCTION IF EXISTS trigger_set_timestamp() CASCADE;

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Ensure the trigger exists and is properly attached
DROP TRIGGER IF EXISTS trg_queue_entry_updated ON queue_entry;

CREATE TRIGGER trg_queue_entry_updated 
BEFORE UPDATE ON queue_entry 
FOR EACH ROW 
EXECUTE FUNCTION trigger_set_timestamp();
