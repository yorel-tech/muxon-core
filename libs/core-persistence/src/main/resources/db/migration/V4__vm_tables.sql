-- V4__vm_tables.sql
-- Add VM-related tables to support VM orchestration
-- This migration creates:
-- - VM table for VM lifecycle management
-- - compute_profile table for VM specification templates
-- - queue_entry table for database queue-based communication
-- - job table for async operation tracking
-- - entity_event table for unified audit trail

-- Create enums for VM status and power states
CREATE TYPE vm_status AS ENUM (
    'PENDING',      -- Initial state after creation request
    'PLANNED',      -- Orchestrator has planned VM
    'PROVISIONING',  -- Provider is creating VM
    'ACTIVE',       -- VM is running and accessible
    'STOPPED',      -- VM is stopped but not deleted
    'SUSPENDED',   -- VM is suspended (memory preserved to disk)
    'ERROR',        -- VM creation/operation failed
    'DELETING',    -- VM deletion in progress
    'DELETED',      -- VM is deleted
    'MIGRATING',   -- VM is being migrated between hosts
    'RESIZING'     -- VM resources are being modified
);

CREATE TYPE vm_power_state AS ENUM (
    'UNKNOWN',
    'ON',
    'OFF',
    'SUSPENDED'
);

-- Create enums for queue and job
CREATE TYPE queue_category AS ENUM (
    'COMMAND',   -- Intent events
    'STATUS',    -- Outcome events
    'AUDIT'     -- History events
);

CREATE TYPE queue_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
);

CREATE TYPE job_type AS ENUM (
    'VM_CREATE', 'VM_DELETE', 'VM_START', 'VM_STOP', 'VM_RESTART',
    'VM_SUSPEND', 'VM_RESUME', 'VM_SNAPSHOT', 'VM_BACKUP',
    'VM_MIGRATE', 'VM_RESIZE', 'NODE_PROVISION', 'NODE_DECOMMISSION',
    'PROVIDER_SYNC'
);

CREATE TYPE job_status AS ENUM (
    'PENDING',
    'RUNNING',
    'COMPLETED',
    'FAILED',
    'CANCELLED'
);

CREATE TYPE entity_type AS ENUM (
    'VM', 'NODE', 'DATACENTER', 'TENANT', 'PROVIDER', 'USER'
);

-- Create VM table
CREATE TABLE vm (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Ownership and authorization
    tenant_datacenter_grant_id UUID NOT NULL REFERENCES tenant_datacenter_grant(id) ON DELETE RESTRICT,
    
    -- VM identification
    name TEXT NOT NULL,
    description TEXT,
    
    -- VM specification (immutable after creation)
    spec JSONB NOT NULL,           -- Provider-agnostic VM spec
    
    -- Runtime state (mutable)
    status vm_status NOT NULL DEFAULT 'PENDING',
    power_state vm_power_state NOT NULL DEFAULT 'UNKNOWN',
    
    -- Provider assignment
    provider_id UUID REFERENCES provider(id) ON DELETE SET NULL,
    node_id UUID REFERENCES node(id) ON DELETE SET NULL,
    external_id TEXT,              -- ID reported by provider
    
    -- Network configuration
    ip_addresses TEXT[],            -- Array of IP addresses
    hostname TEXT,
    
    -- Resource tracking
    resource_usage JSONB,          -- Current resource utilization
    
    -- Metadata and tracking
    metadata JSONB,                -- Arbitrary VM metadata
    tags TEXT[],                    -- Searchable tags
    
    -- Lifecycle timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,       -- When VM first became ACTIVE
    stopped_at TIMESTAMPTZ,        -- Last time VM was stopped
    
    -- Constraints
    CONSTRAINT unique_vm_name_grant UNIQUE (tenant_datacenter_grant_id, name),
    CONSTRAINT unique_vm_external_provider UNIQUE (provider_id, external_id)
);

-- Indexes for VM table
CREATE INDEX idx_vm_grant ON vm(tenant_datacenter_grant_id);
CREATE INDEX idx_vm_status ON vm(status);
CREATE INDEX idx_vm_provider ON vm(provider_id);
CREATE INDEX idx_vm_node ON vm(node_id);
CREATE INDEX idx_vm_tags ON vm USING GIN(tags);

-- Create compute_profile table (VM specification templates)
CREATE TABLE compute_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Ownership
    tenant_datacenter_grant_id UUID REFERENCES tenant_datacenter_grant(id) ON DELETE CASCADE,
    
    -- Profile identification
    name TEXT NOT NULL,
    description TEXT,
    
    -- Specification
    spec JSONB NOT NULL,           -- Full VM spec with provider extensions
    
    -- Metadata
    metadata JSONB,
    tags TEXT[],
    
    -- Lifecycle
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Constraints
    CONSTRAINT unique_profile_name_grant UNIQUE (tenant_datacenter_grant_id, name)
);

-- Indexes for compute_profile
CREATE INDEX idx_compute_profile_grant ON compute_profile(tenant_datacenter_grant_id);
CREATE INDEX idx_compute_profile_tags ON compute_profile USING GIN(tags);

-- Create queue_entry table (database queue for async communication)
CREATE TABLE queue_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Queue identification
    queue_type TEXT NOT NULL,
    entity_type entity_type NOT NULL,
    entity_id UUID NOT NULL,
    queue_category queue_category NOT NULL,
    status queue_status NOT NULL DEFAULT 'PENDING',
    
    -- Queue content
    payload JSONB NOT NULL,
    
    -- Actor information
    actor_user_id UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    actor_service TEXT,
    actor_type TEXT NOT NULL DEFAULT 'SYSTEM',
    
    -- Timing and correlation
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    correlation_id TEXT,
    request_id TEXT,
    
    -- Metadata
    source TEXT NOT NULL,
    version TEXT DEFAULT '1.0',
    metadata JSONB
);

-- Indexes for queue_entry
CREATE INDEX idx_queue_entity ON queue_entry(entity_type, entity_id);
CREATE INDEX idx_queue_type ON queue_entry(queue_type);
CREATE INDEX idx_queue_category ON queue_entry(queue_category);
CREATE INDEX idx_queue_status ON queue_entry(status);
CREATE INDEX idx_queue_created ON queue_entry(created_at);
CREATE INDEX idx_queue_correlation ON queue_entry(correlation_id);

-- Composite index for polling (PENDING entries ordered by creation time)
CREATE INDEX idx_queue_polling ON queue_entry(status, created_at) 
    WHERE status = 'PENDING';

-- Create job table (generic job system for async operations)
CREATE TABLE job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Job identification
    job_type job_type NOT NULL,
    status job_status NOT NULL DEFAULT 'PENDING',
    
    -- Job target
    target_entity_type entity_type NOT NULL,
    target_entity_id UUID NOT NULL,
    
    -- Job details
    parameters JSONB,
    result JSONB,
    
    -- Execution context
    provider_id UUID REFERENCES provider(id) ON DELETE SET NULL,
    provider_operation_id TEXT,
    
    -- Timing
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    
    -- Error handling
    error_message TEXT,
    error_details JSONB,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    -- Correlation
    correlation_id TEXT,
    request_id TEXT
);

-- Indexes for job table
CREATE INDEX idx_job_target ON job(target_entity_type, target_entity_id);
CREATE INDEX idx_job_status ON job(status);
CREATE INDEX idx_job_type ON job(job_type);
CREATE INDEX idx_job_provider ON job(provider_id);
CREATE INDEX idx_job_correlation ON job(correlation_id);

-- Create entity_event table (unified audit trail for all entities)
CREATE TABLE entity_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Event identification
    event_type TEXT NOT NULL,
    entity_type entity_type NOT NULL,
    entity_id UUID NOT NULL,
    
    -- Event details
    message TEXT,
    details JSONB,
    
    -- Actor information
    actor_user_id UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    actor_service TEXT,
    actor_type TEXT NOT NULL DEFAULT 'SYSTEM',
    
    -- Timing and correlation
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    correlation_id TEXT,
    request_id TEXT,
    
    -- Metadata
    source TEXT NOT NULL,
    version TEXT DEFAULT '1.0',
    metadata JSONB
);

-- Indexes for entity_event
CREATE INDEX idx_entity_event_entity ON entity_event(entity_type, entity_id);
CREATE INDEX idx_entity_event_type ON entity_event(event_type);
CREATE INDEX idx_entity_event_created ON entity_event(created_at);
CREATE INDEX idx_entity_event_correlation ON entity_event(correlation_id);

-- Add triggers for updated_at
CREATE TRIGGER trg_vm_updated BEFORE UPDATE ON vm FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_compute_profile_updated BEFORE UPDATE ON compute_profile FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_queue_entry_updated BEFORE UPDATE ON queue_entry FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_job_updated BEFORE UPDATE ON job FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_entity_event_updated BEFORE UPDATE ON entity_event FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- Add VM permissions to permission table
DO $$
DECLARE
ns CONSTANT uuid := '696e6672-6f6e-636f-7265-111111111111';
BEGIN
    INSERT INTO permission (id, action, description, scope)
    VALUES
        (uuid_generate_v5(ns, 'vm:create'), 'vm:create', 'Create VM', 'TENANT'),
        (uuid_generate_v5(ns, 'vm:read'), 'vm:read', 'Read VM', 'TENANT'),
        (uuid_generate_v5(ns, 'vm:edit'), 'vm:edit', 'Edit VM', 'TENANT'),
        (uuid_generate_v5(ns, 'vm:manage'), 'vm:manage', 'Manage VM', 'TENANT'),
        (uuid_generate_v5(ns, 'vm:console'), 'vm:console', 'View VM console', 'TENANT')
    ON CONFLICT (action) DO NOTHING;
END$$;
