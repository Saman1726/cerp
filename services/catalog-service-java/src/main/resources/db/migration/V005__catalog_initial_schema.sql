-- Catalog Service: Initial Schema (Phase 2 foundations)
-- Schema: supply

CREATE SCHEMA IF NOT EXISTS supply;

-- Catalog items master table
CREATE TABLE IF NOT EXISTS supply.catalog_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    sku TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    attributes JSONB,                 -- arbitrary extensible attributes
    categories TEXT[] DEFAULT '{}',   -- tags/categories
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_catalog_items_tenant_id ON supply.catalog_items (tenant_id);

CREATE INDEX IF NOT EXISTS idx_catalog_items_sku ON supply.catalog_items (sku);

-- Catalog prices for items (history-friendly)
CREATE TABLE IF NOT EXISTS supply.catalog_prices (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES supply.catalog_items (id) ON DELETE CASCADE,
    currency CHAR(3) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_catalog_prices_item_id ON supply.catalog_prices (item_id);

CREATE INDEX IF NOT EXISTS idx_catalog_prices_item_id_valid_from ON supply.catalog_prices (item_id, valid_from DESC);

-- Note on RLS:
-- Phase 2 requires tenant isolation via RLS keyed by tenant_id.
-- Introduced in V006__catalog_rls_and_policies.sql. Application should set current_setting('app.tenant_id', true) per request/session.