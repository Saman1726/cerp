-- V11: Create normalized workflow_template_step_assignee_roles table
-- This migration replaces the simple join table from V2 with a fully normalized entity

-- Drop the V2 join table if it exists (it has a different schema)
DROP TABLE IF EXISTS workflow_template_step_assignee_roles CASCADE;

-- Create the normalized assignee roles table
CREATE TABLE workflow_template_step_assignee_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    template_step_id UUID NOT NULL REFERENCES workflow_template_steps (id) ON DELETE CASCADE,
    role_name VARCHAR(64) NOT NULL,
    role_order INTEGER NOT NULL DEFAULT 1,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_step_assignee_roles_step ON workflow_template_step_assignee_roles (template_step_id);

CREATE INDEX idx_step_assignee_roles_role ON workflow_template_step_assignee_roles (role_name);

-- Populate from existing assignee_roles array in workflow_template_steps
INSERT INTO workflow_template_step_assignee_roles (template_step_id, role_name, role_order, is_primary)
SELECT
    id AS template_step_id,
    role AS role_name,
    (array_position(assignee_roles, role))::int AS role_order,
    (array_position(assignee_roles, role))::int = 1 AS is_primary
FROM workflow_template_steps,
     unnest(assignee_roles) AS role
WHERE assignee_roles IS NOT NULL AND array_length(assignee_roles, 1) > 0;

-- Create a view for backward compatibility
CREATE OR REPLACE VIEW workflow_template_steps_with_roles AS
SELECT
    sts.id,
    sts.template_id,
    sts.step_order,
    sts.code,
    sts.name,
    sts.step_type,
    sts.requires_evidence,
    sts.requires_approval,
    sts.due_offset_days,
    sts.sla_hours,
    sts.weight,
    sts.checklists,
    sts.gating_condition,
    ARRAY_AGG (
        wtsar.role_name
        ORDER BY wtsar.role_order
    ) AS assignee_roles
FROM
    workflow_template_steps sts
    LEFT JOIN workflow_template_step_assignee_roles wtsar ON sts.id = wtsar.template_step_id
GROUP BY
    sts.id;