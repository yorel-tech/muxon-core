-- Add system_status column to system_init table for tracking bootstrap lifecycle
-- This migration adds support for three-state bootstrap lifecycle: NOTREADY -> BOOTSTRAPPED -> READY

-- Create bootstrap_status ENUM type
CREATE TYPE bootstrap_status AS ENUM ('NOTREADY', 'BOOTSTRAPPED', 'READY');

-- Add system_status column to system_init table
ALTER TABLE system_init 
ADD COLUMN system_status bootstrap_status NOT NULL DEFAULT 'NOTREADY';

-- Add check constraint to ensure only valid enum values
ALTER TABLE system_init 
ADD CONSTRAINT check_system_status 
CHECK (system_status IN ('NOTREADY', 'BOOTSTRAPPED', 'READY'));

-- Insert initial NOTREADY status row if it doesn't exist
INSERT INTO system_init (primary_key, value, system_status, updated_at)
SELECT 'bootstrap_status', 'false', 'NOTREADY', now()
WHERE NOT EXISTS (
    SELECT 1 FROM system_init WHERE primary_key = 'bootstrap_status'
);
