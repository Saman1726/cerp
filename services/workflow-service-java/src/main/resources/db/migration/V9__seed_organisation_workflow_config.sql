-- Seed organisation workflow config for the default organization
-- Enable auto-generation of workflows on subproject creation

INSERT INTO
    organisation_workflow_config (
        tenant_id,
        org_id,
        auto_generate_on_subproject_creation,
        selected_template_codes,
        custom_phase_mappings,
        created_at,
        updated_at
    )
VALUES (
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        true,
        '[]',
        '{}',
        NOW(),
        NOW()
    ) ON CONFLICT (tenant_id, org_id) DO
UPDATE
SET
    auto_generate_on_subproject_creation = EXCLUDED.auto_generate_on_subproject_creation,
    updated_at = NOW();