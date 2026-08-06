-- V5 seeded bid wallets against placeholder user_ids (...0001/0002/0003) that matched
-- no row in identity.user_accounts, so the frontend bid wallet balance always showed 0.
-- This migration removes those placeholder wallets and reseeds real dev/test personas.
--
-- Rather than hardcoding a user_id, each persona is resolved by email against
-- identity.user_accounts at migration time. identity-service and workflow-service
-- share the same Postgres instance (see CLAUDE.md), but hardcoding a specific id is
-- unreliable: e.g. identity-service's SeedDataConfig CommandLineRunner creates
-- admin@cerp.dev/john@cerp.dev/metal@cerp.dev with a random UUID on first run, and if
-- that ever races ahead of identity's own Flyway migrations (which try to fix admin's
-- id at '...0010'), the random id silently wins and sticks - exactly what caused the
-- original bug. Looking up by email sidesteps that regardless of which id "won".
--
-- Guarded so this is a no-op (not an error) if workflow-service's Flyway happens to
-- run before identity-service has created its schema - a real possibility since
-- docker-compose has no depends_on between the two services.

DO $$
DECLARE
    default_tenant UUID  := '00000000-0000-0000-0000-000000000001';
    seed_amount    BIGINT := 1000;
    wid            UUID;
    rec            RECORD;
BEGIN
    -- Remove the V5 placeholder wallets (and their allocation transactions).
    DELETE FROM bidding.bid_transactions
    WHERE wallet_id IN (
        SELECT id FROM bidding.bid_wallets
        WHERE user_id IN (
            '00000000-0000-0000-0000-000000000001',
            '00000000-0000-0000-0000-000000000002',
            '00000000-0000-0000-0000-000000000003'
        )
    );
    DELETE FROM bidding.bid_wallets
    WHERE user_id IN (
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000003'
    );

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'identity' AND table_name = 'user_accounts'
    ) THEN
        RAISE NOTICE 'identity.user_accounts not present yet; skipping bid wallet reseed';
        RETURN;
    END IF;

    FOR rec IN
        SELECT ua.id AS user_id, persona.entity_type
        FROM identity.user_accounts ua
        JOIN (VALUES
            -- Project owners (create biddings)
            ('admin@cerp.dev',           'PROJECT_OWNER'),
            ('john@cerp.dev',            'PROJECT_OWNER'),
            ('sarah.owens@cerp.dev',     'PROJECT_OWNER'),
            ('laura.fischer@cerp.dev',   'PROJECT_OWNER'),
            -- Vendors (pay bids to participate in open tenders)
            ('metal@cerp.dev',           'VENDOR'),
            ('contact@steelsupplyco.dev','VENDOR'),
            ('sales@concreteworks.dev',  'VENDOR'),
            ('info@elektrosystems.dev',  'VENDOR'),
            ('office@aquaplumb.dev',     'VENDOR'),
            ('orders@timberandco.dev',   'VENDOR'),
            ('contact@glassfacade.dev',  'VENDOR'),
            ('info@roofingpros.dev',     'VENDOR'),
            ('sales@hvacmasters.dev',    'VENDOR')
        ) AS persona(email, entity_type) ON ua.email = persona.email
    LOOP
        IF NOT EXISTS (SELECT 1 FROM bidding.bid_wallets WHERE user_id = rec.user_id) THEN
            INSERT INTO bidding.bid_wallets (tenant_id, user_id, entity_type, entity_id, total_bids, spent_bids, available_bids, created_at, updated_at)
            VALUES (default_tenant, rec.user_id, rec.entity_type, rec.user_id, seed_amount, 0, seed_amount, NOW(), NOW())
            RETURNING id INTO wid;

            INSERT INTO bidding.bid_transactions (tenant_id, user_id, wallet_id, transaction_type, amount, unit_price, total_price, balance_after, reference_type, description, created_at)
            VALUES (default_tenant, rec.user_id, wid, 'ALLOCATION', seed_amount, 0, 0, seed_amount, 'SEED_ALLOCATION', 'Dev seed: 1000 bids allocated', NOW());
        END IF;
    END LOOP;
END $$;
