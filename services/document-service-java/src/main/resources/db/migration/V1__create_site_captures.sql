CREATE SCHEMA IF NOT EXISTS document;

CREATE TABLE IF NOT EXISTS document.site_captures (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    sub_project_id UUID,
    kind VARCHAR(50) NOT NULL,
    notes TEXT,
    captured_at TIMESTAMPTZ NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    byte_size BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_site_captures_project ON document.site_captures (project_id);
CREATE INDEX IF NOT EXISTS idx_site_captures_tenant ON document.site_captures (tenant_id);
