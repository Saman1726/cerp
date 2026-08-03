-- Create notification schema
CREATE SCHEMA IF NOT EXISTS notification;

-- Notification Jobs
CREATE TABLE IF NOT EXISTS notification.notification_jobs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    action_type VARCHAR(32) NOT NULL CHECK (action_type IN ('REVIEW', 'APPROVE', 'REJECT', 'ACKNOWLEDGE', 'EXECUTE', 'INPUT')),
    payload JSONB,
    source_service VARCHAR(100),
    source_entity_id UUID,
    source_entity_type VARCHAR(100),
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'PARTIALLY_COMPLETED', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    recipient_user_ids TEXT[],
    recipient_roles TEXT[],
    due_at VARCHAR(255),
    reminder_interval_minutes INTEGER,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT uk_job_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_job_tenant ON notification.notification_jobs (tenant_id);
CREATE INDEX IF NOT EXISTS idx_job_status ON notification.notification_jobs (status);
CREATE INDEX IF NOT EXISTS idx_job_priority ON notification.notification_jobs (priority);
CREATE INDEX IF NOT EXISTS idx_job_source ON notification.notification_jobs (source_service, source_entity_id);
CREATE INDEX IF NOT EXISTS idx_job_created_at ON notification.notification_jobs (created_at);

-- Notifications (per recipient)
CREATE TABLE IF NOT EXISTS notification.notifications (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES notification.notification_jobs(id) ON DELETE CASCADE,
    tenant_id VARCHAR(255) NOT NULL,
    recipient_id UUID NOT NULL,
    recipient_role VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'READ', 'ACTION_TAKEN', 'EXPIRED', 'ARCHIVED')),
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    read_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    action_taken_at TIMESTAMPTZ,
    action_taken_by UUID,
    action_result JSONB,
    expires_at VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_job_id ON notification.notifications (job_id);
CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notification.notifications (recipient_id);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notification.notifications (status);
CREATE INDEX IF NOT EXISTS idx_notification_expires ON notification.notifications (expires_at);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_status ON notification.notifications (recipient_id, status);