-- V004: Enable Row Level Security (RLS) and tenant isolation policies for Procurement Service
-- Uses current_setting('app.tenant_id', true) to enforce per-tenant access.
-- Application layer should set: SET LOCAL app.tenant_id = '<tenant-uuid>';

-- Safety: ensure schema exists
CREATE SCHEMA IF NOT EXISTS procurement;

-- Enable RLS on multi-tenant root table
ALTER TABLE procurement.requests ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'procurement' AND tablename = 'requests' AND policyname = 'requests_tenant_isolation'
  ) THEN
    EXECUTE 'DROP POLICY requests_tenant_isolation ON procurement.requests';
  END IF;
END
$$;

CREATE POLICY requests_tenant_isolation
  ON procurement.requests
  FOR ALL
  USING (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  );

-- Helpful indexes (if not already existing)
CREATE INDEX IF NOT EXISTS idx_requests_tenant_id ON procurement.requests (tenant_id);