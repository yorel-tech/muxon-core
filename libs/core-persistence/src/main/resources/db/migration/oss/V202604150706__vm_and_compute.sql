-- VM and compute profiles (merged V4 VM parts, V12, V25 vm FK, V29; runs after content_items)

CREATE TYPE vm_status AS ENUM (
    'PENDING',
    'PLANNED',
    'PROVISIONING',
    'ACTIVE',
    'STOPPED',
    'SUSPENDED',
    'ERROR',
    'DELETING',
    'DELETED',
    'MIGRATING',
    'RESIZING'
);

CREATE TYPE vm_power_state AS ENUM (
    'UNKNOWN',
    'ON',
    'OFF',
    'SUSPENDED'
);

CREATE TABLE vm (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_datacenter_grant_id UUID NOT NULL REFERENCES tenant_datacenter_grants (id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    description TEXT,
    spec JSONB NOT NULL,
    status vm_status NOT NULL DEFAULT 'PENDING',
    power_state vm_power_state NOT NULL DEFAULT 'UNKNOWN',
    provider_id UUID REFERENCES providers (id) ON DELETE SET NULL,
    node_id UUID REFERENCES nodes (id) ON DELETE SET NULL,
    external_id TEXT,
    ip_addresses TEXT[],
    hostname TEXT,
    resource_usage JSONB,
    metadata JSONB,
    tags TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    stopped_at TIMESTAMPTZ,
    created_by UUID REFERENCES idp_users (id),
    updated_by UUID REFERENCES idp_users (id),
    version BIGINT NOT NULL DEFAULT 0,
    content_item_id UUID NULL REFERENCES content_items (id) ON DELETE SET NULL,
    attached_iso_item_ids UUID[],
    CONSTRAINT unique_vm_name_grant UNIQUE (tenant_datacenter_grant_id, name),
    CONSTRAINT unique_vm_external_provider UNIQUE (provider_id, external_id)
);

CREATE INDEX idx_vm_grant ON vm (tenant_datacenter_grant_id);
CREATE INDEX idx_vm_status ON vm (status);
CREATE INDEX idx_vm_provider ON vm (provider_id);
CREATE INDEX idx_vm_node ON vm (node_id);
CREATE INDEX idx_vm_tags ON vm USING GIN (tags);
CREATE INDEX idx_vm_content_item_id ON vm (content_item_id);
CREATE INDEX idx_vm_attached_iso_item_ids ON vm USING GIN (attached_iso_item_ids);

COMMENT ON COLUMN vm.attached_iso_item_ids IS 'Content library ISO items currently attached to this VM';

CREATE TABLE compute_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_datacenter_grant_id UUID REFERENCES tenant_datacenter_grants (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    spec JSONB NOT NULL,
    metadata JSONB,
    tags TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_profile_name_grant UNIQUE (tenant_datacenter_grant_id, name)
);

CREATE INDEX idx_compute_profile_grant ON compute_profile (tenant_datacenter_grant_id);
CREATE INDEX idx_compute_profile_tags ON compute_profile USING GIN (tags);

CREATE TRIGGER trg_vm_updated BEFORE UPDATE ON vm FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_compute_profile_updated BEFORE UPDATE ON compute_profile FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
