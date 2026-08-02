-- Add data_json column to saga_events table for storing saga update data
-- This column is used by the updateSaga endpoint to persist saga data changes

ALTER TABLE saga_events ADD COLUMN data_json TEXT;

-- Add index for querying by saga data if needed
CREATE INDEX IF NOT EXISTS idx_saga_events_data_json ON saga_events (data_json);