-- Bidding schema: bid wallets, transactions, vendor submissions, channel subscriptions.

CREATE SCHEMA IF NOT EXISTS bidding;

-- Pricing configuration
CREATE TABLE bidding.pricing_config (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    key         TEXT          NOT NULL UNIQUE,
    value       NUMERIC(10,2) NOT NULL,
    description TEXT,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

INSERT INTO bidding.pricing_config (key, value, description) VALUES
    ('BID_UNIT_PRICE_EUR',             10.00, 'Price per bid in EUR'),
    ('BIDDING_CREATION_COST',           1.00, 'Number of bids required to create a bidding'),
    ('OPEN_TENDER_PARTICIPATION_COST',  5.00, 'Number of bids vendors pay to participate in open tender')
ON CONFLICT (key) DO NOTHING;

-- Bid wallets (one per user)
CREATE TABLE bidding.bid_wallets (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL,
    user_id        UUID         NOT NULL UNIQUE,
    entity_type    TEXT         NOT NULL CHECK (entity_type IN ('PROJECT_OWNER','VENDOR')),
    entity_id      UUID         NOT NULL,
    total_bids     BIGINT       NOT NULL DEFAULT 0,
    spent_bids     BIGINT       NOT NULL DEFAULT 0,
    available_bids BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ
);
CREATE INDEX idx_bid_wallets_user_id   ON bidding.bid_wallets (user_id);
CREATE INDEX idx_bid_wallets_entity    ON bidding.bid_wallets (entity_type, entity_id);
CREATE INDEX idx_bid_wallets_tenant_id ON bidding.bid_wallets (tenant_id);

-- Bid transactions (audit trail)
CREATE TABLE bidding.bid_transactions (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID          NOT NULL,
    user_id          UUID          NOT NULL,
    wallet_id        UUID          NOT NULL REFERENCES bidding.bid_wallets (id),
    bidding_saga_id  UUID,
    tender_id        TEXT,
    transaction_type TEXT          NOT NULL CHECK (transaction_type IN ('ALLOCATION','SPEND','REFUND')),
    amount           BIGINT        NOT NULL,
    unit_price       NUMERIC(10,2),
    total_price      NUMERIC(10,2),
    balance_after    BIGINT        NOT NULL DEFAULT 0,
    reference_type   TEXT,
    reference_id     TEXT,
    description      TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bid_transactions_wallet_id  ON bidding.bid_transactions (wallet_id);
CREATE INDEX idx_bid_transactions_saga_id    ON bidding.bid_transactions (bidding_saga_id);
CREATE INDEX idx_bid_transactions_user_id    ON bidding.bid_transactions (user_id);
CREATE INDEX idx_bid_transactions_tender_id  ON bidding.bid_transactions (tender_id);
CREATE INDEX idx_bid_transactions_type       ON bidding.bid_transactions (transaction_type);

-- Vendor bid submissions
CREATE TABLE bidding.vendor_bid_submissions (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_id        TEXT          NOT NULL,
    bidding_saga_id  UUID          NOT NULL,
    vendor_id        UUID          NOT NULL,
    vendor_user_id   UUID,
    tenant_id        UUID          NOT NULL,
    price            NUMERIC(15,2) NOT NULL,
    quantity         NUMERIC(15,2) NOT NULL DEFAULT 1,
    unit_price       NUMERIC(15,2) NOT NULL,
    total_price      NUMERIC(15,2) NOT NULL,
    timeline         TEXT,
    notes            TEXT,
    status           TEXT          NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','AWARDED','REJECTED')),
    payment_method   VARCHAR(64),
    awarded_at       TIMESTAMPTZ,
    purchase_order_id TEXT,
    submitted_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ
);
CREATE INDEX idx_vendor_submissions_saga_id   ON bidding.vendor_bid_submissions (bidding_saga_id);
CREATE INDEX idx_vendor_submissions_tender_id ON bidding.vendor_bid_submissions (tender_id);
CREATE INDEX idx_vendor_submissions_vendor    ON bidding.vendor_bid_submissions (vendor_id);
CREATE INDEX idx_vendor_submissions_status    ON bidding.vendor_bid_submissions (status);

-- Channel subscriptions (vendor push-notification opt-in)
CREATE TABLE bidding.channel_subscriptions (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL,
    user_id       UUID        NOT NULL,
    channel_code  TEXT        NOT NULL,
    subscribed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_channel_subscription UNIQUE (tenant_id, user_id, channel_code)
);
CREATE INDEX idx_channel_subscriptions_lookup ON bidding.channel_subscriptions (tenant_id, channel_code);
