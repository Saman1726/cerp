-- V5: Create construction_materials table for BIM-parsed materials
CREATE TABLE IF NOT EXISTS project.construction_materials (
    id                         UUID NOT NULL PRIMARY KEY,
    construction_site_id       UUID       NOT NULL
                                 REFERENCES project.construction_sites(id),
    physical_object_type       VARCHAR(20),   -- BUILDING | FLOOR | UNIT (NULL = site-level)
    physical_object_id         UUID,
    material_service_code      VARCHAR(100),  -- catalog.service_code (no cross-schema FK)
    material_service_name      VARCHAR(255),
    ifc_material_name          VARCHAR(500),  -- fallback for unmapped IFC materials
    quantity                   NUMERIC(12, 2),
    unit                       VARCHAR(20),
    created_at                 TIMESTAMP NOT NULL,
    updated_at                 TIMESTAMP NOT NULL
);