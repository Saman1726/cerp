-- V3: Track each Project's permanent primary Owner.
--
-- Set once at creation from the authenticated caller (see ProjectService.create()) and
-- never reassignable thereafter - distinct from any additional OWNER-role users granted
-- via assigned_user_roles, which can be added/removed freely.
ALTER TABLE project.projects ADD COLUMN primary_owner_id UUID;

COMMENT ON COLUMN project.projects.primary_owner_id IS
    'The user who created this Project. Immutable after creation - the permanent primary Owner, distinct from any additional OWNER-role users in assigned_user_roles.';
