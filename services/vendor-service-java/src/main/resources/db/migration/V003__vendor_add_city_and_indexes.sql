-- V003: Add city field and search indexes to Vendor Service
-- Adds city column for vendor location and GIN indexes for efficient search.

CREATE SCHEMA IF NOT EXISTS supply;

-- Enable pg_trgm extension FIRST (must be before trigram indexes)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Add city column to vendors table
ALTER TABLE supply.vendors ADD COLUMN IF NOT EXISTS city TEXT;

-- GIN index on categories for efficient array containment queries
CREATE INDEX IF NOT EXISTS idx_vendors_categories_gin ON supply.vendors USING GIN (categories);

-- GIN index on legal_name for trigram/partial text search
CREATE INDEX IF NOT EXISTS idx_vendors_legal_name_gin ON supply.vendors USING GIN (legal_name gin_trgm_ops);

-- GIN index on city for efficient city lookups
CREATE INDEX IF NOT EXISTS idx_vendors_city_gin ON supply.vendors USING GIN (city gin_trgm_ops);