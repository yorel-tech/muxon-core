-- V19__unified_task_tracking.sql
-- Enhance job table and add task_step and task_log tables for unified task tracking
-- This migration implements a comprehensive task orchestration system with:
-- - Enhanced job tracking with progress, timeouts, and heartbeats
-- - Multi-step task execution tracking
-- - Detailed task logging for debugging
-- - Support for task cancellation and timeout handling

-- Add new columns to existing job table
ALTER TABLE job ADD COLUMN IF NOT EXISTS current_step TEXT;
ALTER TABLE job ADD COLUMN IF NOT EXISTS total_steps INTEGER;
ALTER TABLE job ADD COLUMN IF NOT EXISTS progress_percentage INTEGER DEFAULT 0;
ALTER TABLE job ADD COLUMN IF NOT EXISTS timeout_at TIMESTAMPTZ;
ALTER TABLE job ADD COLUMN IF NOT EXISTS last_heartbeat_at TIMESTAMPTZ;
ALTER TABLE job ADD COLUMN IF NOT EXISTS cancellation_requested BOOLEAN DEFAULT FALSE;
ALTER TABLE job ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;
ALTER TABLE job ADD COLUMN IF NOT EXISTS metadata JSONB;

-- Add TIMEOUT status to job_status enum
ALTER TYPE job_status ADD VALUE IF NOT EXISTS 'TIMEOUT';

-- Create task_step table for tracking individual steps within a task
CREATE TABLE task_step (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    step_name TEXT NOT NULL,
    step_description TEXT,
    status job_status NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    error_details JSONB,
    output JSONB,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT unique_task_step UNIQUE (task_id, step_number),
    CONSTRAINT valid_step_number CHECK (step_number > 0)
);

-- Indexes for task_step
CREATE INDEX idx_task_step_task ON task_step(task_id);
CREATE INDEX idx_task_step_status ON task_step(status);
CREATE INDEX idx_task_step_task_number ON task_step(task_id, step_number);

-- Create task_log table for detailed logging
CREATE TABLE task_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    step_id UUID REFERENCES task_step(id) ON DELETE SET NULL,
    log_level TEXT NOT NULL CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR')),
    message TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for task_log
CREATE INDEX idx_task_log_task ON task_log(task_id, created_at DESC);
CREATE INDEX idx_task_log_step ON task_log(step_id);
CREATE INDEX idx_task_log_level ON task_log(log_level);
CREATE INDEX idx_task_log_created ON task_log(created_at DESC);

-- Add indexes for new job columns
CREATE INDEX idx_job_timeout ON job(timeout_at) WHERE status = 'RUNNING';
CREATE INDEX idx_job_heartbeat ON job(last_heartbeat_at) WHERE status = 'RUNNING';
CREATE INDEX idx_job_cancellation ON job(cancellation_requested) WHERE cancellation_requested = TRUE;
CREATE INDEX idx_job_progress ON job(progress_percentage);

-- Add trigger for task_step updated_at
CREATE TRIGGER trg_task_step_updated 
    BEFORE UPDATE ON task_step 
    FOR EACH ROW 
    EXECUTE FUNCTION trigger_set_timestamp();

-- Add comments for documentation
COMMENT ON TABLE task_step IS 'Tracks individual steps within a multi-step task/job';
COMMENT ON TABLE task_log IS 'Detailed execution logs for tasks and steps';
COMMENT ON COLUMN job.current_step IS 'Human-readable description of current step';
COMMENT ON COLUMN job.total_steps IS 'Total number of steps in this task (if known)';
COMMENT ON COLUMN job.progress_percentage IS 'Progress indicator from 0 to 100';
COMMENT ON COLUMN job.timeout_at IS 'Timestamp when task should timeout';
COMMENT ON COLUMN job.last_heartbeat_at IS 'Last activity/heartbeat timestamp';
COMMENT ON COLUMN job.cancellation_requested IS 'User requested task cancellation';
COMMENT ON COLUMN job.metadata IS 'Additional task context and metadata';
