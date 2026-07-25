-- V006: Enable Row Level Security (RLS) and tenant isolation policies for Catalog Service
-- Uses current_setting('app.tenant_id', true) to enforce per-tenant access.
-- Application layer should set: SET LOCAL app.tenant_id = '<tenant-uuid>';

-- Safety: ensure schema exists
CREATE SCHEMA IF NOT EXISTS supply;

-- Enable RLS on multi-tenant root table
ALTER TABLE supply.catalog_items ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'supply' AND tablename = 'catalog_items' AND policyname = 'catalog_items_tenant_isolation'
  ) THEN
    EXECUTE 'DROP POLICY catalog_items_tenant_isolation ON supply.catalog_items';
  END IF;
END
$$;

CREATE POLICY catalog_items_tenant_isolation
  ON supply.catalog_items
  FOR ALL
  USING (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.tenant_id', true) IS NULL
    OR tenant_id::text = current_setting('app.tenant_id', true)
  );

-- Child table references catalog_items; enforce access via item's tenant linkage

-- catalog_prices
ALTER TABLE supply.catalog_prices ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'supply' AND tablename = 'catalog_prices' AND policyname = 'catalog_prices_tenant_isolation'
  ) THEN
    EXECUTE 'DROP POLICY catalog_prices_tenant_isolation ON supply.catalog_prices';
  END IF;
END
$$;

CREATE POLICY catalog_prices_tenant_isolation
  ON supply.catalog_prices
  FOR ALL
  USING (
    current_setting('app.tenant_id', true) IS NULL
    OR EXISTS (
      SELECT 1 FROM supply.catalog_items i
      WHERE i.id = item_id
        AND i.tenant_id::text = current_setting('app.tenant_id', true)
    )
  )
  WITH CHECK (
    current_setting('app.tenant_id', true) IS NULL
    OR EXISTS (
      SELECT 1 FROM supply.catalog_items i
      WHERE i.id = item_id
        AND i.tenant_id::text = current_setting('app.tenant_id', true)
    )
  );

-- Helpful indexes (root and FKs already added in V005)