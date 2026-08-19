-- V007: Add vendor favorites (user-level) to Vendor Service
-- Each user can favorite vendors across tenants. Relations stored with user_id + vendor_id.

-- Vendor favorites: user-level favorite relations
CREATE TABLE IF NOT EXISTS vendor.vendor_favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    vendor_id UUID NOT NULL REFERENCES vendor.vendors (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vendor_favorites_user_vendor UNIQUE (user_id, vendor_id)
);

CREATE INDEX IF NOT EXISTS idx_vendor_favorites_user_id ON vendor.vendor_favorites (user_id);

CREATE INDEX IF NOT EXISTS idx_vendor_favorites_vendor_id ON vendor.vendor_favorites (vendor_id);