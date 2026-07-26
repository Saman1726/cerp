-- Add assigned_user_roles JSONB column to projects table for role-based project assignment
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS assigned_user_roles JSONB;

-- Add index for faster lookups when querying user roles
CREATE INDEX IF NOT EXISTS idx_projects_assigned_user_roles ON projects USING GIN (assigned_user_roles);