-- Move public schema entities to their proper schemas
-- outbox_events -> workflow schema
-- Note: Bidding entities (bid_wallets, bid_transactions, vendor_bid_submissions, pricing_config)
-- were created directly in the bidding schema by V4, so no schema move is needed for them.

-- Move outbox_events to workflow schema
ALTER TABLE outbox_events SET SCHEMA workflow;

-- Recreate indexes after schema move
DROP INDEX IF EXISTS idx_outbox_events_status_created;
DROP INDEX IF EXISTS idx_outbox_events_saga_id;
DROP INDEX IF EXISTS idx_outbox_events_aggregate;
DROP INDEX IF EXISTS idx_outbox_events_tenant;
CREATE INDEX idx_outbox_events_status_created ON workflow.outbox_events (status, created_at);
CREATE INDEX idx_outbox_events_saga_id ON workflow.outbox_events (saga_id);
CREATE INDEX idx_outbox_events_aggregate ON workflow.outbox_events (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_events_tenant ON workflow.outbox_events (tenant_id);
