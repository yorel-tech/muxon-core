-- Add new queue categories for the Stage 3 event-driven orchestration.
-- TASK_EVENT: task status events emitted by workers, consumed by orchestrator TaskEventProcessor.
-- ENTITY_EVENT: entity state events emitted by workers, consumed by core-services EntityEventProcessor.
ALTER TYPE queue_category ADD VALUE IF NOT EXISTS 'TASK_EVENT';
ALTER TYPE queue_category ADD VALUE IF NOT EXISTS 'ENTITY_EVENT';

-- Add CONTENT_LIBRARY to entity_type enum for content library workflow jobs.
ALTER TYPE entity_type ADD VALUE IF NOT EXISTS 'CONTENT_LIBRARY';

-- Index to speed up polling by category (used by DbTaskEventQueue and DbEntityEventQueue)
CREATE INDEX IF NOT EXISTS idx_queue_entry_category_status
    ON queue_entry(queue_category, status, created_at)
    WHERE status = 'PENDING';
