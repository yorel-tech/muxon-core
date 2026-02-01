-- Fix user_role_bindings view to cast enum columns to text
-- This resolves PostgreSQL error: operator does not exist: role_scope = character varying

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
    rb.subject_type::text as subject_type,
    rb.subject_id,
    rb.scope_type::text as scope_type,
    rb.scope_id,
    rb.expires_at,
    rb.created_by,
    rb.created_at as binding_created_at,
    r.name as role_name,
    r.description as role_description,
    r.scope_type::text as role_scope_type,
    r.scope_id as role_scope_id
FROM idp_user tu
JOIN role_binding rb ON tu.id::text = rb.subject_id AND rb.subject_type = 'USER'
LEFT JOIN role r ON rb.role_id = r.id;
