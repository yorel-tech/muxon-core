-- VM console sessions (merged V31, V44, V45 DDL only; no dedupe DELETE)

CREATE TYPE console_session_status AS ENUM ('ACTIVE', 'EXPIRED', 'CLOSED');
CREATE TYPE vm_console_type AS ENUM ('VNC', 'SPICE', 'SERIAL');

CREATE TABLE console_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vm_id UUID NOT NULL REFERENCES vms (id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES idp_users (id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    console_type vm_console_type NOT NULL,
    hypervisor_host TEXT NOT NULL,
    hypervisor_port INTEGER NOT NULL,
    hypervisor_password TEXT,
    tls BOOLEAN NOT NULL DEFAULT false,
    status console_session_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    upstream_ws_url TEXT,
    upstream_ws_cookie TEXT,
    upstream_ws_csrf TEXT,
    upstream_ws_authorization TEXT,
    CONSTRAINT uq_console_sessions_token UNIQUE (token),
    CONSTRAINT uq_console_sessions_vm_user UNIQUE (vm_id, user_id)
);

CREATE INDEX idx_console_sessions_token ON console_sessions (token);
CREATE INDEX idx_console_sessions_expires_at ON console_sessions (expires_at);
CREATE INDEX idx_console_sessions_vm_user ON console_sessions (vm_id, user_id);
CREATE INDEX idx_console_sessions_user_status ON console_sessions (user_id, status);
