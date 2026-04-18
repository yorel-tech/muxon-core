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

CREATE TABLE provider (
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

CREATE TABLE node_cluster (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
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

CREATE INDEX idx_clusters_provider ON node_cluster (provider_id);
CREATE INDEX idx_clusters_name ON node_cluster (name);
CREATE INDEX idx_node_clusters_provider_id ON node_cluster (provider_id);
CREATE INDEX idx_node_clusters_status ON node_cluster (status);

CREATE TABLE node (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cluster_id UUID REFERENCES node_cluster(id) ON DELETE SET NULL,
  provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
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

CREATE INDEX idx_nodes_provider ON node (provider_id);
CREATE INDEX idx_nodes_cluster ON node (cluster_id);
CREATE INDEX idx_nodes_name ON node (name);

CREATE TABLE datacenter (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  node_cluster_id UUID NOT NULL REFERENCES node_cluster(id),
  capacity JSONB,
  settings JSONB,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_datacenter_name ON datacenter (name);

CREATE TABLE tenant (
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

CREATE UNIQUE INDEX ux_tenant_name ON tenant (name);

INSERT INTO tenant (id, name, display_name, is_system, status)
VALUES (
    '215012d9-8b1e-5dc5-b54f-89022875fe1e',
    'system',
    'System Tenant',
    true,
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

CREATE TABLE tenant_datacenter_grant (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  datacenter_id UUID NOT NULL REFERENCES datacenter(id) ON DELETE CASCADE,
  access BOOLEAN NOT NULL DEFAULT true,
  limits JSONB,
  enabled_features TEXT[],
  override_settings JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, datacenter_id)
);

CREATE INDEX ix_tdg_tenant ON tenant_datacenter_grant (tenant_id);
CREATE INDEX ix_tdg_datacenter ON tenant_datacenter_grant (datacenter_id);

CREATE TRIGGER trg_provider_updated BEFORE UPDATE ON provider FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_datacenter_updated BEFORE UPDATE ON datacenter FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_node_cluster_updated BEFORE UPDATE ON node_cluster FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_node_updated BEFORE UPDATE ON node FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_tenant_updated BEFORE UPDATE ON tenant FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_tdg_updated BEFORE UPDATE ON tenant_datacenter_grant FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
