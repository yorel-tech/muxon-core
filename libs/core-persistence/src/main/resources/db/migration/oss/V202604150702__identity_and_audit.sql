-- Identity, audit, system init, system settings, user_role_bindings view (merged V3, V5 IdP + system_settings, V6, V8, V31 console timeout)

CREATE TYPE idp_protocol AS ENUM ('OIDC', 'OAUTH2');
CREATE TYPE bootstrap_status AS ENUM ('NOTREADY', 'BOOTSTRAPPED', 'READY');

CREATE TABLE identity_provider (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    protocol idp_protocol NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    metadata JSONB NOT NULL,
    is_system BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_identity_protocol UNIQUE (protocol)
);

CREATE INDEX idx_identity_provider_protocol ON identity_provider (protocol);
CREATE INDEX idx_identity_provider_enabled ON identity_provider (enabled);

CREATE UNIQUE INDEX uq_identity_provider_system
ON identity_provider ((1))
WHERE is_system = true;

CREATE TABLE idp_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identity_provider_id UUID NOT NULL REFERENCES identity_provider(id) ON DELETE CASCADE,
    external_id TEXT,
    username TEXT,
    email TEXT,
    display_name TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (identity_provider_id, external_id)
);

CREATE INDEX ix_idp_user_email ON idp_user (email);
CREATE INDEX ix_idp_user_external ON idp_user (identity_provider_id, external_id);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenant(id),
    actor_user_id UUID REFERENCES idp_user(id),
    action TEXT NOT NULL,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE system_init (
    primary_key TEXT PRIMARY KEY,
    value TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    system_status bootstrap_status NOT NULL DEFAULT 'NOTREADY',
    CONSTRAINT check_system_status CHECK (system_status IN ('NOTREADY', 'BOOTSTRAPPED', 'READY'))
);

INSERT INTO system_init (primary_key, value, system_status, updated_at)
VALUES ('bootstrap_status', 'false', 'NOTREADY', now())
ON CONFLICT (primary_key) DO NOTHING;

CREATE TABLE system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenant(id) ON DELETE CASCADE,
    name TEXT,
    description TEXT,
    contact_email TEXT,
    contact_phone TEXT,
    default_timezone TEXT DEFAULT 'UTC',
    default_locale TEXT DEFAULT 'en-US',
    session_timeout_minutes INTEGER DEFAULT 60,
    max_login_attempts INTEGER DEFAULT 5,
    lockout_duration_minutes INTEGER DEFAULT 30,
    password_min_length INTEGER DEFAULT 8,
    password_require_uppercase BOOLEAN DEFAULT true,
    password_require_lowercase BOOLEAN DEFAULT true,
    password_require_digit BOOLEAN DEFAULT true,
    password_require_symbol BOOLEAN DEFAULT false,
    password_expiry_days INTEGER DEFAULT 90,
    api_rate_limit_per_minute INTEGER DEFAULT 100,
    smtp_enabled BOOLEAN DEFAULT false,
    smtp_host TEXT,
    smtp_port INTEGER DEFAULT 587,
    smtp_username TEXT,
    smtp_password TEXT,
    smtp_from_email TEXT,
    smtp_use_tls BOOLEAN DEFAULT true,
    slack_webhook_url TEXT,
    webhook_enabled BOOLEAN DEFAULT false,
    theme TEXT DEFAULT 'light',
    logo_url TEXT,
    favicon_url TEXT,
    primary_color TEXT DEFAULT '#3b82f6',
    secondary_color TEXT DEFAULT '#64748b',
    custom_css_url TEXT,
    console_session_timeout_minutes INTEGER DEFAULT 15,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES idp_user(id)
);

CREATE UNIQUE INDEX uq_tenant_settings_singleton ON system_settings (tenant_id);
CREATE INDEX idx_system_settings_tenant ON system_settings (tenant_id);

CREATE TRIGGER trg_system_init_updated BEFORE UPDATE ON system_init FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_identity_provider_updated BEFORE UPDATE ON identity_provider FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_idp_user_updated BEFORE UPDATE ON idp_user FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_system_settings_updated
    BEFORE UPDATE ON system_settings
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

INSERT INTO system_settings (
    id, tenant_id, name, description, contact_email, contact_phone, default_timezone, default_locale,
    session_timeout_minutes, max_login_attempts, lockout_duration_minutes,
    password_min_length, password_require_uppercase, password_require_lowercase, password_require_digit,
    password_expiry_days, api_rate_limit_per_minute, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    '215012d9-8b1e-5dc5-b54f-89022875fe1e',
    'Muxon Cloud Platform',
    'Multi-tenant cloud management platform',
    'admin@muxon.example',
    '+1-555-123-4567',
    'UTC',
    'en-US',
    60,
    5,
    30,
    8,
    true,
    true,
    true,
    90,
    100,
    now(),
    now()
)
ON CONFLICT DO NOTHING;

CREATE OR REPLACE VIEW user_role_bindings AS
SELECT
    tu.id AS user_id,
    tu.external_id,
    tu.identity_provider_id,
    tu.username,
    tu.email,
    tu.display_name,
    tu.metadata AS user_metadata,
    tu.created_at AS user_created_at,
    tu.updated_at AS user_updated_at,
    rb.id AS binding_id,
    NULL::uuid AS role_id,
    rb.subject_type::text AS subject_type,
    rb.subject_id,
    rb.scope_type::text AS scope_type,
    rb.scope_id,
    rb.expires_at,
    rb.created_by,
    rb.created_at AS binding_created_at,
    rb.role_name AS role_name,
    NULL::text AS role_description,
    rb.scope_type::text AS role_scope_type,
    rb.scope_id AS role_scope_id
FROM idp_user tu
JOIN role_bindings rb ON tu.id::text = rb.subject_id AND rb.subject_type = 'USER';
