-- Vendor Service: Initial Schema (Phase 2 foundations)
-- Schema: supply

CREATE SCHEMA IF NOT EXISTS supply;

-- Vendors master table
CREATE TABLE IF NOT EXISTS supply.vendors (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    legal_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending', -- pending|approved|rejected
    tax_profile JSONB,                      -- arbitrary tax profile structure incl. tax IDs
    categories TEXT[] DEFAULT '{}',         -- vendor categories (tags)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_vendors_tenant_id ON supply.vendors (tenant_id);

CREATE INDEX IF NOT EXISTS idx_vendors_status ON supply.vendors (status);
-- Optional GIN index if categories filtering is frequent (uncomment if needed)
-- CREATE INDEX IF NOT EXISTS idx_vendors_categories_gin ON supply.vendors USING GIN (categories);

-- Vendor compliance/reference documents
CREATE TABLE IF NOT EXISTS supply.vendor_documents (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL REFERENCES supply.vendors (id) ON DELETE CASCADE,
    doc_type TEXT NOT NULL, -- e.g., ins_certificate, tax_clearance, etc.
    doc_ref TEXT NOT NULL, -- reference to DMS document ID or external link
    status TEXT NOT NULL DEFAULT 'pending', -- pending|approved|expired
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vendor_documents_vendor_id ON supply.vendor_documents (vendor_id);

CREATE INDEX IF NOT EXISTS idx_vendor_documents_doc_type ON supply.vendor_documents (doc_type);

-- Vendor ratings (simple rolling entries; average computed in views/queries)
CREATE TABLE IF NOT EXISTS supply.vendor_ratings (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL REFERENCES supply.vendors (id) ON DELETE CASCADE,
    score INT NOT NULL CHECK (score BETWEEN 1 AND 5),
    rationale TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vendor_ratings_vendor_id ON supply.vendor_ratings (vendor_id);

-- Optional: simple directory view for listing with average rating
CREATE OR REPLACE VIEW supply.vendor_directory AS
SELECT
    v.tenant_id,
    v.id,
    v.legal_name,
    v.status,
    v.categories,
    COALESCE(AVG(r.score)::NUMERIC(4,2), 0)::NUMERIC(4,2) AS rating_avg,
    v.created_at,
    v.updated_at
FROM supply.vendors v
LEFT JOIN supply.vendor_ratings r ON r.vendor_id = v.id
GROUP BY v.tenant_id, v.id, v.legal_name, v.status, v.categories, v.created_at, v.updated_at;

-- Note on RLS:
-- Phase 2 requires tenant isolation via RLS keyed by tenant_id. Introduced in V002__vendor_rls_and_policies.sql.
-- Application should set current_setting('app.tenant_id', true) per request/session.