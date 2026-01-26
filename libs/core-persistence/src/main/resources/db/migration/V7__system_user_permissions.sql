-- =====================================================================
-- Fix for 403 error on /api/v1/system-users endpoint
-- =====================================================================
-- This migration adds SYSTEM-scoped permissions for system user management.
-- 
-- Root Cause:
-- - The 'user:read' permission is TENANT-scoped (for tenant users)
-- - The 'system:admin' role only gets SYSTEM-scoped permissions
-- - Therefore, system admins cannot access /api/v1/system-users endpoint
--
-- Solution:
-- - Add SYSTEM-scoped permissions for system user management
-- - Assign them to the 'system:admin' role
-- =====================================================================

DO $$
DECLARE
  ns CONSTANT uuid := '696e6672-6f6e-636f-7265-111111111111';
BEGIN
    -- Insert SYSTEM-scoped user management permissions
    INSERT INTO permission (id, action, description, scope)
        VALUES
            (uuid_generate_v5(ns, 'system:user:read'), 'system:user:read',     'Read system user', 'SYSTEM'),
            (uuid_generate_v5(ns, 'system:user:edit'), 'system:user:edit',    'Edit system user', 'SYSTEM'),
            (uuid_generate_v5(ns, 'system:user:manage'), 'system:user:manage',  'Manage system user', 'SYSTEM')
    ON CONFLICT (action) DO NOTHING;

    -- Assign these permissions to the system:admin role
    INSERT INTO role_permission (role_id, permission_id)
    SELECT r.id, p.id
    FROM role r, permission p
    WHERE r.name = 'system:admin'
      AND p.action IN ('system:user:read', 'system:user:edit', 'system:user:manage')
    ON CONFLICT DO NOTHING;
END$$;
