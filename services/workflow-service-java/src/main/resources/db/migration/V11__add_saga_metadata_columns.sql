-- Add saga metadata columns required by SagaEntity but missing from V1.
-- These columns support "Created by Me" view, "Assigned to Me" view, and
-- vendor/procurement shared access (party_ids as JSON array).
ALTER TABLE workflow.saga_events ADD COLUMN created_by   VARCHAR(255);
ALTER TABLE workflow.saga_events ADD COLUMN assigned_to  VARCHAR(255);
ALTER TABLE workflow.saga_events ADD COLUMN party_ids    TEXT;