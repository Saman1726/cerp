-- V2: Align users and memberships tables with Phase 1 contract
-- - tenants.region TEXT
-- - users: name TEXT (rename from display_name), status TEXT DEFAULT 'active'
-- - memberships: roles JSONB (default []), scopes JSONB (default {}), drop legacy role TEXT

-- Tenants: add region
ALTER TABLE identity.tenants ADD COLUMN IF NOT EXISTS region TEXT;

-- Users: rename display_name -> name if present, ensure name exists, add status
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'identity'
      AND table_name = 'users'
      AND column_name = 'display_name'
  ) THEN
    EXECUTE 'ALTER TABLE identity.users RENAME COLUMN display_name TO name';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'identity'
      AND table_name = 'users'
      AND column_name = 'name'
  ) THEN
    EXECUTE 'ALTER TABLE identity.users ADD COLUMN name TEXT';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'identity'
      AND table_name = 'users'
      AND column_name = 'status'
  ) THEN
    EXECUTE 'ALTER TABLE identity.users ADD COLUMN status TEXT NOT NULL DEFAULT ''active''';
  END IF;
END
$$;

-- Memberships: add roles, scopes jsonb; drop legacy role column if exists
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'identity'
      AND table_name = 'memberships'
      AND column_name = 'roles'
  ) THEN
    EXECUTE 'ALTER TABLE identity.memberships ADD COLUMN roles JSONB NOT NULL DEFAULT ''[]''::jsonb';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'identity'
      AND table_name = 'memberships'
      AND column_name = 'scopes'
  ) THEN
    EXECUTE 'ALTER TABLE identity.memberships ADD COLUMN scopes JSONB NOT NULL DEFAULT ''{}''::jsonb';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'identity'
      AND table_name = 'memberships'
      AND column_name = 'role'
  ) THEN
    EXECUTE 'ALTER TABLE identity.memberships DROP COLUMN role';
  END IF;
END
$$;