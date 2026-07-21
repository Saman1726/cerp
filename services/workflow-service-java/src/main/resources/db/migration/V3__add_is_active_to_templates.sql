-- V3: Add is_active compatibility column to workflow_templates
-- This column provides backward compatibility for queries referencing is_active
-- The actual status is stored in the 'status' column (DRAFT, ACTIVE, DEPRECATED)

ALTER TABLE workflow_templates
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;

-- Create a trigger to keep is_active in sync with status
CREATE OR REPLACE FUNCTION update_template_is_active()
RETURNS TRIGGER AS $$
BEGIN
    NEW.is_active := (NEW.status = 'ACTIVE');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_template_is_active ON workflow_templates;

CREATE TRIGGER trg_update_template_is_active
    BEFORE UPDATE OF status ON workflow_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_template_is_active();

-- Backfill is_active from existing status values
UPDATE workflow_templates SET is_active = (status = 'ACTIVE');

-- Create view for backward compatibility with 'workflow' (singular) queries
CREATE OR REPLACE VIEW workflow AS SELECT * FROM workflows;