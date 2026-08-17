-- Add assignee_email and assignee_name columns to workflow_steps for human-readable
-- assignee display, reducing dependency on the identity-service for lookups at render time.
ALTER TABLE workflow.workflow_steps ADD COLUMN assignee_email VARCHAR(255);
ALTER TABLE workflow.workflow_steps ADD COLUMN assignee_name VARCHAR(255);