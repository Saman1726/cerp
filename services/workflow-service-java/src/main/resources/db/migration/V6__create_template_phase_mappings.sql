-- Template Phase Mappings: maps workflow templates to construction phases by building type group
-- This allows per-tenant, per-org configuration of which templates apply to which phases.

CREATE TABLE template_phase_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    tenant_id VARCHAR(255) NOT NULL,
    phase_type VARCHAR(64) NOT NULL,
    building_type_group VARCHAR(32) NOT NULL,
    workflow_template_code VARCHAR(64) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_template_phase_mapping UNIQUE (
        tenant_id,
        phase_type,
        building_type_group,
        workflow_template_code
    ),
    CONSTRAINT chk_building_type_group CHECK (
        building_type_group IN (
            'RESIDENTIAL_TYPE',
            'PUBLIC_TYPE'
        )
    )
);

CREATE INDEX idx_template_phase_mappings_tenant ON template_phase_mappings (tenant_id);

CREATE INDEX idx_template_phase_mappings_phase ON template_phase_mappings (phase_type);

CREATE INDEX idx_template_phase_mappings_template ON template_phase_mappings (workflow_template_code);