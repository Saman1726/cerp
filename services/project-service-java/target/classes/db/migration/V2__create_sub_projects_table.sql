-- V2: Create sub_projects table
-- SubProjects represent buildings or major building components within a Project
-- Each SubProject corresponds to a Building on a Construction Site

CREATE TABLE sub_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    tenant_id VARCHAR(255) NOT NULL,
    org_id UUID NOT NULL,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    external_id VARCHAR(128),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    building_type VARCHAR(64),
    construction_phase VARCHAR(64),
    building_level VARCHAR(32) NOT NULL DEFAULT 'WHOLE',
    usable_area NUMERIC(12, 2),
    total_area NUMERIC(12, 2),
    floor_count INTEGER,
    unit_count INTEGER,
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNING',
    start_date DATE,
    end_date DATE,
    progress NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    budget NUMERIC(18, 2),
    currency_code VARCHAR(8) NOT NULL DEFAULT 'EUR',
    sync_status VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(128),
    updated_by VARCHAR(128),
    CONSTRAINT uk_subproject_tenant_project_code UNIQUE (tenant_id, project_id, code)
);

-- Indexes
CREATE INDEX idx_subproject_tenant_id ON sub_projects (tenant_id);

CREATE INDEX idx_subproject_org_id ON sub_projects (org_id);

CREATE INDEX idx_subproject_project_id ON sub_projects (project_id);

CREATE UNIQUE INDEX idx_subproject_external_id ON sub_projects (external_id)
WHERE
    external_id IS NOT NULL;

CREATE INDEX idx_subproject_code ON sub_projects (code);

CREATE INDEX idx_subproject_status ON sub_projects (status);

CREATE INDEX idx_subproject_tenant_project ON sub_projects (tenant_id, project_id);

CREATE INDEX idx_subproject_building_type ON sub_projects (building_type);

CREATE INDEX idx_subproject_construction_phase ON sub_projects (construction_phase);

CREATE INDEX idx_subproject_building_level ON sub_projects (building_level);

-- Comments
COMMENT ON
TABLE sub_projects IS 'Buildings or major building components as sub-projects within a Project, scoped by tenant and org';

COMMENT ON COLUMN sub_projects.building_type IS 'RESIDENTIAL, COMMERCIAL, INDUSTRIAL, MIXED_USE, PUBLIC, PARKING, UTILITY';

COMMENT ON COLUMN sub_projects.construction_phase IS 'PRE_CONSTRUCTION, SITE_PREPARATION, FOUNDATION, BASEMENT, SUPERSTRUCTURE, ROOF, BUILDING_ENVELOPE, INTERIOR_PARTITIONS, MEP_ROUGH_IN, INTERIOR_FINISH, EXTERIOR_WORKS, TESTING_COMMISSIONING, SNAGGING, HANDOVER, FACILITY_MANAGEMENT';

COMMENT ON COLUMN sub_projects.building_level IS 'WHOLE, FLOOR, PART - determines the scope of workflows';

COMMENT ON COLUMN sub_projects.status IS 'PLANNING, IN_CONSTRUCTION, ON_HOLD, COMPLETED, CANCELLED';

COMMENT ON COLUMN sub_projects.sync_status IS 'LOCAL, REMOTE, CONFLICT, PENDING_SYNC';

-- Hibernate @ElementCollection tables for floor_ids and part_ids
-- These are created by the migration to match the @ElementCollection mappings in SubProject.java
CREATE TABLE sub_project_floor_ids (
    sub_project_id UUID NOT NULL REFERENCES sub_projects (id) ON DELETE CASCADE,
    floor_ids VARCHAR(255),
    PRIMARY KEY (sub_project_id, floor_ids)
);

CREATE TABLE sub_project_part_ids (
    sub_project_id UUID NOT NULL REFERENCES sub_projects (id) ON DELETE CASCADE,
    part_ids VARCHAR(255),
    PRIMARY KEY (sub_project_id, part_ids)
);