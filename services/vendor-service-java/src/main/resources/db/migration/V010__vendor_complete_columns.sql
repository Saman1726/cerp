-- V010: Ensure all Vendor entity columns exist in the database.
-- The vendor.vendors table schema may be out of sync with the Vendor entity
-- due to Flyway history inconsistency (e.g., after a database volume reset
-- where flyway_schema_history persisted but actual schema was cleared).
-- This migration is a safety net that adds ALL columns the entity expects,
-- using IF NOT EXISTS to be idempotent across environments.

ALTER TABLE vendor.vendors
  ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid(),
  ADD COLUMN IF NOT EXISTS user_id UUID,
  ADD COLUMN IF NOT EXISTS legal_name TEXT NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS contact_name TEXT,
  ADD COLUMN IF NOT EXISTS phone TEXT,
  ADD COLUMN IF NOT EXISTS website TEXT,
  ADD COLUMN IF NOT EXISTS address TEXT,
  ADD COLUMN IF NOT EXISTS city TEXT,
  ADD COLUMN IF NOT EXISTS country TEXT,
  ADD COLUMN IF NOT EXISTS description TEXT,
  ADD COLUMN IF NOT EXISTS logo_url TEXT,
  ADD COLUMN IF NOT EXISTS registration_number TEXT,
  ADD COLUMN IF NOT EXISTS years_in_business INTEGER,
  ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'pending',
  ADD COLUMN IF NOT EXISTS tax_profile JSONB,
  ADD COLUMN IF NOT EXISTS categories TEXT[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS vendor_types TEXT[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS category_group_ids UUID[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS capability_ids UUID[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS project_types TEXT[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS regions TEXT[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- Re-create indexes that may be missing
CREATE INDEX IF NOT EXISTS idx_vendors_tenant_id ON vendor.vendors (tenant_id);
CREATE INDEX IF NOT EXISTS idx_vendors_status ON vendor.vendors (status);
CREATE INDEX IF NOT EXISTS idx_vendors_user_id ON vendor.vendors (user_id);
CREATE INDEX IF NOT EXISTS idx_vendors_categories_gin ON vendor.vendors USING GIN (categories);
CREATE INDEX IF NOT EXISTS idx_vendors_legal_name_gin ON vendor.vendors USING GIN (legal_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_vendors_city_gin ON vendor.vendors USING GIN (city gin_trgm_ops);

-- Ensure pg_trgm is available
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Ensure sub-entities exist (idempotent)
CREATE TABLE IF NOT EXISTS vendor.vendor_certifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL REFERENCES vendor.vendors(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    issuing_body TEXT,
    issue_date DATE,
    expiry_date DATE,
    certificate_number TEXT,
    document_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_vendor_certifications_vendor_id ON vendor.vendor_certifications(vendor_id);

CREATE TABLE IF NOT EXISTS vendor.vendor_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL REFERENCES vendor.vendors(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    project_type TEXT,
    start_date DATE,
    end_date DATE,
    estimated_value NUMERIC(19, 2),
    image_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_vendor_projects_vendor_id ON vendor.vendor_projects(vendor_id);