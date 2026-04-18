-- Foundation: tenancy, provider topology, datacenter grants (merged V1, V9, V10, V11; provider_type without KUBERNETES)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE tenant_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
CREATE TYPE provider_type AS ENUM ('PROXMOX', 'LIBVIRT');
CREATE TYPE node_status AS ENUM ('UNKNOWN', 'READY', 'DOWN', 'MAINTENANCE');
CREATE TYPE node_cluster_status AS ENUM ('UNKNOWN', 'READY', 'DEGRADED', 'MAINTENANCE', 'INACTIVE');

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE providers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  type provider_type NOT NULL,
  endpoint TEXT NOT NULL,
  credentials JSONB NULL,
  capabilities JSONB NULL,
  metadata JSONB NULL,
  description VARCHAR(1000) NULL,
  status TEXT NOT NULL DEFAULT 'UNKNOWN',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE node_clusters (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
  external_id TEXT NULL,
  capacity JSONB NULL,
  capabilities JSONB NULL,
  resources JSONB NULL,
  metadata JSONB,
  status node_cluster_status NOT NULL DEFAULT 'UNKNOWN',
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE (provider_id, external_id)
);

CREATE INDEX idx_clusters_provider ON node_clusters (provider_id);
CREATE INDEX idx_clusters_name ON node_clusters (name);
CREATE INDEX idx_node_clusters_provider_id ON node_clusters (provider_id);
CREATE INDEX idx_node_clusters_status ON node_clusters (status);

CREATE TABLE nodes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cluster_id UUID REFERENCES node_clusters(id) ON DELETE SET NULL,
  provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
  external_id TEXT NULL,
  name TEXT NOT NULL,
  description VARCHAR(1000) NULL,
  role TEXT NULL,
  cpu_total INTEGER NULL,
  mem_mb INTEGER NULL,
  capabilities TEXT[] NULL,
  resources JSONB NULL,
  status node_status NOT NULL DEFAULT 'UNKNOWN',
  last_seen_at TIMESTAMPTZ NULL,
  credentials JSONB NULL,
  ip_addresses TEXT[],
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_nodes_provider ON nodes (provider_id);
CREATE INDEX idx_nodes_cluster ON nodes (cluster_id);
CREATE INDEX idx_nodes_name ON nodes (name);

CREATE TABLE datacenters (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  node_cluster_id UUID NOT NULL REFERENCES node_clusters(id),
  capacity JSONB,
  settings JSONB,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_datacenter_name ON datacenters (name);

CREATE TABLE tenants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  display_name TEXT,
  is_system BOOLEAN NOT NULL DEFAULT false,
  status tenant_status NOT NULL DEFAULT 'ACTIVE',
  metadata JSONB,
  settings JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_tenant_name ON tenants (name);

INSERT INTO tenants (id, name, display_name, is_system, status)
VALUES (
    '215012d9-8b1e-5dc5-b54f-89022875fe1e',
    'system',
    'System Tenant',
    true,
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

CREATE TABLE tenant_datacenter_grants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  datacenter_id UUID NOT NULL REFERENCES datacenters(id) ON DELETE CASCADE,
  access BOOLEAN NOT NULL DEFAULT true,
  limits JSONB,
  enabled_features TEXT[],
  override_settings JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, datacenter_id)
);

CREATE INDEX ix_tdg_tenant ON tenant_datacenter_grants (tenant_id);
CREATE INDEX ix_tdg_datacenter ON tenant_datacenter_grants (datacenter_id);

CREATE TRIGGER trg_provider_updated BEFORE UPDATE ON providers FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_datacenter_updated BEFORE UPDATE ON datacenters FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_node_cluster_updated BEFORE UPDATE ON node_clusters FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_node_updated BEFORE UPDATE ON nodes FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_tenant_updated BEFORE UPDATE ON tenants FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_tdg_updated BEFORE UPDATE ON tenant_datacenter_grants FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
