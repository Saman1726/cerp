-- V6: Create indexes and dedup constraints on construction_materials
CREATE INDEX IF NOT EXISTS idx_cm_site_id
    ON project.construction_materials (construction_site_id);

CREATE INDEX IF NOT EXISTS idx_cm_physical_obj
    ON project.construction_materials (physical_object_type, physical_object_id);

CREATE INDEX IF NOT EXISTS idx_cm_service_code
    ON project.construction_materials (material_service_code);