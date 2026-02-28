-- V11__node_cluster_resources.sql
-- Align node and node_cluster tables with JPA entities.
-- - Add resources JSONB column to node_cluster (for NodeClusterEntity.resources)
-- - Add description, capabilities, and resources columns to node (for NodeEntity + BaseEntity)

-- Add resources column to node_cluster if missing
ALTER TABLE node_cluster
ADD COLUMN IF NOT EXISTS resources JSONB NULL;

-- Add missing columns to node table
ALTER TABLE node
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000) NULL,
    ADD COLUMN IF NOT EXISTS capabilities TEXT[] NULL,
    ADD COLUMN IF NOT EXISTS resources JSONB NULL;
