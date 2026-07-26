-- Add assigned user IDs array to projects table for vendor role-based project visibility
ALTER TABLE projects ADD COLUMN IF NOT EXISTS assigned_user_ids TEXT[];

-- Add index for faster lookups when filtering projects by assigned user
CREATE INDEX IF NOT EXISTS idx_projects_assigned_user_ids ON projects USING GIN (assigned_user_ids);