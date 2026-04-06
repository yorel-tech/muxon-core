-- Platform libraries: backfill system tenant id where tenant_id was NULL
UPDATE content_libraries
SET tenant_id = '215012d9-8b1e-5dc5-b54f-89022875fe1e'::uuid
WHERE tenant_id IS NULL;

ALTER TABLE content_libraries ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE content_libraries DROP COLUMN IF EXISTS scope;
DROP INDEX IF EXISTS idx_content_libraries_scope;

UPDATE content_libraries SET library_type = 'local' WHERE library_type = 'subscribed';
