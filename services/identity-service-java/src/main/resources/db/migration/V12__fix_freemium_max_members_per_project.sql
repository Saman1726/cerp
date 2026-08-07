-- V12: Correct FREEMIUM's max_members_per_project.
--
-- V10 set this to NULL, which under this table's own documented convention ("NULL means
-- no cap") contradicts the original Freemium rule "exactly 1 owner + 1 manager" (i.e.
-- zero of anything else). project-service's OWNER/MANAGER/SUPERVISOR/VIEWER vocabulary
-- maps SUPERVISOR+VIEWER assignees onto max_members_per_project, so this must be 0 for
-- FREEMIUM, not unlimited. Forward-only correction - V10 already shipped.
UPDATE identity.plan_limits
SET max_members_per_project = 0, updated_at = NOW()
WHERE plan = 'FREEMIUM';
