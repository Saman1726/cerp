-- V5: Add construction_site_id and is_deleted to projects table
-- Each construction site can only have one active project
-- is_deleted flag allows soft-delete without freeing construction site link immediately

ALTER TABLE projects ADD COLUMN construction_site_id VARCHAR(64);

ALTER TABLE projects
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;

-- Unique index: only one active (non-deleted) project per construction site
CREATE UNIQUE INDEX idx_projects_unique_construction_site ON projects (construction_site_id)
WHERE
    construction_site_id IS NOT NULL
    AND is_deleted = false;

-- Index for performance
CREATE INDEX idx_projects_construction_site ON projects (construction_site_id);

COMMENT ON COLUMN projects.construction_site_id IS 'Reference to construction site (String ID from core-api). Each construction site can only have one active project.';

COMMENT ON COLUMN projects.is_deleted IS 'Soft-delete flag. When true, project is archived and construction_site_id can be released.';