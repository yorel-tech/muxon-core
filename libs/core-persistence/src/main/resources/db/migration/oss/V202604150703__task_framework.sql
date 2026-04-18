-- Queue, jobs, task steps/logs, entity events (merged V4 without VM; V13, V14, V19, V21, V26, V35, V39)

CREATE TYPE queue_category AS ENUM (
    'COMMAND',
    'STATUS',
    'AUDIT',
    'TASK_EVENT',
    'ENTITY_EVENT'
);

CREATE TYPE queue_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
);

CREATE TYPE job_type AS ENUM (
    'VM_CREATE', 'VM_DELETE', 'VM_START', 'VM_STOP', 'VM_RESTART',
    'VM_SUSPEND', 'VM_RESUME', 'VM_SNAPSHOT', 'VM_BACKUP',
    'VM_MIGRATE', 'VM_RESIZE', 'NODE_PROVISION', 'NODE_DECOMMISSION',
    'PROVIDER_SYNC',
    'PROVIDER_STORAGE_SYNC',
    'CONTENT_LIBRARY_SYNC', 'CONTENT_ITEM_FETCH',
    'CONTENT_LIBRARY_REPLICATE', 'CONTENT_DATACENTER_REPLICATE'
);

CREATE TYPE job_status AS ENUM (
    'PENDING',
    'RUNNING',
    'COMPLETED',
    'FAILED',
    'CANCELLED',
    'TIMEOUT'
);

CREATE TYPE entity_type AS ENUM (
    'VM', 'NODE', 'DATACENTER', 'TENANT', 'PROVIDER', 'USER', 'CONTENT_LIBRARY'
);

CREATE TABLE queue_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_type TEXT NOT NULL,
    entity_type entity_type NOT NULL,
    entity_id UUID NOT NULL,
    queue_category queue_category NOT NULL,
    status queue_status NOT NULL DEFAULT 'PENDING',
    payload JSONB NOT NULL,
    actor_user_id UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    actor_service TEXT,
    actor_type TEXT NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    correlation_id TEXT,
    request_id TEXT,
    source TEXT NOT NULL,
    version TEXT DEFAULT '1.0',
    metadata JSONB,
    created_by UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    error_message TEXT
);

CREATE INDEX idx_queue_entity ON queue_entry (entity_type, entity_id);
CREATE INDEX idx_queue_type ON queue_entry (queue_type);
CREATE INDEX idx_queue_category ON queue_entry (queue_category);
CREATE INDEX idx_queue_status ON queue_entry (status);
CREATE INDEX idx_queue_created ON queue_entry (created_at);
CREATE INDEX idx_queue_correlation ON queue_entry (correlation_id);
CREATE INDEX idx_queue_polling ON queue_entry (status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_queue_updated ON queue_entry (updated_at);
CREATE INDEX idx_queue_entry_category_status ON queue_entry (queue_category, status, created_at) WHERE status = 'PENDING';

CREATE TABLE job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type job_type NOT NULL,
    status job_status NOT NULL DEFAULT 'PENDING',
    target_entity_type entity_type NOT NULL,
    target_entity_id UUID NOT NULL,
    parameters JSONB,
    result JSONB,
    provider_id UUID REFERENCES provider(id) ON DELETE SET NULL,
    provider_operation_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    error_details JSONB,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    correlation_id TEXT,
    request_id TEXT,
    current_step TEXT,
    total_steps INTEGER,
    progress_percentage INTEGER DEFAULT 0,
    timeout_at TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    cancellation_requested BOOLEAN DEFAULT FALSE,
    cancellation_reason TEXT,
    metadata JSONB
);

CREATE INDEX idx_job_target ON job (target_entity_type, target_entity_id);
CREATE INDEX idx_job_status ON job (status);
CREATE INDEX idx_job_type ON job (job_type);
CREATE INDEX idx_job_provider ON job (provider_id);
CREATE INDEX idx_job_correlation ON job (correlation_id);
CREATE INDEX idx_job_timeout ON job (timeout_at) WHERE status = 'RUNNING';
CREATE INDEX idx_job_heartbeat ON job (last_heartbeat_at) WHERE status = 'RUNNING';
CREATE INDEX idx_job_cancellation ON job (cancellation_requested) WHERE cancellation_requested = TRUE;
CREATE INDEX idx_job_progress ON job (progress_percentage);

CREATE TABLE entity_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type TEXT NOT NULL,
    entity_type entity_type NOT NULL,
    entity_id UUID NOT NULL,
    message TEXT,
    details JSONB,
    actor_user_id UUID REFERENCES idp_user(id) ON DELETE SET NULL,
    actor_service TEXT,
    actor_type TEXT NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    correlation_id TEXT,
    request_id TEXT,
    source TEXT NOT NULL,
    version TEXT DEFAULT '1.0',
    metadata JSONB
);

CREATE INDEX idx_entity_event_entity ON entity_event (entity_type, entity_id);
CREATE INDEX idx_entity_event_type ON entity_event (event_type);
CREATE INDEX idx_entity_event_created ON entity_event (created_at);
CREATE INDEX idx_entity_event_correlation ON entity_event (correlation_id);

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

CREATE INDEX idx_task_step_task ON task_step (task_id);
CREATE INDEX idx_task_step_status ON task_step (status);
CREATE INDEX idx_task_step_task_number ON task_step (task_id, step_number);

CREATE TABLE task_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    step_id UUID REFERENCES task_step(id) ON DELETE SET NULL,
    log_level TEXT NOT NULL CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR')),
    message TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_log_task ON task_log (task_id, created_at DESC);
CREATE INDEX idx_task_log_step ON task_log (step_id);
CREATE INDEX idx_task_log_level ON task_log (log_level);
CREATE INDEX idx_task_log_created ON task_log (created_at DESC);

CREATE TRIGGER trg_queue_entry_updated BEFORE UPDATE ON queue_entry FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_task_step_updated
    BEFORE UPDATE ON task_step
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

COMMENT ON TABLE task_step IS 'Tracks individual steps within a multi-step task/job';
COMMENT ON TABLE task_log IS 'Detailed execution logs for tasks and steps';
COMMENT ON COLUMN job.current_step IS 'Human-readable description of current step';
COMMENT ON COLUMN job.total_steps IS 'Total number of steps in this task (if known)';
COMMENT ON COLUMN job.progress_percentage IS 'Progress indicator from 0 to 100';
COMMENT ON COLUMN job.timeout_at IS 'Timestamp when task should timeout';
COMMENT ON COLUMN job.last_heartbeat_at IS 'Last activity/heartbeat timestamp';
COMMENT ON COLUMN job.cancellation_requested IS 'User requested task cancellation';
COMMENT ON COLUMN job.metadata IS 'Additional task context and metadata';
