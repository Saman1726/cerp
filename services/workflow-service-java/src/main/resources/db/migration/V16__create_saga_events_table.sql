-- Saga Events Table for saga state persistence
-- Stores saga execution state including metadata, step history, and error information
CREATE TABLE IF NOT EXISTS saga_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    saga_type VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    current_step VARCHAR(255),
    next_step VARCHAR(255),
    steps_json TEXT,
    last_error_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for finding active sagas by tenant and status
CREATE INDEX IF NOT EXISTS idx_saga_events_tenant_status ON saga_events (tenant_id, status);

-- Index for finding sagas by type and status
CREATE INDEX IF NOT EXISTS idx_saga_events_saga_type_status ON saga_events (saga_type, status);

-- Index for finding stale/timed-out sagas
CREATE INDEX IF NOT EXISTS idx_saga_events_updated_at ON saga_events (updated_at);