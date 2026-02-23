-- V9__nodes_audit.sql
-- Merged migration: node clusters, nodes, and audit log with proper enum types

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

-- Node cluster status enum (for node_cluster table)
-- Values: UNKNOWN - status cannot be determined
--         READY - cluster is operational and accepting workloads
--         DEGRADED - cluster is operational but with reduced capacity
--         MAINTENANCE - cluster is under maintenance
--         INACTIVE - cluster is not active
CREATE TYPE node_cluster_status AS ENUM ('UNKNOWN', 'READY', 'DEGRADED', 'MAINTENANCE', 'INACTIVE');

-- Note: node_status enum already exists from V1__init.sql with values:
-- ('UNKNOWN', 'READY', 'DOWN', 'MAINTENANCE')

-- ============================================================================
-- NODE CLUSTER ENHANCEMENTS
-- ============================================================================

-- Update node_cluster status column to use enum type
-- First update any existing VARCHAR values to valid enum values, then alter column type
ALTER TABLE node_cluster 
ALTER COLUMN status TYPE node_cluster_status 
USING CASE 
    WHEN status = 'READY' THEN 'READY'::node_cluster_status
    WHEN status = 'DEGRADED' THEN 'DEGRADED'::node_cluster_status
    WHEN status = 'MAINTENANCE' THEN 'MAINTENANCE'::node_cluster_status
    WHEN status = 'INACTIVE' THEN 'INACTIVE'::node_cluster_status
    ELSE 'UNKNOWN'::node_cluster_status
END;

-- Set default for node_cluster status
ALTER TABLE node_cluster 
ALTER COLUMN status SET DEFAULT 'UNKNOWN'::node_cluster_status;

-- Indexes for node_cluster
CREATE INDEX IF NOT EXISTS idx_node_clusters_provider_id ON node_cluster(provider_id);
CREATE INDEX IF NOT EXISTS idx_node_clusters_status ON node_cluster(status);

-- ============================================================================
-- NODE ENHANCEMENTS
-- ============================================================================

-- Ensure node table uses the node_status enum properly with default
ALTER TABLE node 
ALTER COLUMN status SET DEFAULT 'UNKNOWN'::node_status;

-- Indexes for node (these should already exist from V1, but ensure they do)
CREATE INDEX IF NOT EXISTS idx_nodes_provider ON node(provider_id);
CREATE INDEX IF NOT EXISTS idx_nodes_cluster ON node(cluster_id);

-- ============================================================================
-- AUDIT LOG TABLE
-- ============================================================================

-- Audit log table for tracking all actions
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID,
    request_data JSONB,
    response_status INTEGER,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for audit_log
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource ON audit_log(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Trigger function for node_cluster updated_at (if not already exists from V1)
CREATE OR REPLACE FUNCTION trigger_set_node_cluster_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger if exists and recreate
DROP TRIGGER IF EXISTS trigger_set_node_cluster_updated_at ON node_cluster;
CREATE TRIGGER trigger_set_node_cluster_updated_at 
BEFORE UPDATE ON node_cluster 
FOR EACH ROW 
EXECUTE FUNCTION trigger_set_node_cluster_updated_at();
