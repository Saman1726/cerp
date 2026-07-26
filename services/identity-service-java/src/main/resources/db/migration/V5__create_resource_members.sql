-- Create resource_members table for per-resource access control
-- This enables Users to be assigned to specific Projects or Construction Sites
-- with granular read/write/delete/assign permissions.
CREATE TABLE IF NOT EXISTS identity.resource_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(50) NOT NULL,  -- 'PROJECT' or 'CONSTRUCTION_SITE'
    resource_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    roles TEXT[] NOT NULL DEFAULT '{"viewer"}',
    permissions JSONB NOT NULL DEFAULT '{"read": true, "write": false, "delete": false, "assign": false}',
    invited_by UUID REFERENCES identity.users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(resource_type, resource_id, user_id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_resource_members_resource ON identity.resource_members (resource_type, resource_id);

CREATE INDEX idx_resource_members_user ON identity.resource_members (user_id);

-- Add update trigger so updated_at refreshes automatically
CREATE OR REPLACE FUNCTION identity._update_resource_members_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_resource_members_updated_at
    BEFORE UPDATE ON identity.resource_members
    FOR EACH ROW
    EXECUTE FUNCTION identity._update_resource_members_updated_at();

-- Seed data: Assign the existing seed users (Metal vendor, John) to specific resources
-- This will be done after the migration is applied