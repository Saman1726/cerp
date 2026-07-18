-- V7: Add building_id to sub_projects table
-- Link SubProjects to Buildings on Construction Sites

ALTER TABLE sub_projects ADD COLUMN building_id UUID;

ALTER TABLE sub_projects
ADD CONSTRAINT fk_subproject_building FOREIGN KEY (building_id) REFERENCES buildings (id) ON DELETE SET NULL;

CREATE INDEX idx_subproject_building_id ON sub_projects (building_id);

COMMENT ON COLUMN sub_projects.building_id IS 'Reference to the Building on a Construction Site';