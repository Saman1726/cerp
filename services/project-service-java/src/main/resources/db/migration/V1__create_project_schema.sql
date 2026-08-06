-- V1: Create the project schema and its tables.
--
-- Replaces the old public-schema migration set (V1-V9, V99), which was never actually
-- applied: Flyway was hardcoded disabled for this service (SPRING_FLYWAY_ENABLED=false)
-- and Hibernate's ddl-auto=update failed silently on every startup with "ERROR: no
-- schema has been selected to create in" (no writable default schema for the cerp
-- role), so no project tables ever existed anywhere.
--
-- Table shapes below mirror the current JPA entities in io.cerp.project.domain exactly.

CREATE SCHEMA IF NOT EXISTS project;

-- ============================================================
-- PROJECTS
-- ============================================================
CREATE TABLE project.projects (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            VARCHAR(255) NOT NULL,
    org_id               UUID NOT NULL,
    external_id          VARCHAR(128),
    code                 VARCHAR(64) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    construction_site_id VARCHAR(64),
    property_id          UUID,
    address_line_1       VARCHAR(255),
    address_line_2       VARCHAR(255),
    postal_code          VARCHAR(20),
    city                 VARCHAR(128),
    country_code         VARCHAR(4),
    status               VARCHAR(32) NOT NULL,
    project_type         VARCHAR(64),
    start_date           DATE,
    end_date             DATE,
    actual_start_date    DATE,
    actual_end_date      DATE,
    progress             NUMERIC(5, 2),
    budget               NUMERIC(18, 2),
    currency_code        VARCHAR(8),
    client_name          VARCHAR(255),
    client_email         VARCHAR(255),
    client_phone         VARCHAR(32),
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    sync_status          VARCHAR(32) NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(128),
    updated_by           VARCHAR(128),
    assigned_user_ids    TEXT[] NOT NULL DEFAULT '{}',
    assigned_user_roles  JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX idx_projects_tenant_id ON project.projects (tenant_id);
CREATE INDEX idx_projects_org_id ON project.projects (org_id);
CREATE UNIQUE INDEX idx_projects_external_id ON project.projects (external_id);
CREATE INDEX idx_projects_code ON project.projects (code);
CREATE INDEX idx_projects_status ON project.projects (status);
CREATE INDEX idx_projects_tenant_org ON project.projects (tenant_id, org_id);

-- ============================================================
-- CONSTRUCTION SITES
-- ============================================================
CREATE TABLE project.construction_sites (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  VARCHAR(255) NOT NULL,
    org_id     UUID,
    name       VARCHAR(255) NOT NULL,
    address    VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    updated_by VARCHAR(128)
);
CREATE INDEX idx_construction_sites_tenant_id ON project.construction_sites (tenant_id);
CREATE INDEX idx_construction_sites_org_id ON project.construction_sites (org_id);

-- ============================================================
-- BUILDINGS (belong to a construction site)
-- ============================================================
CREATE TABLE project.buildings (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255) NOT NULL,
    construction_site_id UUID NOT NULL REFERENCES project.construction_sites (id) ON DELETE CASCADE,
    usable_area          NUMERIC(12, 2),
    total_area           NUMERIC(12, 2),
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_buildings_construction_site_id ON project.buildings (construction_site_id);

-- ============================================================
-- FLOORS (belong to a building)
-- ============================================================
CREATE TABLE project.floors (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type        VARCHAR(128) NOT NULL,
    usable_area NUMERIC(12, 2),
    total_area  NUMERIC(12, 2),
    building_id UUID NOT NULL REFERENCES project.buildings (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_floors_building_id ON project.floors (building_id);

-- ============================================================
-- UNITS (belong to a floor)
-- ============================================================
CREATE TABLE project.units (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label       VARCHAR(128) NOT NULL,
    usable_area NUMERIC(12, 2),
    bedrooms    INTEGER,
    bathrooms   INTEGER,
    balconies   INTEGER,
    toilets     INTEGER,
    floor_id    UUID NOT NULL REFERENCES project.floors (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_units_floor_id ON project.units (floor_id);

-- ============================================================
-- SUB PROJECTS (buildings modeled as sub-projects of a Project;
-- references both projects and buildings)
-- ============================================================
CREATE TABLE project.sub_projects (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          VARCHAR(255) NOT NULL,
    org_id             UUID NOT NULL,
    project_id         UUID NOT NULL REFERENCES project.projects (id),
    external_id        VARCHAR(128),
    code               VARCHAR(64) NOT NULL,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    building_type      VARCHAR(64),
    construction_phase VARCHAR(64),
    building_level     VARCHAR(32) NOT NULL DEFAULT 'WHOLE',
    floor_ids          TEXT[] NOT NULL DEFAULT '{}',
    part_ids           TEXT[] NOT NULL DEFAULT '{}',
    usable_area        NUMERIC(12, 2),
    total_area         NUMERIC(12, 2),
    floor_count        INTEGER,
    unit_count         INTEGER,
    status             VARCHAR(32) NOT NULL,
    start_date         DATE,
    end_date           DATE,
    progress           NUMERIC(5, 2),
    budget             NUMERIC(18, 2),
    currency_code      VARCHAR(8),
    building_id        UUID REFERENCES project.buildings (id),
    sync_status        VARCHAR(32) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(128),
    updated_by         VARCHAR(128)
);
CREATE INDEX idx_subproject_tenant_id ON project.sub_projects (tenant_id);
CREATE INDEX idx_subproject_org_id ON project.sub_projects (org_id);
CREATE INDEX idx_subproject_project_id ON project.sub_projects (project_id);
CREATE UNIQUE INDEX idx_subproject_external_id ON project.sub_projects (external_id);
CREATE INDEX idx_subproject_code ON project.sub_projects (code);
CREATE INDEX idx_subproject_status ON project.sub_projects (status);
CREATE INDEX idx_subproject_tenant_project ON project.sub_projects (tenant_id, project_id);
