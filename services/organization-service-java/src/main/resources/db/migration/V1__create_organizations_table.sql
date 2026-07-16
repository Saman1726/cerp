-- V1: Create organizations table
-- Creates the core organizations table with tenant isolation

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    tenant_id VARCHAR(255) NOT NULL,
    external_id VARCHAR(128),
    name VARCHAR(255) NOT NULL,
    legal_form VARCHAR(64),
    tax_id VARCHAR(64),
    commercial_register VARCHAR(64),
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    postal_code VARCHAR(20),
    city VARCHAR(128),
    country_code VARCHAR(4),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    contact_email VARCHAR(255),
    contact_phone VARCHAR(32),
    website VARCHAR(255),
    currency_code VARCHAR(8) NOT NULL DEFAULT 'EUR',
    sync_status VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(128),
    updated_by VARCHAR(128)
);

-- Indexes
CREATE INDEX idx_organizations_tenant_id ON organizations (tenant_id);

CREATE UNIQUE INDEX idx_organizations_external_id ON organizations (external_id)
WHERE
    external_id IS NOT NULL;

CREATE INDEX idx_organizations_name ON organizations (name);

CREATE INDEX idx_organizations_status ON organizations (status);

CREATE INDEX idx_organizations_sync_status ON organizations (sync_status);

CREATE INDEX idx_organizations_tenant_status ON organizations (tenant_id, status);

-- Comment
COMMENT ON
TABLE organizations IS 'Business organization profiles, scoped by tenant for RLS';

COMMENT ON COLUMN organizations.status IS 'ACTIVE, INACTIVE, SUSPENDED, UNDER_REVIEW';

COMMENT ON COLUMN organizations.sync_status IS 'LOCAL, REMOTE, CONFLICT, PENDING_SYNC';