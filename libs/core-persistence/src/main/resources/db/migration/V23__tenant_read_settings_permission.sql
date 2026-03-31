-- SYSTEM-scoped permission for reading tenant settings.
-- This is intentionally assigned only to system:admin.

INSERT INTO permission (id, action, description, scope)
VALUES (
    uuid_generate_v5('696e6672-6f6e-636f-7265-111111111111'::uuid, 'tenant:read-settings'),
    'tenant:read-settings',
    'Read tenant settings',
    'SYSTEM'
)
ON CONFLICT (action) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'system:admin' AND p.action = 'tenant:read-settings'
ON CONFLICT (role_id, permission_id) DO NOTHING;
