-- Organisation Workflow Configuration: per-org settings for workflow generation
-- Controls auto-generation behavior and allows custom template selection/overrides.

CREATE TABLE organisation_workflow_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    tenant_id VARCHAR(255) NOT NULL,
    org_id UUID NOT NULL,
    auto_generate_on_subproject_creation BOOLEAN NOT NULL DEFAULT false,
    selected_template_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    custom_phase_mappings JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_organisation_workflow_config UNIQUE (tenant_id, org_id)
);

CREATE INDEX idx_organisation_workflow_config_tenant ON organisation_workflow_config (tenant_id);

CREATE INDEX idx_organisation_workflow_config_org ON organisation_workflow_config (org_id);