-- V4: Add password hash column to users for authentication
-- NOTE: V1 already creates user_accounts with password_hash column
-- This migration is kept for backward compatibility but is now a no-op

CREATE SCHEMA IF NOT EXISTS identity;

-- password_hash already exists in V1
-- This migration ensures Flyway history is consistent