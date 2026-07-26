-- V9: Seed sample projects for all tenants to ensure projects are visible after login
-- This migration creates sample projects for development and testing purposes.

-- Seed projects for "default" tenant (using seeded org ID: 00000000-0000-0000-0000-000000000001)
INSERT INTO projects (
    id,
    tenant_id,
    org_id,
    external_id,
    code,
    name,
    description,
    status,
    project_type,
    progress,
    currency_code,
    sync_status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    assigned_user_ids
)
SELECT
    '00000000-0000-0000-0000-000000000201'::uuid,
    'default',
    '00000000-0000-0000-0000-000000000001'::uuid,
    'PRJ-WEB-001',
    'PRJ-WEB-001',
    'Downtown Office Complex',
    'Construction of a modern office building in the city center with 5 floors and underground parking.',
    'ACTIVE',
    'COMMERCIAL',
    35.00,
    'EUR',
    'LOCAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system',
    ARRAY['admin@cerp.dev', 'john@cerp.dev', 'metal@cerp.dev']
WHERE
    NOT EXISTS (SELECT 1 FROM projects WHERE external_id = 'PRJ-WEB-001');

INSERT INTO projects (
    id,
    tenant_id,
    org_id,
    external_id,
    code,
    name,
    description,
    status,
    project_type,
    progress,
    currency_code,
    sync_status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    assigned_user_ids
)
SELECT
    '00000000-0000-0000-0000-000000000202'::uuid,
    'default',
    '00000000-0000-0000-0000-000000000001'::uuid,
    'PRJ-RES-002',
    'PRJ-RES-002',
    'Green Valley Residential',
    'A residential development project featuring 120 luxury apartments with modern amenities.',
    'PLANNED',
    'RESIDENTIAL',
    5.00,
    'EUR',
    'LOCAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system',
    ARRAY['admin@cerp.dev', 'john@cerp.dev', 'metal@cerp.dev']
WHERE
    NOT EXISTS (SELECT 1 FROM projects WHERE external_id = 'PRJ-RES-002');

-- Seed project for MTRD tenant
INSERT INTO projects (
    id,
    tenant_id,
    org_id,
    external_id,
    code,
    name,
    description,
    status,
    project_type,
    progress,
    currency_code,
    sync_status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    assigned_user_ids
)
SELECT
    '00000000-0000-0000-0000-000000000203'::uuid,
    'mtrd',
    '00000000-0000-0000-0000-000000000001'::uuid,
    'PRJ-MTR-003',
    'PRJ-MTR-003',
    'Metal Warehouse Expansion',
    'Expansion of the existing warehouse facility for Metal Trading including new loading docks.',
    'ACTIVE',
    'INDUSTRIAL',
    60.00,
    'EUR',
    'LOCAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system',
    ARRAY['admin@cerp.dev', 'john@cerp.dev', 'metal@cerp.dev']
WHERE
    NOT EXISTS (SELECT 1 FROM projects WHERE external_id = 'PRJ-MTR-003');

-- Seed project for John's Workspace tenant
INSERT INTO projects (
    id,
    tenant_id,
    org_id,
    external_id,
    code,
    name,
    description,
    status,
    project_type,
    progress,
    currency_code,
    sync_status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    assigned_user_ids
)
SELECT
    '00000000-0000-0000-0000-000000000204'::uuid,
    'john',
    '00000000-0000-0000-0000-000000000001'::uuid,
    'PRJ-JHN-004',
    'PRJ-JHN-004',
    'John''s Renovation Project',
    'Complete renovation of a vintage apartment including plumbing, electrical, and interior finishing.',
    'PLANNED',
    'RENOVATION',
    0.00,
    'EUR',
    'LOCAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system',
    ARRAY['admin@cerp.dev', 'john@cerp.dev', 'metal@cerp.dev']
WHERE
    NOT EXISTS (SELECT 1 FROM projects WHERE external_id = 'PRJ-JHN-004');

-- Seed project for Admin tenant
INSERT INTO projects (
    id,
    tenant_id,
    org_id,
    external_id,
    code,
    name,
    description,
    status,
    project_type,
    progress,
    currency_code,
    sync_status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    assigned_user_ids
)
SELECT
    '00000000-0000-0000-0000-000000000205'::uuid,
    'admin',
    '00000000-0000-0000-0000-000000000001'::uuid,
    'PRJ-ADM-005',
    'PRJ-ADM-005',
    'Admin Portal Infrastructure',
    'Infrastructure project for the admin portal including server setup and monitoring systems.',
    'ACTIVE',
    'INFRASTRUCTURE',
    20.00,
    'EUR',
    'LOCAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system',
    ARRAY['admin@cerp.dev', 'john@cerp.dev', 'metal@cerp.dev']
WHERE
    NOT EXISTS (SELECT 1 FROM projects WHERE external_id = 'PRJ-ADM-005');

COMMENT ON
TABLE projects IS 'Construction projects and project portfolios, scoped by tenant and org (seeded with sample data)';