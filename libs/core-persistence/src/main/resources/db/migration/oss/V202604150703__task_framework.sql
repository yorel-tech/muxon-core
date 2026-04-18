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

CREATE TABLE orchestrator_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_type TEXT NOT NULL,
    entity_type entity_type NOT NULL,
    entity_id UUID NOT NULL,
    queue_category queue_category NOT NULL,
    status queue_status NOT NULL DEFAULT 'PENDING',
    payload JSONB NOT NULL,
    actor_user_id UUID REFERENCES idp_users(id) ON DELETE SET NULL,
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
    created_by UUID REFERENCES idp_users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES idp_users(id) ON DELETE SET NULL,
    error_message TEXT
);

CREATE INDEX idx_queue_entity ON orchestrator_queue (entity_type, entity_id);
CREATE INDEX idx_queue_type ON orchestrator_queue (queue_type);
CREATE INDEX idx_queue_category ON orchestrator_queue (queue_category);
CREATE INDEX idx_queue_status ON orchestrator_queue (status);
CREATE INDEX idx_queue_created ON orchestrator_queue (created_at);
CREATE INDEX idx_queue_correlation ON orchestrator_queue (correlation_id);
CREATE INDEX idx_queue_polling ON orchestrator_queue (status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_queue_updated ON orchestrator_queue (updated_at);
CREATE INDEX idx_queue_entry_category_status ON orchestrator_queue (queue_category, status, created_at) WHERE status = 'PENDING';

CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type job_type NOT NULL,
    status job_status NOT NULL DEFAULT 'PENDING',
    target_entity_type entity_type NOT NULL,
    target_entity_id UUID NOT NULL,
    parameters JSONB,
    result JSONB,
    provider_id UUID REFERENCES providers(id) ON DELETE SET NULL,
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

CREATE INDEX idx_job_target ON jobs (target_entity_type, target_entity_id);
CREATE INDEX idx_job_status ON jobs (status);
CREATE INDEX idx_job_type ON jobs (job_type);
CREATE INDEX idx_job_provider ON jobs (provider_id);
CREATE INDEX idx_job_correlation ON jobs (correlation_id);
CREATE INDEX idx_job_timeout ON jobs (timeout_at) WHERE status = 'RUNNING';
CREATE INDEX idx_job_heartbeat ON jobs (last_heartbeat_at) WHERE status = 'RUNNING';
CREATE INDEX idx_job_cancellation ON jobs (cancellation_requested) WHERE cancellation_requested = TRUE;
CREATE INDEX idx_job_progress ON jobs (progress_percentage);

CREATE TABLE entity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type TEXT NOT NULL,
    entity_type entity_type NOT NULL,
    entity_id UUID NOT NULL,
    message TEXT,
    details JSONB,
    actor_user_id UUID REFERENCES idp_users(id) ON DELETE SET NULL,
    actor_service TEXT,
    actor_type TEXT NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    correlation_id TEXT,
    request_id TEXT,
    source TEXT NOT NULL,
    version TEXT DEFAULT '1.0',
    metadata JSONB
);

CREATE INDEX idx_entity_event_entity ON entity_events (entity_type, entity_id);
CREATE INDEX idx_entity_event_type ON entity_events (event_type);
CREATE INDEX idx_entity_event_created ON entity_events (created_at);
CREATE INDEX idx_entity_event_correlation ON entity_events (correlation_id);

CREATE TABLE task_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
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

CREATE INDEX idx_task_step_task ON task_steps (task_id);
CREATE INDEX idx_task_step_status ON task_steps (status);
CREATE INDEX idx_task_step_task_number ON task_steps (task_id, step_number);

CREATE TABLE task_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    step_id UUID REFERENCES task_steps(id) ON DELETE SET NULL,
    log_level TEXT NOT NULL CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR')),
    message TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_log_task ON task_logs (task_id, created_at DESC);
CREATE INDEX idx_task_log_step ON task_logs (step_id);
CREATE INDEX idx_task_log_level ON task_logs (log_level);
CREATE INDEX idx_task_log_created ON task_logs (created_at DESC);

CREATE TRIGGER trg_orchestrator_queue_updated BEFORE UPDATE ON orchestrator_queue FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER trg_task_step_updated
    BEFORE UPDATE ON task_steps
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

COMMENT ON TABLE task_steps IS 'Tracks individual steps within a multi-step task/job';
COMMENT ON TABLE task_logs IS 'Detailed execution logs for tasks and steps';
COMMENT ON COLUMN jobs.current_step IS 'Human-readable description of current step';
COMMENT ON COLUMN jobs.total_steps IS 'Total number of steps in this task (if known)';
COMMENT ON COLUMN jobs.progress_percentage IS 'Progress indicator from 0 to 100';
COMMENT ON COLUMN jobs.timeout_at IS 'Timestamp when task should timeout';
COMMENT ON COLUMN jobs.last_heartbeat_at IS 'Last activity/heartbeat timestamp';
COMMENT ON COLUMN jobs.cancellation_requested IS 'User requested task cancellation';
COMMENT ON COLUMN jobs.metadata IS 'Additional task context and metadata';
