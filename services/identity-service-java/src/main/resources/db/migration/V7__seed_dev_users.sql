-- V7: Seed 20 development users in the default tenant with varied membership roles.
--
-- These users have deterministic ids (unlike the "metal"/"john" personas seeded by
-- SeedDataConfig's CommandLineRunner, which get a random UUID at first startup), so
-- other services can safely reference them by a fixed user_id in their own seed
-- migrations. This fixes bidding.bid_wallets (workflow-service V5) referencing
-- placeholder user_ids that matched no row in identity.user_accounts, which made the
-- frontend bid wallet balance always resolve to 0.
--
-- All users share the dev password 'Admin123!@#' (same BCrypt hash as the admin user
-- seeded in V6) for convenience when logging in locally.

CREATE SCHEMA IF NOT EXISTS identity;

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
TABLE identity.user_accounts IS 'Users: seeded admin (V6) plus 20 dev personas with varied tenant roles (V7)';
