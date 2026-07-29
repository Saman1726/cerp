-- Enable pgcrypto extension for gen_random_uuid() (required in PostgreSQL 17+)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Transactional Outbox Events Table
-- Supports the transactional outbox pattern for exactly-once event delivery to Kafka
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    event_type VARCHAR(255) NOT NULL,
    saga_type VARCHAR(255) NOT NULL,
    saga_id VARCHAR(255) NOT NULL,
    aggregate_id UUID,
    aggregate_type VARCHAR(255),
    tenant_id VARCHAR(255),
    user_id VARCHAR(255),
    event_data TEXT,
    metadata TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN (
            'PENDING',
            'PUBLISHED',
            'FAILED'
        )
    ),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 1
);

-- Index for filtering pending events by status and creation time
CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created ON outbox_events (status, created_at);

-- Index for querying by saga correlation
CREATE INDEX IF NOT EXISTS idx_outbox_events_saga_id ON outbox_events (saga_id);

-- Index for filtering by aggregate
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate ON outbox_events (aggregate_type, aggregate_id);

-- Index for tenant-based queries
CREATE INDEX IF NOT EXISTS idx_outbox_events_tenant ON outbox_events (tenant_id);