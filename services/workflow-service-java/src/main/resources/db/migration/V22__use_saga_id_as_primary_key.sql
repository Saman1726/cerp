-- Use saga_id as primary key for saga_events table
-- This fixes the duplicate key insert issue where toEntity() created new entities
-- with generated UUIDs, causing JPA to INSERT instead of UPDATE when saga_id already exists.
-- By making id (saga_id) the primary key, JPA will naturally UPDATE existing records.

-- Drop the unique constraint on the old saga_id column (will be renamed to id)
ALTER TABLE workflow.saga_events DROP CONSTRAINT IF EXISTS uq_saga_id;

-- Drop the old id column and its dependencies (indexes, constraints)
ALTER TABLE workflow.saga_events DROP COLUMN IF EXISTS id;

-- Rename saga_id to id and make it the primary key
ALTER TABLE workflow.saga_events RENAME COLUMN saga_id TO id;
ALTER TABLE workflow.saga_events ADD CONSTRAINT saga_events_pkey PRIMARY KEY (id);

-- Convert project_id from UUID to VARCHAR to match SagaEntity.java (projectId is String)
ALTER TABLE workflow.saga_events ALTER COLUMN project_id TYPE VARCHAR(255) USING project_id::VARCHAR;

-- Recreate the index on the new id column
DROP INDEX IF EXISTS idx_saga_events_saga_id;
DROP INDEX IF EXISTS idx_saga_events_id;
CREATE INDEX idx_saga_events_id ON workflow.saga_events (id);
