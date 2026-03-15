-- Run this in your infron DB to see why migrations 10–14 might not have run.
-- Check for failed migrations (success = false) and which versions exist.

SELECT version, description, success, installed_on, execution_time
FROM flyway_schema_history
ORDER BY installed_rank;

-- If you see any row with success = false, that migration failed and blocks all later ones.
-- If you see no rows for version 10, 11, 12, 13, or 14, Flyway simply hasn't run since those files were added.
