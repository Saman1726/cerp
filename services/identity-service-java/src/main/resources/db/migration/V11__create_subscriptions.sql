-- V11: Per-user Subscriptions.
--
-- Every user account - regardless of Type (User/Vendor/Company) - has exactly one
-- Subscription, keyed by user_id (NOT tenant_id): a Company's plan does not cover every
-- user under its tenant as a shared entitlement; the Company account itself carries its
-- own Subscription like any other account.

CREATE TABLE identity.subscriptions (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL UNIQUE REFERENCES identity.user_accounts (id) ON DELETE CASCADE,
    plan       TEXT        NOT NULL REFERENCES identity.plan_limits (plan),
    status     TEXT        NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_subscriptions_user_id ON identity.subscriptions (user_id);

-- Backfill: every pre-existing user_account gets a Freemium/active Subscription so the
-- "every account has exactly one Subscription" invariant holds immediately in dev, not
-- just for accounts created after this migration ships. (Accounts created by
-- SeedDataConfig's CommandLineRunner - metal@cerp.dev, john@cerp.dev - run after Flyway
-- and are NOT covered by this backfill; see SeedDataConfig.java for their own explicit
-- Subscription creation.)
INSERT INTO identity.subscriptions (user_id, plan, status)
SELECT ua.id, 'FREEMIUM', 'active'
FROM identity.user_accounts ua
WHERE NOT EXISTS (SELECT 1 FROM identity.subscriptions s WHERE s.user_id = ua.id);
