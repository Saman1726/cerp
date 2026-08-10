-- Adds a soft-delete/archive flag to workflows and saga_events, used when a Project (in
-- project-service) is deleted. This purely hides matching rows from default listings - it does
-- NOT touch status, and does NOT touch bidding.bid_wallets/bid_transactions (those are per-vendor
-- financial ledgers shared across every project a vendor has ever bid on, not project-scoped -
-- they are deliberately left untouched by this cascade).
ALTER TABLE workflow.workflows ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE workflow.saga_events ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_workflows_is_deleted ON workflow.workflows (is_deleted);
