-- V4: Fix sub_projects.building_level to allow NULL and ensure default
ALTER TABLE sub_projects ALTER COLUMN building_level DROP NOT NULL;
ALTER TABLE sub_projects ALTER COLUMN building_level SET DEFAULT 'WHOLE';
