-- Outbox Events table for reliable event publishing
CREATE TABLE notification.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status ON notification.outbox_events (status);
CREATE INDEX idx_outbox_aggregate ON notification.outbox_events (aggregate_id);
CREATE INDEX idx_outbox_created ON notification.outbox_events (created_at);