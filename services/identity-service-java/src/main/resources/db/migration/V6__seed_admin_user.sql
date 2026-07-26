-- V6: Seed default admin user for development/testing
-- This provides an admin account that can manage projects and assign users.
--
-- Credentials:
--   Username: Admin (for login lookup by name)
--   Email: admin@cerp.dev
--   Password: Admin123!@#
--   Role: admin (grants tenant-level admin/manager access)

CREATE SCHEMA IF NOT EXISTS identity;

-- Ensure default tenant exists
INSERT INTO
    identity.tenants (id, name, region)
VALUES (
        '00000000-0000-0000-0000-000000000001',
        'Default Tenant',
        'local'
    ) ON CONFLICT (id) DO NOTHING;

-- Seed admin user (password_hash is BCrypt hash of 'Admin123!@#')
-- Generated with BCrypt round=12 using spring.security.crypto.bcrypt encoder
INSERT INTO
    identity.user_accounts (
        id,
        email,
        name,
        password_hash,
        status
    )
VALUES (
        '00000000-0000-0000-0000-000000000010',
        'admin@cerp.dev',
        'Admin',
        -- BCrypt hash for 'Admin123!@#' - generated with rounds=12
        '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq',
        'active'
    ) ON CONFLICT (email) DO
UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    status = EXCLUDED.status;

-- Create admin membership (tenant-level admin role)
-- NOTE: memberships table uses 'roles' JSONB column (plural), not 'role'
-- Generate a deterministic UUID for the membership record
-- Only add if no membership exists for this user in this tenant
INSERT INTO
    identity.memberships (id, tenant_id, user_id, roles)
SELECT gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001', -- default tenant
    '00000000-0000-0000-0000-000000000010', -- admin user
    '["admin"]'::jsonb
WHERE
    NOT EXISTS (
        SELECT 1
        FROM identity.memberships
        WHERE
            tenant_id = '00000000-0000-0000-0000-000000000001'
            AND user_id = '00000000-0000-0000-0000-000000000010'
    );

COMMENT ON
TABLE identity.user_accounts IS 'Users with seeded admin account for development';