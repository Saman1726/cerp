-- V2: Enable Row Level Security (RLS) and tenant isolation policies for Vendor Service
-- Uses current_setting('app.tenant_id', true) to enforce per-tenant access.
-- Application layer should set: SET LOCAL app.tenant_id = '<tenant-uuid>';

-- Safety: ensure schema exists
CREATE SCHEMA IF NOT EXISTS supply;

-- Enable RLS on multi-tenant root table
ALTER TABLE supply.vendors ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'supply' AND tablename = 'vendors' AND policyname = 'vendors_tenant_isolation'
  ) THEN
    EXECUTE 'DROP POLICY vendors_tenant_isolation ON supply.vendors';
  END IF;
END
$$;

CREATE POLICY vendors_tenant_isolation
  ON supply.vendors
  FOR ALL
  USING (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  );

-- Child tables reference vendors; enforce access via vendor's tenant linkage

-- vendor_documents
ALTER TABLE supply.vendor_documents ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'supply' AND tablename = 'vendor_documents' AND policyname = 'vendor_documents_tenant_isolation'
  ) THEN
    EXECUTE 'DROP POLICY vendor_documents_tenant_isolation ON supply.vendor_documents';
  END IF;
END
$$;

CREATE POLICY vendor_documents_tenant_isolation
  ON supply.vendor_documents
  FOR ALL
  USING (
    current_setting('app.tenant_id', true) IS NULL
    OR EXISTS (
      SELECT 1 FROM supply.vendors v
      WHERE v.id = vendor_id
        AND v.tenant_id::text = current_setting('app.tenant_id', true)
    )
  )
  WITH CHECK (
    current_setting('app.tenant_id', true) IS NULL
    OR EXISTS (
      SELECT 1 FROM supply.vendors v
      WHERE v.id = vendor_id
        AND v.tenant_id::text = current_setting('app.tenant_id', true)
    )
  );

-- vendor_ratings
ALTER TABLE supply.vendor_ratings ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'supply' AND tablename = 'vendor_ratings' AND policyname = 'vendor_ratings_tenant_isolation'
  ) THEN
    EXECUTE 'DROP POLICY vendor_ratings_tenant_isolation ON supply.vendor_ratings';
  END IF;
END
$$;

CREATE POLICY vendor_ratings_tenant_isolation
  ON supply.vendor_ratings
  FOR ALL
  USING (
    current_setting('app.tenant_id', true) IS NULL
    OR EXISTS (
      SELECT 1 FROM supply.vendors v
      WHERE v.id = vendor_id
        AND v.tenant_id::text = current_setting('app.tenant_id', true)
    )
  )
  WITH CHECK (
    current_setting('app.tenant_id', true) IS NULL
    OR EXISTS (
      SELECT 1 FROM supply.vendors v
      WHERE v.id = vendor_id
        AND v.tenant_id::text = current_setting('app.tenant_id', true)
    )
  );

-- Helpful indexes (if not already existing)
CREATE INDEX IF NOT EXISTS idx_vendors_tenant_id ON supply.vendors (tenant_id);

CREATE INDEX IF NOT EXISTS idx_vendor_documents_vendor_id ON supply.vendor_documents (vendor_id);

CREATE INDEX IF NOT EXISTS idx_vendor_ratings_vendor_id ON supply.vendor_ratings (vendor_id);