-- RBAC: permissions, roles, bindings, seeds (merged V2, V7, V22–V24, V28, V30; vm:create from V4)

CREATE TYPE role_scope AS ENUM ('SYSTEM', 'TENANT', 'TENANT_GLOBAL');
CREATE TYPE subject_type AS ENUM ('USER', 'GROUP', 'SERVICE_ACCOUNT');

CREATE TABLE role_bindings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name TEXT NOT NULL,
    subject_type subject_type NOT NULL DEFAULT 'USER',
    subject_id TEXT NOT NULL,
    scope_type role_scope NOT NULL DEFAULT 'SYSTEM',
    scope_id UUID,
    expires_at TIMESTAMPTZ,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_role_bindings UNIQUE (role_name, subject_type, subject_id, scope_type, scope_id)
);

CREATE INDEX idx_role_bindings_rolename ON role_bindings (role_name);
CREATE INDEX idx_role_bindings_subject ON role_bindings (subject_type, subject_id);
CREATE INDEX idx_role_bindings_scope ON role_bindings (scope_type, scope_id);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
