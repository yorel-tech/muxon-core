-- =====================================================================
--  Core RBAC Schema - Flyway Migration V2
-- =====================================================================
-- This migration introduces:
--   - roles
--   - permissions (canonical)
--   - role_permissions (mapping)
--   - role_bindings
--   - seed builtin roles & permissions
--
-- IMPORTANT: This migration is Core-only and contains no Nexus schema.
-- =====================================================================
CREATE TYPE role_scope AS ENUM ('SYSTEM','TENANT','TENANT_GLOBAL', 'PROJECT');
CREATE TYPE subject_type AS ENUM ('USER','GROUP','SERVICE_ACCOUNT');
-----------------------------------------------------------------------
-- 1. Permissions (canonical)
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action TEXT NOT NULL UNIQUE,     -- e.g. "vm:create", "provider:read"
    description TEXT,
    scope role_scope NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-----------------------------------------------------------------------
-- 2. Roles
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,        -- e.g. "system:admin", "tenant:admin"
    description TEXT,
    scope_type role_scope NOT NULL DEFAULT 'SYSTEM',
    scope_id UUID NULL,
    immutable BOOLEAN DEFAULT FALSE,  -- prevents UI/API from editing/deleting
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-----------------------------------------------------------------------
-- Scope examples
-- scope_type = 'system' — system-only roles (Core/system operators). scope_id MUST be NULL.
-- scope_type = 'tenant_global' — tenant-global roles that are available to all tenants (e.g. tenant:admin seeded by sysadmin); scope_id MUST be NULL.
-- scope_type = 'tenant' — tenant-scoped roles created by a tenant admin for a specific tenant; scope_id = that tenant's UUID.
-- scope_type = 'project' — project-scoped roles; scope_id = project UUID.
-----------------------------------------------------------------------
-----------------------------------------------------------------------
-- 3. Role → Permission mapping
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Useful index for permission resolution
CREATE INDEX IF NOT EXISTS idx_role_permission_role
    ON role_permission(role_id);

-- Each permission should not be duplicated inside a role
ALTER TABLE role_permission
    ADD CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id);

-----------------------------------------------------------------------
-- 4. Role Bindings (assign roles to subjects)
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_binding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    subject_type subject_type NOT NULL DEFAULT 'USER',    -- 'user' | 'group' | 'service_account'
    subject_id TEXT NOT NULL,      -- user UUID OR external group ID (string)
    scope_type role_scope NOT NULL DEFAULT 'SYSTEM', -- 'system' | 'tenant' | 'project'
    scope_id UUID,                 -- null for system scope
    expires_at TIMESTAMPTZ,        -- optional time-limited grant
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for fast permission resolution
CREATE INDEX IF NOT EXISTS idx_role_bindings_role
    ON role_binding(role_id);

CREATE INDEX IF NOT EXISTS idx_role_bindings_subject
    ON role_binding(subject_type, subject_id);

CREATE INDEX IF NOT EXISTS idx_role_bindings_scope
    ON role_binding(scope_type, scope_id);

-- Prevent duplicate bindings
ALTER TABLE role_binding
    ADD CONSTRAINT uq_role_binding UNIQUE (
        role_id, subject_type, subject_id, scope_type, scope_id
    );

-----------------------------------------------------------------------
-- 5. Seed builtin permissions
-----------------------------------------------------------------------
-- Ensure uuid-ossp for uuid_generate_v5
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Choose a stable namespace UUID for your product (keep this constant)
-- Replace namespace below with your own chosen UUID if desired.
DO $$
DECLARE
  ns CONSTANT uuid := '696e6672-6f6e-636f-7265-111111111111';
BEGIN
    -- Helper: compute deterministic uuid v5 for a permission action
    -- usage: uuid_generate_v5(ns, 'vm:create')
    -- Insert canonical permission rows with deterministic IDs

    -- SYSTEM-LEVEL permissions
    INSERT INTO permission (id, action, description, scope)
        VALUES
            (uuid_generate_v5(ns, 'system:settings'), 'system:settings',  'Update system settings', 'SYSTEM'),
            (uuid_generate_v5(ns, 'provider:read'), 'provider:read',     'Read provider', 'SYSTEM'),
            (uuid_generate_v5(ns, 'provider:edit'), 'provider:edit',    'Edit provider', 'SYSTEM'),
            (uuid_generate_v5(ns, 'provider:manage'), 'provider:manage',  'Manage provider', 'SYSTEM'),
            (uuid_generate_v5(ns, 'user:read'), 'user:read',     'Read user', 'PROJECT'),
            (uuid_generate_v5(ns, 'user:edit'), 'user:edit',    'Edit user', 'PROJECT'),
            (uuid_generate_v5(ns, 'user:manage'), 'user:manage',  'Manage user', 'PROJECT'),
            (uuid_generate_v5(ns, 'tenant:read'), 'tenant:read',     'Read tenant', 'SYSTEM'),
            (uuid_generate_v5(ns, 'tenant:edit'), 'tenant:edit',    'Edit tenant', 'SYSTEM'),
            (uuid_generate_v5(ns, 'tenant:manage'), 'tenant:manage',  'Manage tenant', 'SYSTEM'),
            (uuid_generate_v5(ns, 'datacenter:read'), 'datacenter:read',     'Read datacenter', 'SYSTEM'),
            (uuid_generate_v5(ns, 'datacenter:edit'), 'datacenter:edit',    'Edit datacenter', 'SYSTEM'),
            (uuid_generate_v5(ns, 'datacenter:manage'), 'datacenter:manage',  'Manage datacenter', 'SYSTEM')
    ON CONFLICT (action) DO NOTHING;

    -- TENANT/PROJECT-LEVEL permissions
    INSERT INTO permission (id, action, description, scope)
        VALUES
            (uuid_generate_v5(ns, 'tenant:settings'), 'tenant:settings', 'Update tenant settings', 'TENANT'),
            (uuid_generate_v5(ns, 'project:settings'), 'project:settings', 'Update project settings', 'PROJECT'),
            (uuid_generate_v5(ns, 'project:read'), 'project:read',     'Read project', 'TENANT'),
            (uuid_generate_v5(ns, 'project:edit'), 'project:edit',    'Edit project', 'TENANT'),
            (uuid_generate_v5(ns, 'project:manage'), 'project:manage',  'Manage project', 'TENANT'),
            (uuid_generate_v5(ns, 'vm:read'), 'vm:read',     'Read vm', 'PROJECT'),
            (uuid_generate_v5(ns, 'vm:edit'), 'vm:edit',    'Edit vm', 'PROJECT'),
            (uuid_generate_v5(ns, 'vm:manage'), 'vm:manage',  'Manage vm', 'PROJECT'),
            (uuid_generate_v5(ns, 'vm:console'), 'vm:console',  'View vm console', 'PROJECT')
    ON CONFLICT (action) DO NOTHING;
END$$;

-----------------------------------------------------------------------
-- 6. Seed builtin roles (& assign permissions)
-----------------------------------------------------------------------

-- SYSTEM ADMIN
INSERT INTO role (name, description, scope_type, scope_id, immutable)
VALUES ('system:admin', 'Platform Administrator', 'SYSTEM', '215012d9-8b1e-5dc5-b54f-89022875fe1e', TRUE)
ON CONFLICT (name) DO NOTHING;


INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'system:admin'
ON CONFLICT DO NOTHING;

-- TENANT ADMIN
INSERT INTO role (name, description, scope_type, scope_id, immutable)
VALUES ('tenant:admin', 'Tenant Administrator', 'TENANT_GLOBAL', NULL, TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'tenant:admin'
  AND p.scope IN ('TENANT', 'PROJECT')
ON CONFLICT DO NOTHING;

-- PROJECT ADMIN
INSERT INTO role (name, description, scope_type, scope_id, immutable)
VALUES ('project:admin', 'Project Administrator', 'TENANT_GLOBAL', NULL, TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'project:admin'
  AND p.scope = 'PROJECT'
ON CONFLICT DO NOTHING;

-- WORKLOAD OPERATOR
INSERT INTO role (name, description, scope_type, scope_id, immutable)
VALUES ('workload:operator', 'Workload Operator', 'TENANT_GLOBAL', NULL, TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'workload:operator'
  AND p.action IN ('vm:manage', 'vm:edit', 'vm:read', 'vm:console')
ON CONFLICT DO NOTHING;

-- WORKLOAD USER
INSERT INTO role (name, description, scope_type, scope_id, immutable)
VALUES ('workload:user', 'Workload User', 'TENANT_GLOBAL', NULL, TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'workload:user'
  AND p.action IN ('vm:read', 'vm:console')
ON CONFLICT DO NOTHING;

-----------------------------------------------------------------------
-- 7. DONE
-----------------------------------------------------------------------
