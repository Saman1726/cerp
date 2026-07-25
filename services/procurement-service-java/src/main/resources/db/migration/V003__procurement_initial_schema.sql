-- Procurement Service: Initial Schema (Phase 2 foundations)
-- Schema: procurement

CREATE SCHEMA IF NOT EXISTS procurement;

-- Procurement Requests master table
CREATE TABLE IF NOT EXISTS procurement.requests (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'draft', -- draft|submitted|approved|ordered|closed
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_requests_tenant_id ON procurement.requests (tenant_id);

CREATE INDEX IF NOT EXISTS idx_requests_status ON procurement.requests (status);

-- Note on RLS:
-- Phase 2 requires tenant isolation via RLS keyed by tenant_id. Introduced in V004__procurement_rls_and_policies.sql.
-- Application should set current_setting('app.tenant_id', true) per request/session.