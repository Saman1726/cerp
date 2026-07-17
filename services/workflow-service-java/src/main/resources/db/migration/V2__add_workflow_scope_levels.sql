-- V2: Add workflow scope level support for SubProjects
-- Adds applicable_scope_levels and default_scope_level to workflow_templates
-- Adds floor_ids and part_ids to workflows for building-level scoping

-- Add scope level columns to workflow_templates
ALTER TABLE workflow_templates
ADD COLUMN applicable_scope_levels TEXT[];

ALTER TABLE workflow_templates
ADD COLUMN default_scope_level VARCHAR(16) NOT NULL DEFAULT 'WHOLE';

-- Add floor and part ID columns to workflows
ALTER TABLE workflows ADD COLUMN floor_ids TEXT[];

ALTER TABLE workflows ADD COLUMN part_ids TEXT[];

-- Add scope level column to workflows to indicate the execution scope
ALTER TABLE workflows
ADD COLUMN scope_level VARCHAR(16) NOT NULL DEFAULT 'WHOLE';

-- Add indexes for new columns
CREATE INDEX idx_templates_scope_level ON workflow_templates (default_scope_level);

CREATE INDEX idx_workflows_scope_level ON workflows (scope_level);

CREATE INDEX idx_workflows_floor_ids ON workflows USING (gin floor_ids);

CREATE INDEX idx_workflows_part_ids ON workflows USING (gin part_ids);

-- Comments
COMMENT ON COLUMN workflow_templates.applicable_scope_levels IS 'WHOLE, FLOOR, PART - which scopes this template applies to';

COMMENT ON COLUMN workflow_templates.default_scope_level IS 'WHOLE, FLOOR, PART - default scope when instantiating';

COMMENT ON COLUMN workflows.floor_ids IS 'Floor identifiers for FLOOR scope level workflows';

COMMENT ON COLUMN workflows.part_ids IS 'Building part identifiers for PART scope level workflows';

COMMENT ON COLUMN workflows.scope_level IS 'WHOLE, FLOOR, PART - the execution scope of this workflow instance';