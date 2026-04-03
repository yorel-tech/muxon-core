-- Grant new content library permissions to built-in roles (permissions added in V28 after V2 seed).

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON TRUE
WHERE r.name IN ('tenant:admin', 'system:admin')
  AND p.action IN (
        'content_library:read',
        'content_library:write',
        'content_library:publish_template')
ON CONFLICT DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON TRUE
WHERE r.name IN ('workload:operator', 'workload:user')
  AND p.action = 'content_library:read'
ON CONFLICT DO NOTHING;
