-- V2: Add workflow scope level support for SubProjects
-- Adds applicable_scope_levels and default_scope_level to workflow_templates
-- Adds floor_ids and part_ids to workflows for building-level scoping

-- JPA @ElementCollection creates separate join tables for applicableScopeLevels and requiredDocuments
CREATE TABLE workflow_template_applicable_scope_levels (
    workflow_template_id UUID NOT NULL REFERENCES workflow_templates (id) ON DELETE CASCADE,
    applicable_scope_levels VARCHAR(16) NOT NULL,
    PRIMARY KEY (
        workflow_template_id,
        applicable_scope_levels
    )
);

CREATE INDEX idx_template_scope_levels_template ON workflow_template_applicable_scope_levels (workflow_template_id);

CREATE TABLE workflow_template_required_documents (
    workflow_template_id UUID NOT NULL REFERENCES workflow_templates (id) ON DELETE CASCADE,
    required_documents VARCHAR(64) NOT NULL,
    PRIMARY KEY (
        workflow_template_id,
        required_documents
    )
);

CREATE INDEX idx_template_required_docs_template ON workflow_template_required_documents (workflow_template_id);

-- JPA @ElementCollection for WorkflowTemplateStep.assigneeRoles
-- Hibernate default naming: workflow_template_step_assignee_roles(workflow_template_step_id)
CREATE TABLE workflow_template_step_assignee_roles (
    workflow_template_step_id UUID NOT NULL REFERENCES workflow_template_steps (id) ON DELETE CASCADE,
    assignee_roles VARCHAR(64) NOT NULL,
    PRIMARY KEY (
        workflow_template_step_id,
        assignee_roles
    )
);

CREATE INDEX idx_step_assignee_roles_step ON workflow_template_step_assignee_roles (workflow_template_step_id);

-- Add default_scope_level column to workflow_templates (applicable_scope_levels is a separate table via @ElementCollection)
ALTER TABLE workflow_templates
ADD COLUMN default_scope_level VARCHAR(16) NOT NULL DEFAULT 'WHOLE';

-- Add scope level column to workflows to indicate the execution scope
ALTER TABLE workflows
ADD COLUMN scope_level VARCHAR(16) NOT NULL DEFAULT 'WHOLE';

-- Add indexes for new columns
CREATE INDEX idx_templates_scope_level ON workflow_templates (default_scope_level);

CREATE INDEX idx_workflows_scope_level ON workflows (scope_level);

-- Comments
COMMENT ON COLUMN workflow_templates.default_scope_level IS 'WHOLE, FLOOR, PART - default scope when instantiating';

COMMENT ON COLUMN workflows.scope_level IS 'WHOLE, FLOOR, PART - the execution scope of this workflow instance';

-- Note: applicable_scope_levels, required_documents are stored in separate join tables via JPA @ElementCollection
-- Note: floor_ids and part_ids are stored in separate join tables (workflow_floor_ids, workflow_part_ids)
-- via JPA @ElementCollection, not as columns in the workflows table.