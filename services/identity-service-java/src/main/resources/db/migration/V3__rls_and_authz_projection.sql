-- V3: Introduce RLS for multi-tenant table(s) and add initial authz_projection
-- Notes:
-- - Phase 1 requires RLS-based isolation on multi-tenant tables keyed by tenant_id.
-- - For Identity service, memberships is tenant-scoped; tenants/users are global and remain without RLS.
-- - Policy uses current_setting('app.tenant_id', true). If not set (NULL), policy allows access to avoid breaking local/dev.
--   Application should set `SET app.tenant_id = '<tenant-uuid>';` per request/session to enforce scoping strictly.

-- Safety: ensure schema exists
CREATE SCHEMA IF NOT EXISTS identity;

-- Helpful index for tenant-scoped queries (unique (tenant_id, user_id) already exists from V1)
CREATE INDEX IF NOT EXISTS idx_memberships_tenant_id ON identity.memberships (tenant_id);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON identity.memberships (user_id);

-- Enable Row Level Security on memberships
ALTER TABLE identity.memberships ENABLE ROW LEVEL SECURITY;

-- Drop any existing policies to allow re-create idempotently
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_policies
    WHERE schemaname = 'identity' AND tablename = 'memberships' AND policyname = 'memberships_tenant_isolation'
  ) THEN
    EXECUTE 'DROP POLICY memberships_tenant_isolation ON identity.memberships';
  END IF;
END
$$;

-- Create a single policy covering all commands (SELECT/INSERT/UPDATE/DELETE)
CREATE POLICY memberships_tenant_isolation
  ON identity.memberships
  FOR ALL
  USING (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  );

-- Initial authz_projection: flattens memberships for fast authorization checks
-- Keeping roles/scopes as JSONB for flexibility; can evolve to computed claims if needed.
CREATE OR REPLACE VIEW identity.authz_projection AS
SELECT m.tenant_id, m.user_id, m.roles, -- JSONB array of role strings
    m.scopes, -- JSONB object
    m.created_at, m.updated_at
FROM identity.memberships m;

-- Optional convenience index for common projection lookup paths (materialized view not used in V3)
-- (Indexes on underlying table already created above.)