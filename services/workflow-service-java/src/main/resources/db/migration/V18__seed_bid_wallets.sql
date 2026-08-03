-- Seed bid wallets with 1000 bids for dev/test users.
-- user_id is UNIQUE per wallet, so each user gets one wallet matching their role.

DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
    seed_amount    BIGINT := 1000;
    wid            UUID;
BEGIN
    -- User 1 (admin / project owner)
    IF NOT EXISTS (SELECT 1 FROM bidding.bid_wallets WHERE user_id = '00000000-0000-0000-0000-000000000001') THEN
        INSERT INTO bidding.bid_wallets (tenant_id, user_id, entity_type, entity_id, total_bids, spent_bids, available_bids, created_at, updated_at)
        VALUES (default_tenant, '00000000-0000-0000-0000-000000000001', 'PROJECT_OWNER', '00000000-0000-0000-0000-000000000001', seed_amount, 0, seed_amount, NOW(), NOW())
        RETURNING id INTO wid;

        INSERT INTO bidding.bid_transactions (tenant_id, user_id, wallet_id, transaction_type, amount, unit_price, total_price, balance_after, reference_type, description, created_at)
        VALUES (default_tenant, '00000000-0000-0000-0000-000000000001', wid, 'ALLOCATION', seed_amount, 0, 0, seed_amount, 'SEED_ALLOCATION', 'Dev seed: 1000 bids allocated', NOW());
    END IF;

    -- User 2 (vendor)
    IF NOT EXISTS (SELECT 1 FROM bidding.bid_wallets WHERE user_id = '00000000-0000-0000-0000-000000000002') THEN
        INSERT INTO bidding.bid_wallets (tenant_id, user_id, entity_type, entity_id, total_bids, spent_bids, available_bids, created_at, updated_at)
        VALUES (default_tenant, '00000000-0000-0000-0000-000000000002', 'VENDOR', '00000000-0000-0000-0000-000000000002', seed_amount, 0, seed_amount, NOW(), NOW())
        RETURNING id INTO wid;

        INSERT INTO bidding.bid_transactions (tenant_id, user_id, wallet_id, transaction_type, amount, unit_price, total_price, balance_after, reference_type, description, created_at)
        VALUES (default_tenant, '00000000-0000-0000-0000-000000000002', wid, 'ALLOCATION', seed_amount, 0, 0, seed_amount, 'SEED_ALLOCATION', 'Dev seed: 1000 bids allocated', NOW());
    END IF;

    -- User 3 (vendor)
    IF NOT EXISTS (SELECT 1 FROM bidding.bid_wallets WHERE user_id = '00000000-0000-0000-0000-000000000003') THEN
        INSERT INTO bidding.bid_wallets (tenant_id, user_id, entity_type, entity_id, total_bids, spent_bids, available_bids, created_at, updated_at)
        VALUES (default_tenant, '00000000-0000-0000-0000-000000000003', 'VENDOR', '00000000-0000-0000-0000-000000000003', seed_amount, 0, seed_amount, NOW(), NOW())
        RETURNING id INTO wid;

        INSERT INTO bidding.bid_transactions (tenant_id, user_id, wallet_id, transaction_type, amount, unit_price, total_price, balance_after, reference_type, description, created_at)
        VALUES (default_tenant, '00000000-0000-0000-0000-000000000003', wid, 'ALLOCATION', seed_amount, 0, 0, seed_amount, 'SEED_ALLOCATION', 'Dev seed: 1000 bids allocated', NOW());
    END IF;
END $$;
