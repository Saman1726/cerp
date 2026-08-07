-- V2: Enforce 1:1 ConstructionSite<->Project and Building<->SubProject at the DB level.
--
-- Both relationships were previously application-level only (ProjectService checking
-- findByConstructionSiteIdAndDeletedFalse; SubProjectService had no equivalent check at
-- all), race-prone under concurrent requests.
--
-- If either of these returns rows, resolve manually before this migration can apply -
-- there was never a DB backstop before this, so a race could already have produced
-- duplicates:
--   SELECT construction_site_id, COUNT(*) FROM project.projects
--   WHERE construction_site_id IS NOT NULL AND is_deleted = false
--   GROUP BY construction_site_id HAVING COUNT(*) > 1;
--   SELECT building_id, COUNT(*) FROM project.sub_projects
--   WHERE building_id IS NOT NULL GROUP BY building_id HAVING COUNT(*) > 1;

-- Partial on is_deleted for defense-in-depth: Project.delete() already nulls
-- construction_site_id when releasing the link, so a plain unique index would also work
-- given current code, but this doesn't rely on that always remaining true.
CREATE UNIQUE INDEX idx_projects_construction_site_id_active
    ON project.projects (construction_site_id)
    WHERE construction_site_id IS NOT NULL AND is_deleted = false;

-- SubProject has no soft-delete flag (deleteSubProject() is a hard delete), so no
-- partial-on-deleted predicate is needed. NULL exclusion here is documentation/index-size
-- only - Postgres unique indexes never treat two NULLs as duplicates regardless.
CREATE UNIQUE INDEX idx_subproject_building_id_unique
    ON project.sub_projects (building_id)
    WHERE building_id IS NOT NULL;
