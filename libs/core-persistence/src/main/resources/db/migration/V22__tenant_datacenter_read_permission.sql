-- Tenant-scoped permission so tenant admins can list/view datacenter grants without system datacenter:read.
-- system:admin also receives this so RequiresAnyPermission(DATACENTER_READ, TENANT_DATACENTER_READ) is satisfied via either binding.

INSERT INTO permission (id, action, description, scope)
VALUES (
    uuid_generate_v5('696e6672-6f6e-636f-7265-111111111111'::uuid, 'tenant:datacenter:read'),
    'tenant:datacenter:read',
    'Read tenant datacenter grants',
    'TENANT'
)
ON CONFLICT (action) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'tenant:admin' AND p.action = 'tenant:datacenter:read'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'system:admin' AND p.action = 'tenant:datacenter:read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
