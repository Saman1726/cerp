-- Identity service initial schema
-- Schema
CREATE SCHEMA IF NOT EXISTS identity;

-- Tenants
CREATE TABLE IF NOT EXISTS identity.tenants (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Users
CREATE TABLE IF NOT EXISTS identity.users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    display_name TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Memberships
CREATE TABLE IF NOT EXISTS identity.memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity.tenants (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES identity.users (id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    UNIQUE (tenant_id, user_id)
);