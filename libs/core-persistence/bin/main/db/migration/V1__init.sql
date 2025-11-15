-- V1__init.sql
-- Infron initial schema: provider objects + tenant/project/user model
-- Postgres 18 assumed. All ids are UUID.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enums
CREATE TYPE tenant_status AS ENUM ('active','inactive','suspended');
CREATE TYPE project_status AS ENUM ('active','suspended','archived');
CREATE TYPE provider_type AS ENUM ('proxmox', 'libvirt', 'kubernetes');

-- Common audit fields function (optional)
-- We'll add created_at / updated_at timestamp columns with default now().

-- Providers: management endpoints (vcenter, proxmox, libvirt, kubernetes etc.)
CREATE TABLE IF NOT EXISTS provider (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  type provider_type NOT NULL, -- e.g. vcenter, proxmox, libvirt, kubernetes, aws
  endpoint TEXT NOT NULL, -- base API URL or connection string
  credentials JSONB NULL,  -- encrypted or opaque credential blob; consider external secrets store
  capabilities JSONB NULL, -- e.g. {"vm": true, "k8s": true, "snapshot": true}
  metadata JSONB NULL,     -- arbitrary provider-specific metadata
  status TEXT NOT NULL DEFAULT 'UNKNOWN', -- CONNECTED, DISCONNECTED, UNKNOWN
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- PROVIDER: Datacenter
CREATE TABLE datacenter (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  node_cluster_id UUID NOT NULL REFERENCES node_cluster(id)
  capacity JSONB,          -- capacity shape flexible
  settings JSONB,          -- DatacenterSettings (JSON)
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_datacenter_name ON datacenter(name);

-- Provider: NodeCluster (group of hypervisors)
CREATE TABLE node_cluster (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
  external_id TEXT NULL,  -- id reported by provider (cluster id)
  name TEXT NOT NULL,
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
  status TEXT NOT NULL DEFAULT 'UNKNOWN', -- READY, DOWN, MAINTENANCE
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

-- TENANT model
CREATE TABLE tenant (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,            -- canonical name
  display_name TEXT,
  status tenant_status NOT NULL DEFAULT 'active',
  metadata JSONB,
  settings JSONB,                -- TenantSettings JSON
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_tenant_name ON tenant (name);

-- Users (OIDC)
-- We store both internal user id and OIDC identifiers to map identity provider info.
CREATE TABLE tenant_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  external_id TEXT,              -- OIDC sub (subject)
  external_issuer TEXT,          -- OIDC issuer URL / idp id
  username TEXT,                 -- preferred username
  email TEXT,
  display_name TEXT,
  metadata JSONB,                -- extra profile claims
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (external_issuer, external_id)
);

CREATE INDEX ix_tenant_user_email ON tenant_user (email);
CREATE INDEX ix_tenant_user_external ON tenant_user (external_issuer, external_id);

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

-- Project
CREATE TABLE project (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  tenant_datacenter_grant_id UUID REFERENCES tenant_datacenter_grant(id) ON DELETE SET NULL,
  name TEXT NOT NULL,
  display_name TEXT,
  description TEXT,
  status project_status NOT NULL DEFAULT 'active',
  owner UUID REFERENCES tenant_user(id), -- owner user id
  resource_limits JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, name)
);

CREATE INDEX ix_project_tenant ON project (tenant_id);

-- Roles (simple role vocabulary) - keep flexible
CREATE TABLE role (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL UNIQUE,  -- e.g. TENANT_ADMIN, TENANT_USER, PROJECT_ADMIN, PROJECT_MEMBER
  description TEXT
);

-- ProjectMember: mapping between tenant_user and project with a role
CREATE TABLE project_member (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES tenant_user(id) ON DELETE CASCADE,
  role_id UUID NOT NULL REFERENCES role(id) ON DELETE RESTRICT,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (project_id, user_id)
);

CREATE INDEX ix_pm_project ON project_member (project_id);
CREATE INDEX ix_pm_user ON project_member (user_id);

-- Tenant-level user roles: mapping users -> tenant -> role(s)
CREATE TABLE tenant_user_role (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES tenant_user(id) ON DELETE CASCADE,
  role_id UUID NOT NULL REFERENCES role(id) ON DELETE RESTRICT,
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, user_id, role_id)
);

CREATE INDEX ix_tur_tenant ON tenant_user_role (tenant_id);
CREATE INDEX ix_tur_user ON tenant_user_role (user_id);

-- Audit-ish small support tables (optional)
CREATE TABLE audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID REFERENCES tenant(id),
  actor_user_id UUID REFERENCES tenant_user(id),
  action TEXT NOT NULL,
  payload JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed some basic roles (you can expand)
INSERT INTO role (id, name, description) VALUES
  (gen_random_uuid(), 'TENANT_ADMIN', 'Manage tenant-level resources'),
  (gen_random_uuid(), 'TENANT_USER', 'Regular tenant user'),
  (gen_random_uuid(), 'PROJECT_ADMIN', 'Manage project-level resources'),
  (gen_random_uuid(), 'PROJECT_MEMBER', 'Project member');

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
CREATE TRIGGER trg_project_updated BEFORE UPDATE ON project FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_tenant_user_updated BEFORE UPDATE ON tenant_user FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- END of migration
