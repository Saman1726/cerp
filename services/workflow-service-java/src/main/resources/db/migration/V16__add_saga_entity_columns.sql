-- Add explicit saga_id, user_id, and project_id columns to saga_events table
-- This fixes the missing relationships between saga state, events, and business entities

-- Add saga_id for explicit saga correlation
ALTER TABLE saga_events ADD COLUMN saga_id VARCHAR(255);

-- Add user_id for tracking who initiated the saga
ALTER TABLE saga_events ADD COLUMN user_id VARCHAR(255);

-- Add project_id for direct project correlation
ALTER TABLE saga_events ADD COLUMN project_id UUID;

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_saga_events_saga_id ON saga_events (saga_id);

CREATE INDEX IF NOT EXISTS idx_saga_events_user_id ON saga_events (user_id);

CREATE INDEX IF NOT EXISTS idx_saga_events_project_id ON saga_events (project_id);

-- Populate saga_id from id for existing records (id IS the saga_id)
UPDATE saga_events SET saga_id = id::VARCHAR WHERE saga_id IS NULL;