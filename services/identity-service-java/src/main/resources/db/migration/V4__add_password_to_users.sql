-- V4: Add password hash column to users for authentication
-- Notes:
-- - password_hash is nullable because some users may be managed via Keycloak SSO only
-- - BCrypt hashes are stored (minimum 60 chars, max 255 chars)

-- Safety: ensure schema exists
CREATE SCHEMA IF NOT EXISTS identity;

-- Add password_hash column to user_accounts table
ALTER TABLE identity.user_accounts
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Add updated_at trigger support column if not exists
-- (updated_at already exists, but ensure the column is not null for new rows)
ALTER TABLE identity.user_accounts
ALTER COLUMN password_hash
SET DEFAULT NULL;

-- Add comment for documentation
COMMENT ON COLUMN identity.user_accounts.password_hash IS 'BCrypt hashed password. NULL means user is managed via external auth (e.g., Keycloak SSO).';