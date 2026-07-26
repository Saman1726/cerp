-- V5: Create resource_members table for fine-grained access control
-- NOTE: V1 already creates the resource_members table
-- This migration is kept for backward compatibility but is now a no-op

CREATE SCHEMA IF NOT EXISTS identity;

-- resource_members already exists in V1 with correct schema
-- This migration ensures Flyway history is consistent