-- V9: Add fixed, exhaustive account "Type" to user_accounts.
--
-- Every user account is exactly one of USER, VENDOR, COMPANY. Distinct from "roles"
-- (identity.memberships.roles / identity.resource_members.roles), which are tenant- or
-- resource-scoped and assignable independent of Type.
--
-- Backfill: anyone with "vendor" in their Membership.roles (case-insensitive) becomes
-- VENDOR. Everyone else backfills to USER. No COMPANY rows exist in seed data yet.

ALTER TABLE identity.user_accounts ADD COLUMN IF NOT EXISTS type TEXT;

UPDATE identity.user_accounts ua
SET type = 'VENDOR'
WHERE type IS NULL
  AND EXISTS (
    SELECT 1 FROM identity.memberships m
    WHERE m.user_id = ua.id
      AND EXISTS (
        SELECT 1 FROM jsonb_array_elements_text(m.roles) AS role
        WHERE role ILIKE 'vendor'
      )
  );

UPDATE identity.user_accounts SET type = 'USER' WHERE type IS NULL;

ALTER TABLE identity.user_accounts ALTER COLUMN type SET DEFAULT 'USER';
ALTER TABLE identity.user_accounts ALTER COLUMN type SET NOT NULL;
ALTER TABLE identity.user_accounts
    ADD CONSTRAINT user_accounts_type_check CHECK (type IN ('USER', 'VENDOR', 'COMPANY'));

COMMENT ON COLUMN identity.user_accounts.type IS
    'Fixed, exhaustive account type: USER, VENDOR, or COMPANY. Immutable after creation.';
