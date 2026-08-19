-- V007: Add vendor_id and vendor_type to catalog_items
-- Enables vendor-owned product/service catalog in catalog-service
-- Note: vendor_id is a UUID reference only; vendor data is resolved via vendor-service API

ALTER TABLE catalog.catalog_items
  ADD COLUMN IF NOT EXISTS vendor_id UUID,
  ADD COLUMN IF NOT EXISTS vendor_type TEXT DEFAULT 'PRODUCT'
    CHECK (vendor_type IN ('PRODUCT', 'SERVICE'));

CREATE INDEX IF NOT EXISTS idx_catalog_items_vendor_id
    ON catalog.catalog_items(vendor_id);
CREATE INDEX IF NOT EXISTS idx_catalog_items_vendor_type
    ON catalog.catalog_items(vendor_type);