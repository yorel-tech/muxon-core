-- V3__user_role_bindings_view.sql
-- Create a view that combines tenant_user and role_binding details
-- Also create identity_provider table for OIDC, OAuth2.0, and SAML 2.0 support


-- Create enum for identity provider protocols
CREATE TYPE idp_protocol AS ENUM ('OIDC', 'OAUTH2');

-- Identity Provider table
CREATE TABLE IF NOT EXISTS identity_provider (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    protocol idp_protocol NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    metadata JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Ensure unique names within a tenant
ALTER TABLE identity_provider
    ADD CONSTRAINT uq_identity_protocol UNIQUE (protocol);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_identity_provider_protocol ON identity_provider(protocol);
CREATE INDEX IF NOT EXISTS idx_identity_provider_enabled ON identity_provider(enabled);

-- Users (OIDC)
-- We store both internal user id and OIDC identifiers to map identity provider info.
CREATE TABLE IF NOT EXISTS idp_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  identity_provider_id UUID NOT NULL REFERENCES identity_provider(id) ON DELETE CASCADE,
  external_id TEXT,              -- OIDC sub (subject)
  username TEXT,                 -- preferred username
  email TEXT,
  display_name TEXT,
  metadata JSONB,                -- extra profile claims
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (identity_provider_id, external_id)
);

CREATE INDEX IF NOT EXISTS ix_idp_user_email ON idp_user (email);
CREATE INDEX IF NOT EXISTS ix_idp_user_external ON idp_user (identity_provider_id, external_id);

-- create view for user role mapping
CREATE OR REPLACE VIEW user_role_bindings AS
SELECT
    tu.id as user_id,
    tu.external_id,
    tu.identity_provider_id,
    tu.username,
    tu.email,
    tu.display_name,
    tu.metadata as user_metadata,
    tu.created_at as user_created_at,
    tu.updated_at as user_updated_at,
    rb.id as binding_id,
    rb.role_id,
    rb.subject_type,
    rb.subject_id,
    rb.scope_type,
    rb.scope_id,
    rb.expires_at,
    rb.created_by,
    rb.created_at as binding_created_at,
    r.name as role_name,
    r.description as role_description,
    r.scope_type as role_scope_type,
    r.scope_id as role_scope_id
FROM idp_user tu
JOIN role_binding rb ON tu.id::text = rb.subject_id AND rb.subject_type = 'USER'
LEFT JOIN role r ON rb.role_id = r.id;

-- Audit-ish small support tables (optional)
CREATE TABLE audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID REFERENCES tenant(id),
  actor_user_id UUID REFERENCES idp_user(id),
  action TEXT NOT NULL,
  payload JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Project
CREATE TABLE project (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  tenant_datacenter_grant_id UUID REFERENCES tenant_datacenter_grant(id) ON DELETE SET NULL,
  name TEXT NOT NULL,
  display_name TEXT,
  description TEXT,
  status project_status NOT NULL DEFAULT 'ACTIVE',
  owner UUID REFERENCES idp_user(id), -- owner user id
  resource_limits JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, name)
);

CREATE INDEX ix_project_tenant ON project (tenant_id);

-- System init table
CREATE TABLE IF NOT EXISTS system_init (
    primary_key TEXT PRIMARY KEY,
    value TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Add updated_at trigger for system_init
CREATE TRIGGER trg_system_init_updated BEFORE UPDATE ON system_init FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- Add updated_at trigger for identity_provider
CREATE TRIGGER trg_identity_provider_updated BEFORE UPDATE ON identity_provider FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- Add updated_at trigger for idp_user
CREATE TRIGGER trg_idp_user_updated BEFORE UPDATE ON idp_user FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER trg_project_updated BEFORE UPDATE ON project FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
