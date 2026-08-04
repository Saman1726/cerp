-- Add unique constraint to saga_id column in saga_events table
ALTER TABLE saga_events ADD CONSTRAINT uq_saga_id UNIQUE (saga_id);