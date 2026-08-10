-- SubProjects were only ever hard-deleted (or, via the project cascade, not touched at all).
-- Add the same is_deleted soft-delete flag projects.is_deleted already uses, so deleting a
-- SubProject (directly, or via a Project deletion cascade) archives it instead of destroying
-- construction history (evidence, approvals, workflow links) tied to it.
ALTER TABLE project.sub_projects ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_subproject_is_deleted ON project.sub_projects (is_deleted);
