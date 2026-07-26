-- V2: Align users and memberships tables with Phase 1 contract
-- NOTE: V1 already creates the tables with correct schema (user_accounts, memberships)
-- This migration is kept for backward compatibility but is now a no-op
-- because V1 already includes:
-- - user_accounts.name (not display_name)
-- - user_accounts.status DEFAULT 'active'
-- - memberships.roles JSONB DEFAULT []
-- - memberships.scopes JSONB DEFAULT {}

-- Safety: ensure schema exists
CREATE SCHEMA IF NOT EXISTS identity;

-- All table structure changes were moved to V1__initial_schema.sql
-- This migration ensures Flyway history is consistent.

-- Update memberships to have roles and scopes if they somehow don't exist
-- (V1 already creates them, but this ensures idempotency)
DO $$ BEGIN IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE
        table_schema = 'identity'
        AND table_name = 'memberships'
        AND column_name = 'roles'
) THEN
EXECUTE 'ALTER TABLE identity.memberships ADD COLUMN roles JSONB NOT NULL DEFAULT ''[]''::jsonb';

END IF;

IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE
        table_schema = 'identity'
        AND table_name = 'memberships'
        AND column_name = 'scopes'
) THEN
EXECUTE 'ALTER TABLE identity.memberships ADD COLUMN scopes JSONB NOT NULL DEFAULT ''{}''::jsonb';

END IF;

-- Drop legacy role column if it somehow exists
IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE
        table_schema = 'identity'
        AND table_name = 'memberships'
        AND column_name = 'role'
) THEN
EXECUTE 'ALTER TABLE identity.memberships DROP COLUMN role';

END IF;

END $$;