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
-- just for accounts created after this migration ships. This now also covers
-- metal@cerp.dev/john@cerp.dev, since V7 seeds them with a fixed id ahead of this
-- migration; SeedDataConfig's own Subscription creation for them (CommandLineRunners
-- run after Flyway) is a no-op in practice, same as its user-creation code.
INSERT INTO identity.subscriptions (user_id, plan, status)
SELECT ua.id, 'FREEMIUM', 'active'
FROM identity.user_accounts ua
WHERE NOT EXISTS (SELECT 1 FROM identity.subscriptions s WHERE s.user_id = ua.id);
