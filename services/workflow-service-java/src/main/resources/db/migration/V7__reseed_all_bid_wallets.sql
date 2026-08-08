-- V6 tried to reseed real bid wallets (after removing V5's placeholder rows) by joining a
-- fixed persona list against identity.user_accounts, keyed by email. But docker-compose has
-- no depends_on between workflow-service and identity-service, so V6 ran before
-- identity-service had created its schema. Its guard silently no-op'd (RAISE NOTICE +
-- RETURN) instead of failing, so Flyway recorded V6 as successful even though it inserted
-- nothing - and by then it had already deleted V5's placeholder rows. Net effect:
-- bidding.bid_wallets has been empty in every environment since the original seed.
--
-- This migration reseeds every user in identity.user_accounts (not a fixed persona list),
-- so it also covers personas a hardcoded list would miss (e.g. manager/admin/user/viewer
-- roles) and any real signup. entity_type is VENDOR for any user with a "vendor" role in
-- any of their tenant memberships, PROJECT_OWNER otherwise. Users with no membership at all
-- are skipped since there's no tenant to attribute the wallet to.
--
-- Guarded the same way V6 was: if identity.user_accounts doesn't exist yet, this is a
-- silent no-op rather than a failure. That race is now also addressed by adding
-- workflow-service's depends_on identity-service in docker-compose.yml, but the guard stays
-- as a safety net for environments that don't use that compose file.

DO $$
DECLARE
    seed_amount BIGINT := 1000;
    wid         UUID;
    rec         RECORD;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'identity' AND table_name = 'user_accounts'
    ) THEN
        RAISE NOTICE 'identity.user_accounts not present yet; skipping bid wallet reseed';
        RETURN;
    END IF;

    FOR rec IN
        SELECT
            ua.id AS user_id,
            MIN(m.tenant_id::text)::uuid AS tenant_id,
            CASE WHEN bool_or(m.roles @> '["vendor"]'::jsonb) THEN 'VENDOR' ELSE 'PROJECT_OWNER' END AS entity_type
        FROM identity.user_accounts ua
        JOIN identity.memberships m ON m.user_id = ua.id
        GROUP BY ua.id
    LOOP
        IF NOT EXISTS (SELECT 1 FROM bidding.bid_wallets WHERE user_id = rec.user_id) THEN
            INSERT INTO bidding.bid_wallets (tenant_id, user_id, entity_type, entity_id, total_bids, spent_bids, available_bids, created_at, updated_at)
            VALUES (rec.tenant_id, rec.user_id, rec.entity_type, rec.user_id, seed_amount, 0, seed_amount, NOW(), NOW())
            RETURNING id INTO wid;

            INSERT INTO bidding.bid_transactions (tenant_id, user_id, wallet_id, transaction_type, amount, unit_price, total_price, balance_after, reference_type, description, created_at)
            VALUES (rec.tenant_id, rec.user_id, wid, 'ALLOCATION', seed_amount, 0, 0, seed_amount, 'SEED_ALLOCATION', 'Dev seed: 1000 bids allocated', NOW());
        END IF;
    END LOOP;
END $$;
