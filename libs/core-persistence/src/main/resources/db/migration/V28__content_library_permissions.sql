-- Tenant-scoped content library permissions (UUID v5 namespace matches Permission.java)
DO $$
DECLARE
    ns CONSTANT uuid := '696e6672-6f6e-636f-7265-111111111111';
BEGIN
    INSERT INTO permission (id, action, description, scope)
    VALUES
        (uuid_generate_v5(ns, 'content_library:read'), 'content_library:read', 'Read content library', 'TENANT'),
        (uuid_generate_v5(ns, 'content_library:write'), 'content_library:write', 'Write to content library', 'TENANT'),
        (uuid_generate_v5(ns, 'content_library:publish_template'), 'content_library:publish_template', 'Publish VM as template to content library', 'TENANT')
    ON CONFLICT (action) DO NOTHING;
END$$;
