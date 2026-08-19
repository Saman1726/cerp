-- V003: Add city field and search indexes to Vendor Service
-- Adds city column for vendor location and GIN indexes for efficient search.

-- Ensure the vendor schema is in search_path so pg_trgm gets created there
SET search_path TO vendor, public;

-- Enable pg_trgm extension (database-wide, but scoped to search_path context)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Add city column to vendors table
ALTER TABLE vendor.vendors ADD COLUMN IF NOT EXISTS city TEXT;

-- GIN index on categories for efficient array containment queries
CREATE INDEX IF NOT EXISTS idx_vendors_categories_gin ON vendor.vendors USING GIN (categories);

-- GIN index on legal_name for trigram/partial text search
CREATE INDEX IF NOT EXISTS idx_vendors_legal_name_gin ON vendor.vendors USING GIN (legal_name gin_trgm_ops);

-- GIN index on city for efficient city lookups
CREATE INDEX IF NOT EXISTS idx_vendors_city_gin ON vendor.vendors USING GIN (city gin_trgm_ops);