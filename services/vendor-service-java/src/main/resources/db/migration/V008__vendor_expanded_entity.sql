-- V008: Expand vendor entity with full attribute model + sub-entities
-- Adds: user_id, phone, website, address, city, country, vendor_types,
--        category_group_ids, capability_ids, project_types, years_in_business,
--        description, logo_url, registration_number, regions
--        plus vendor_certifications and vendor_projects sub-entities

ALTER TABLE vendor.vendors
  ADD COLUMN IF NOT EXISTS user_id UUID,
  ADD COLUMN IF NOT EXISTS phone TEXT,
  ADD COLUMN IF NOT EXISTS website TEXT,
  ADD COLUMN IF NOT EXISTS address TEXT,
  ADD COLUMN IF NOT EXISTS city TEXT,
  ADD COLUMN IF NOT EXISTS country TEXT,
  ADD COLUMN IF NOT EXISTS description TEXT,
  ADD COLUMN IF NOT EXISTS logo_url TEXT,
  ADD COLUMN IF NOT EXISTS registration_number TEXT,
  ADD COLUMN IF NOT EXISTS vendor_types TEXT[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS category_group_ids UUID[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS capability_ids UUID[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS project_types TEXT[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS years_in_business INTEGER,
  ADD COLUMN IF NOT EXISTS regions TEXT[] DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_vendors_user_id ON vendor.vendors (user_id);

-- =====================================================================
-- V008 (originally V006): Vendor Certifications table
-- =====================================================================
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

CREATE INDEX IF NOT EXISTS idx_vendor_certifications_vendor_id
    ON vendor.vendor_certifications(vendor_id);

-- =====================================================================
-- V008 (originally V006): Vendor Projects (Experience/Portfolio) table
-- =====================================================================
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

CREATE INDEX IF NOT EXISTS idx_vendor_projects_vendor_id
    ON vendor.vendor_projects(vendor_id);