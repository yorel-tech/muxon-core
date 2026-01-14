-- V5__system_settings.sql
-- Create system_settings table for system and tenant-level configuration
-- Add is_system column to identity_provider table for marking system-level IdP

-- Add tenant_id to system_settings table
-- This allows tenant-specific overrides while maintaining system defaults
CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- SYSTEM_ID constant = system-level settings, tenant UUID = tenant-specific settings
    tenant_id UUID REFERENCES tenant(id) ON DELETE CASCADE,
    
    -- General Settings
    name TEXT,
    description TEXT,
    contact_email TEXT,
    contact_phone TEXT,
    default_timezone TEXT DEFAULT 'UTC',
    default_locale TEXT DEFAULT 'en-US',
    
    -- Security Settings
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
    
    -- Notification Settings
    smtp_enabled BOOLEAN DEFAULT false,
    smtp_host TEXT,
    smtp_port INTEGER DEFAULT 587,
    smtp_username TEXT,
    smtp_password TEXT, -- encrypted
    smtp_from_email TEXT,
    smtp_use_tls BOOLEAN DEFAULT true,
    slack_webhook_url TEXT,
    webhook_enabled BOOLEAN DEFAULT false,
    
    -- Appearance Settings
    theme TEXT DEFAULT 'light',
    logo_url TEXT,
    favicon_url TEXT,
    primary_color TEXT DEFAULT '#3b82f6',
    secondary_color TEXT DEFAULT '#64748b',
    custom_css_url TEXT,
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES idp_user(id)
);

-- Each tenant can have at most one settings override
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_settings_singleton 
ON system_settings(tenant_id)

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_system_settings_tenant ON system_settings(tenant_id);

-- Trigger for updated_at
CREATE TRIGGER trg_system_settings_updated 
    BEFORE UPDATE ON system_settings 
    FOR EACH ROW 
    EXECUTE FUNCTION trigger_set_timestamp();

-- Add is_system column to identity_provider table
ALTER TABLE identity_provider 
ADD COLUMN IF NOT EXISTS is_system BOOLEAN DEFAULT false;

-- Ensure only one system IdP exists
CREATE UNIQUE INDEX IF NOT EXISTS uq_identity_provider_system 
ON identity_provider ((1)) 
WHERE is_system = true;

-- Insert default system settings row
INSERT INTO system_settings (id, tenant_id, name, description, default_timezone, default_locale, 
    session_timeout_minutes, max_login_attempts, lockout_duration_minutes, 
    password_min_length, password_require_uppercase, password_require_lowercase, password_require_digit, 
    password_expiry_days, api_rate_limit_per_minute, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '215012d9-8b1e-5dc5-b54f-89022875fe1e',
    'Infron Cloud Platform',
    'Multi-tenant cloud management platform',
    'admin@infron.example',
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
