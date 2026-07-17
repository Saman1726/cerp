-- V1: Create projects table
-- Core construction project/project portfolio table

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    tenant_id VARCHAR(255) NOT NULL,
    org_id UUID NOT NULL,
    external_id VARCHAR(128),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    property_id UUID,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    postal_code VARCHAR(20),
    city VARCHAR(128),
    country_code VARCHAR(4),
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNING',
    project_type VARCHAR(64),
    start_date DATE,
    end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    progress NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    budget NUMERIC(18, 2),
    currency_code VARCHAR(8) NOT NULL DEFAULT 'EUR',
    client_name VARCHAR(255),
    client_email VARCHAR(255),
    client_phone VARCHAR(32),
    sync_status VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(128),
    updated_by VARCHAR(128)
);

-- Indexes
CREATE INDEX idx_projects_tenant_id ON projects (tenant_id);

CREATE INDEX idx_projects_org_id ON projects (org_id);

CREATE UNIQUE INDEX idx_projects_external_id ON projects (external_id)
WHERE
    external_id IS NOT NULL;

CREATE UNIQUE INDEX idx_projects_code ON projects (tenant_id, code);

CREATE INDEX idx_projects_status ON projects (status);

CREATE INDEX idx_projects_tenant_org ON projects (tenant_id, org_id);

CREATE INDEX idx_projects_sync_status ON projects (sync_status);

COMMENT ON
TABLE projects IS 'Construction projects and project portfolios, scoped by tenant and org';

COMMENT ON COLUMN projects.status IS 'PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED, SUSPENDED';

COMMENT ON COLUMN projects.project_type IS 'NEW_CONSTRUCTION, RENOVATION, MAINTENANCE, INFRASTRUCTURE, COMMERCIAL, RESIDENTIAL, INDUSTRIAL';

COMMENT ON COLUMN projects.sync_status IS 'LOCAL, REMOTE, CONFLICT, PENDING_SYNC';