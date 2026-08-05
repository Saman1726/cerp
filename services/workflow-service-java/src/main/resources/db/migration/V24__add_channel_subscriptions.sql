-- Channel Subscriptions: vendors opt-in to receive push notifications for a channel
-- Schema: bidding

CREATE TABLE IF NOT EXISTS bidding.channel_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    channel_code TEXT NOT NULL,
    subscribed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_channel_subscription UNIQUE (tenant_id, user_id, channel_code)
);

CREATE INDEX IF NOT EXISTS idx_channel_subscriptions_lookup
    ON bidding.channel_subscriptions (tenant_id, channel_code);
