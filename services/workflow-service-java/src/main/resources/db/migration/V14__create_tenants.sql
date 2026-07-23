-- Create tenants table
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add index for faster lookups
CREATE INDEX IF NOT EXISTS idx_tenants_code ON tenants (code);

CREATE INDEX IF NOT EXISTS idx_tenants_is_active ON tenants (is_active);

-- Seed the default tenant (UUID is deterministic for reproducibility)
INSERT INTO
    tenants (
        id,
        code,
        name,
        description,
        is_active
    )
VALUES (
        '00000000-0000-0000-0000-000000000001',
        'default',
        'Default Tenant',
        'System default tenant for development and initial setup',
        TRUE
    ) ON CONFLICT (code) DO NOTHING;