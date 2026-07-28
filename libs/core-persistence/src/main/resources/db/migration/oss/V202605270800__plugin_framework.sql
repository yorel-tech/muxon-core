-- Plugin framework: registry, capabilities, resource types, UI modules, catalog, health log
-- Covers: plugins, plugin_capabilities, resource_type_definitions, plugin_ui_modules,
--         plugin_catalog_contributions, plugin_health_log

-- ─── Enum types ─────────────────────────────────────────────────────────────

CREATE TYPE plugin_source AS ENUM ('BUILTIN', 'EXTERNAL');
CREATE TYPE plugin_status AS ENUM ('REGISTERED', 'ACTIVE', 'DEGRADED', 'DISABLED');
CREATE TYPE plugin_capability_type AS ENUM ('RUNTIME', 'SERVICE', 'RESOURCE_PROVIDER', 'UI_EXTENSION');
CREATE TYPE resource_type_status AS ENUM ('ACTIVE', 'UNAVAILABLE');
CREATE TYPE catalog_item_type AS ENUM ('SERVICE_OFFERING', 'RUNTIME_OFFERING', 'STACK_BLUEPRINT');
CREATE TYPE catalog_contribution_status AS ENUM ('PENDING_APPROVAL', 'ACTIVE', 'HIDDEN');

-- ─── plugins ────────────────────────────────────────────────────────────────

CREATE TABLE plugins (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             TEXT NOT NULL,
    version          TEXT NOT NULL,
    source           plugin_source NOT NULL DEFAULT 'EXTERNAL',
    status           plugin_status NOT NULL DEFAULT 'REGISTERED',
    grpc_address     TEXT,
    health_endpoint  TEXT,
    manifest         JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plugins_name_version UNIQUE (name, version)
);

CREATE INDEX idx_plugins_status ON plugins (status);
CREATE INDEX idx_plugins_source ON plugins (source);

CREATE TRIGGER trg_plugins_updated
    BEFORE UPDATE ON plugins
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── plugin_capabilities ────────────────────────────────────────────────────

CREATE TABLE plugin_capabilities (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id        UUID NOT NULL REFERENCES plugins (id) ON DELETE CASCADE,
    capability_type  plugin_capability_type NOT NULL,
    capability_name  TEXT NOT NULL,
    config           JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plugin_capability UNIQUE (plugin_id, capability_type, capability_name)
);

CREATE INDEX idx_plugin_capabilities_plugin ON plugin_capabilities (plugin_id);
CREATE INDEX idx_plugin_capabilities_type ON plugin_capabilities (plugin_id, capability_type);

-- ─── resource_type_definitions ──────────────────────────────────────────────

CREATE TABLE resource_type_definitions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id            UUID NOT NULL REFERENCES plugins (id) ON DELETE RESTRICT,
    kind                 TEXT NOT NULL,
    plural_kind          TEXT NOT NULL,
    schema               JSONB NOT NULL DEFAULT '{}',
    supported_operations TEXT[] NOT NULL DEFAULT '{}',
    quota_dimensions     JSONB,
    status               resource_type_status NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_resource_type_kind UNIQUE (kind)
);

CREATE INDEX idx_resource_type_definitions_plugin ON resource_type_definitions (plugin_id);
CREATE INDEX idx_resource_type_definitions_status ON resource_type_definitions (status);

CREATE TRIGGER trg_resource_type_definitions_updated
    BEFORE UPDATE ON resource_type_definitions
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── plugin_ui_modules ──────────────────────────────────────────────────────

CREATE TABLE plugin_ui_modules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id            UUID NOT NULL REFERENCES plugins (id) ON DELETE CASCADE,
    module_id            TEXT NOT NULL,
    display_name         TEXT NOT NULL,
    module_url           TEXT NOT NULL,
    required_permissions TEXT[] NOT NULL DEFAULT '{}',
    target_kinds         TEXT[] NOT NULL DEFAULT '{}',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plugin_ui_module_id UNIQUE (module_id)
);

CREATE INDEX idx_plugin_ui_modules_plugin ON plugin_ui_modules (plugin_id);

CREATE TRIGGER trg_plugin_ui_modules_updated
    BEFORE UPDATE ON plugin_ui_modules
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── plugin_catalog_contributions ───────────────────────────────────────────

CREATE TABLE plugin_catalog_contributions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id          UUID NOT NULL REFERENCES plugins (id) ON DELETE CASCADE,
    catalog_item_type  catalog_item_type NOT NULL,
    name               TEXT NOT NULL,
    display_name       TEXT NOT NULL,
    description        TEXT,
    config_schema      JSONB NOT NULL DEFAULT '{}',
    target_capability  VARCHAR(255),
    status             catalog_contribution_status NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plugin_catalog_contribution UNIQUE (plugin_id, name)
);

CREATE INDEX idx_plugin_catalog_contributions_plugin ON plugin_catalog_contributions (plugin_id);
CREATE INDEX idx_plugin_catalog_contributions_status ON plugin_catalog_contributions (status);

CREATE TRIGGER trg_plugin_catalog_contributions_updated
    BEFORE UPDATE ON plugin_catalog_contributions
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ─── plugin_health_log ──────────────────────────────────────────────────────

CREATE TABLE plugin_health_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id    UUID NOT NULL REFERENCES plugins (id) ON DELETE CASCADE,
    checked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    success      BOOLEAN NOT NULL,
    error_detail TEXT,
    latency_ms   INTEGER
);

CREATE INDEX idx_plugin_health_log_plugin ON plugin_health_log (plugin_id);
CREATE INDEX idx_plugin_health_log_checked_at ON plugin_health_log (plugin_id, checked_at DESC);
