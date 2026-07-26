-- V1: Identity service initial schema
-- Schema
CREATE SCHEMA IF NOT EXISTS identity;

-- Tenants
CREATE TABLE IF NOT EXISTS identity.tenants (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    region TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Users (user_accounts for consistency with entity mapping)
CREATE TABLE IF NOT EXISTS identity.user_accounts (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    name TEXT,
    password_hash VARCHAR(255),
    status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Memberships
CREATE TABLE IF NOT EXISTS identity.memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity.tenants (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES identity.user_accounts (id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    UNIQUE (tenant_id, user_id)
);

-- Resource Memberships (for fine-grained access control)
CREATE TABLE IF NOT EXISTS identity.resource_members (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES identity.user_accounts (id) ON DELETE CASCADE,
    resource_type TEXT NOT NULL,
    permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    roles JSONB NOT NULL DEFAULT '[]'::jsonb,
    invited_by UUID REFERENCES identity.user_accounts (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    UNIQUE (resource_id, user_id, resource_type)
);

-- Flyway schema history table must be created in the identity schema
-- Flyway handles this automatically, but we ensure the schema exists first