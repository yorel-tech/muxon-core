-- V1__init.sql
-- Infron initial schema: provider objects + tenant/project/user model
-- Postgres 18 assumed. All ids are UUID.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enums
CREATE TYPE tenant_status AS ENUM ('active','inactive','suspended');
CREATE TYPE project_status AS ENUM ('active','suspended','archived');

-- Common audit fields function (optional)
-- We'll add created_at / updated_at timestamp columns with default now().

-- PROVIDER: Datacenter
CREATE TABLE datacenter (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  capacity JSONB,          -- capacity shape flexible
  settings JSONB,          -- DatacenterSettings (JSON)
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_datacenter_name ON datacenter (lower(name));

-- Provider: NodeCluster (group of hypervisors)
CREATE TABLE node_cluster (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  datacenter_id UUID REFERENCES datacenter(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_node_cluster_datacenter ON node_cluster(datacenter_id);

-- Provider: Node
CREATE TABLE node (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cluster_id UUID REFERENCES node_cluster(id) ON DELETE SET NULL,
  datacenter_id UUID REFERENCES datacenter(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  provider_type TEXT,            -- e.g., kvm, esx, xen
  endpoint TEXT,                 -- connection endpoint (hostname/ip)
  port INTEGER,
  auth JSONB,                    -- connection/auth info (encrypted at app level)
  ip_addresses TEXT[],           -- array of ip addresses
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_node_datacenter ON node(datacenter_id);
CREATE INDEX ix_node_cluster ON node(cluster_id);

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

CREATE UNIQUE INDEX ux_tenant_name ON tenant (lower(name));

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

CREATE INDEX ix_tenant_user_email ON tenant_user (lower(email));
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
  labels JSONB,
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
