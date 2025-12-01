-- V1__init.sql
-- Infron initial schema: provider objects + tenant/project/user model
-- Postgres 18 assumed. All ids are UUID.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enums
CREATE TYPE tenant_status AS ENUM ('ACTIVE','INACTIVE','SUSPENDED');
CREATE TYPE project_status AS ENUM ('ACTIVE','SUSPENDED','ARCHIVED');
CREATE TYPE provider_type AS ENUM ('PROXMOX', 'LIBVIRT', 'KUBERNETES');
CREATE TYPE node_status AS ENUM ('UNKNOWN', 'READY', 'DOWN', 'MAINTENANCE');

-- Common audit fields function (optional)
-- We'll add created_at / updated_at timestamp columns with default now().

-- Providers: management endpoints (vcenter, proxmox, libvirt, kubernetes etc.)
CREATE TABLE IF NOT EXISTS provider (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  type provider_type NOT NULL, -- e.g. proxmox, libvirt, kubernetes, aws
  endpoint TEXT NOT NULL, -- base API URL or connection string
  credentials JSONB NULL,  -- encrypted or opaque credential blob; consider external secrets store
  capabilities JSONB NULL, -- e.g. {"vm": true, "k8s": true, "snapshot": true}
  metadata JSONB NULL,     -- arbitrary provider-specific metadata
  status TEXT NOT NULL DEFAULT 'UNKNOWN', -- CONNECTED, DISCONNECTED, UNKNOWN
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Provider: NodeCluster (group of hypervisors)
CREATE TABLE node_cluster (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
  external_id TEXT NULL,  -- id reported by provider (cluster id)
  capacity JSONB NULL,    -- e.g. {"cpu_total":32000, "mem_mb":262144}
  capabilities JSONB NULL,
  metadata JSONB,
  status TEXT NOT NULL DEFAULT 'UNKNOWN', -- READY, DEGRADED, MAINTENANCE
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(provider_id, external_id)
);

CREATE INDEX IF NOT EXISTS idx_clusters_provider ON node_cluster(provider_id);
CREATE INDEX IF NOT EXISTS idx_clusters_name ON node_cluster(name);

-- Provider: Node
CREATE TABLE node (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cluster_id UUID REFERENCES node_cluster(id) ON DELETE SET NULL,
  provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
  external_id TEXT NULL,   -- id on provider
  name TEXT NOT NULL,
  role TEXT NULL,          -- hypervisor | worker | control
  cpu_total INTEGER NULL,  -- number of vCPUs or cores
  mem_mb INTEGER NULL,     -- total memory in MB
  status node_status NOT NULL DEFAULT 'UNKNOWN',
  last_seen_at TIMESTAMPTZ NULL,
  credentials JSONB NULL,  -- encrypted or opaque credential blob; consider external secrets store
  ip_addresses TEXT[],           -- array of ip addresses
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_nodes_provider ON node(provider_id);
CREATE INDEX IF NOT EXISTS idx_nodes_cluster ON node(cluster_id);
CREATE INDEX IF NOT EXISTS idx_nodes_name ON node(name);

-- PROVIDER: Datacenter
CREATE TABLE datacenter (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  node_cluster_id UUID NOT NULL REFERENCES node_cluster(id),
  capacity JSONB,          -- capacity shape flexible
  settings JSONB,          -- DatacenterSettings (JSON)
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_datacenter_name ON datacenter(name);

-- TENANT model
CREATE TABLE tenant (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,            -- canonical name
  display_name TEXT,
  is_system BOOLEAN NOT NULL DEFAULT false,
  status tenant_status NOT NULL DEFAULT 'ACTIVE',
  metadata JSONB,
  settings JSONB,                -- TenantSettings JSON
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_tenant_name ON tenant (name);

-- Seed system tenant
INSERT INTO tenant (id, name, display_name, is_system, status)
VALUES (
    '215012d9-8b1e-5dc5-b54f-89022875fe1e',
    'system',
    'System Tenant',
    true,
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;



-- TenantDatacenterGrant
CREATE TABLE tenant_datacenter_grant (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  datacenter_id UUID NOT NULL REFERENCES datacenter(id) ON DELETE CASCADE,
  access BOOLEAN NOT NULL DEFAULT true,
  limits JSONB,                  -- ResourceLimits
  enabled_features TEXT[],       -- array of enum strings (DatacenterFeature)
  override_settings JSONB,       -- DatacenterSettings
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, datacenter_id)
);

CREATE INDEX ix_tdg_tenant ON tenant_datacenter_grant (tenant_id);
CREATE INDEX ix_tdg_datacenter ON tenant_datacenter_grant (datacenter_id);


-- Triggers to keep updated_at maintained (example)
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach triggers to tables with updated_at
CREATE TRIGGER trg_datacenter_updated BEFORE UPDATE ON datacenter FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_node_cluster_updated BEFORE UPDATE ON node_cluster FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_node_updated BEFORE UPDATE ON node FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_tenant_updated BEFORE UPDATE ON tenant FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_tdg_updated BEFORE UPDATE ON tenant_datacenter_grant FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- END of migration
