-- Move saga_events table from public schema to workflow schema
-- This ensures all saga-related tables are co-located with other workflow tables

ALTER TABLE saga_events SET SCHEMA workflow;

-- Recreate index for saga_id after schema move
DROP INDEX IF EXISTS idx_saga_events_saga_id;
CREATE INDEX idx_saga_events_saga_id ON workflow.saga_events (saga_id);