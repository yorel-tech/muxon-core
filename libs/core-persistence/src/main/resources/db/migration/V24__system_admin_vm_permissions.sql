-- =====================================================================
-- Allow system admins to operate tenant VMs
-- =====================================================================
-- Root cause:
-- - VM APIs are tenant-scoped and require vm:* permissions.
-- - system:admin was only granted SYSTEM-scoped permissions.
-- - Result: system admins receive 403 on /api/v1/tenants/{tenantId}/vms endpoints.
--
-- Solution:
-- - Grant VM tenant permissions to system:admin role.
-- =====================================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON TRUE
WHERE r.name = 'system:admin'
  AND p.action IN ('vm:read', 'vm:edit', 'vm:manage', 'vm:console')
ON CONFLICT DO NOTHING;
