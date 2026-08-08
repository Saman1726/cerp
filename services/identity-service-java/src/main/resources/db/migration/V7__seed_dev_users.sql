-- V7: Seed 20 development users in the default tenant with varied membership roles,
-- plus fixed ids for the "metal"/"john" personas.
--
-- These users have deterministic ids (unlike "metal"/"john" below, which used to be
-- seeded by SeedDataConfig's CommandLineRunner with a random UUID picked at first
-- startup), so other services can safely reference them by a fixed user_id in their
-- own seed migrations. This fixes bidding.bid_wallets (workflow-service V5) referencing
-- placeholder user_ids that matched no row in identity.user_accounts, which made the
-- frontend bid wallet balance always resolve to 0.
--
-- All users share the dev password 'Admin123!@#' (same BCrypt hash as the admin user
-- seeded in V6) for convenience when logging in locally.

CREATE SCHEMA IF NOT EXISTS identity;

-- "metal" (vendor) and "john" (project owner) used to only exist via SeedDataConfig's
-- CommandLineRunner, which assigns a random UUID on first run - unlike admin@cerp.dev
-- (fixed at ...0010 by V6), there was nothing pinning these two to a stable id, so a
-- container restart could re-seed them under a new UUID and orphan any data (bid
-- wallets, resource memberships, etc.) keyed to the old one. Fixed ids here make them
-- as stable as the admin account. SeedDataConfig still runs on every startup and finds
-- these rows already present (by email), so its user/membership/subscription creation
-- for metal/john is now a no-op; it only still does real work seeding john's
-- construction site/project resource memberships.
INSERT INTO
    identity.tenants (id, name, region)
VALUES (
        '00000000-0000-0000-0000-000000000002',
        'John''s Workspace',
        'john'
    ) ON CONFLICT (id) DO NOTHING;

-- No "type" column here (deliberately, unlike the persona block below which also
-- omits it): user_accounts.type doesn't exist until V9, which backfills it from
-- Membership.roles - VENDOR for metal (via the "vendor" role membership inserted
-- below), USER for john. Setting it here would fail against this migration's own
-- point-in-time schema.
INSERT INTO
    identity.user_accounts (id, email, name, password_hash, status)
VALUES
    (
        '00000000-0000-0000-0000-000000000008',
        'metal@cerp.dev',
        'Metal',
        -- BCrypt hash of 'secret' - the password SeedDataConfig has always used for
        -- this persona.
        '$2a$10$NM56cDPJzmija8ySMSzbg.1Znwzt1k.Vxi4MORIv0dtVr96MDf1ru',
        'active'
    ),
    (
        '00000000-0000-0000-0000-000000000009',
        'john@cerp.dev',
        'John',
        '$2a$10$NM56cDPJzmija8ySMSzbg.1Znwzt1k.Vxi4MORIv0dtVr96MDf1ru',
        'active'
    )
ON CONFLICT (email) DO NOTHING;

INSERT INTO
    identity.memberships (id, tenant_id, user_id, roles)
VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000008', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000009', '["user", "owner"]'::jsonb)
ON CONFLICT (tenant_id, user_id) DO NOTHING;

INSERT INTO
    identity.user_accounts (id, email, name, password_hash, status)
VALUES
    ('00000000-0000-0000-0000-000000000011', 'sarah.owens@cerp.dev', 'Sarah Owens', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000012', 'marcus.chen@cerp.dev', 'Marcus Chen', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000013', 'priya.patel@cerp.dev', 'Priya Patel', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000014', 'diego.alvarez@cerp.dev', 'Diego Alvarez', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000015', 'fatima.alsayed@cerp.dev', 'Fatima Al-Sayed', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000016', 'tom.becker@cerp.dev', 'Tom Becker', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000017', 'nina.kowalski@cerp.dev', 'Nina Kowalski', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000018', 'wei.zhang@cerp.dev', 'Wei Zhang', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000019', 'contact@steelsupplyco.dev', 'Steel Supply Co', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000020', 'sales@concreteworks.dev', 'Concrete Works Ltd', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000021', 'info@elektrosystems.dev', 'Elektro Systems', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000022', 'office@aquaplumb.dev', 'AquaPlumb Solutions', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000023', 'orders@timberandco.dev', 'Timber & Co', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000024', 'contact@glassfacade.dev', 'Glass Facade Experts', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000025', 'info@roofingpros.dev', 'Roofing Pros', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000026', 'sales@hvacmasters.dev', 'HVAC Masters', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000027', 'laura.fischer@cerp.dev', 'Laura Fischer', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000028', 'omar.haddad@cerp.dev', 'Omar Haddad', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000029', 'ben.novak@cerp.dev', 'Ben Novak', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active'),
    ('00000000-0000-0000-0000-000000000030', 'grace.lindqvist@cerp.dev', 'Grace Lindqvist', '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq', 'active')
ON CONFLICT (email) DO NOTHING;

-- Membership roles vary per persona so the 20 users exercise different access levels:
--   owner    - property/project owner, creates biddings
--   manager  - project/procurement manager
--   admin    - tenant administrator
--   vendor   - supplier, participates in open tenders
--   user     - regular staff, no elevated access
--   viewer   - read-only access
INSERT INTO
    identity.memberships (id, tenant_id, user_id, roles)
VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000011', '["owner"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000012', '["manager"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000013', '["manager"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000014', '["manager", "user"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000015', '["admin"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000016', '["user"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000017', '["user"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000018', '["viewer"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000019', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000020', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000021', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000022', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000023', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000024', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000025', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000026', '["vendor"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000027', '["owner", "user"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000028', '["manager"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000029', '["user"]'::jsonb),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000030', '["viewer"]'::jsonb)
ON CONFLICT (tenant_id, user_id) DO NOTHING;

COMMENT ON
TABLE identity.user_accounts IS 'Users: seeded admin (V6) plus metal/john and 20 dev personas with varied tenant roles (V7)';
