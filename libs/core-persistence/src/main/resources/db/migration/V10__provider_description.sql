-- V10__provider_description.sql
-- Add description column to provider table (BaseEntity defines it; table was missing it)

ALTER TABLE provider
ADD COLUMN IF NOT EXISTS description VARCHAR(1000) NULL;
