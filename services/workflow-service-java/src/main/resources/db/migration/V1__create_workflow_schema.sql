-- All workflow-service DDL: workflow + tenant tables, functions, triggers, views.
-- Flyway sets search_path = workflow; all tables use explicit schema prefix.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS workflow;

-- ============================================================
-- WORKFLOW TEMPLATES
-- ============================================================
CREATE TABLE workflow.workflow_templates (
    id                  UUID         PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    code                VARCHAR(64)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    phase_type          VARCHAR(64)  NOT NULL,
    version             INTEGER      NOT NULL DEFAULT 1,
    parameters_schema   TEXT,
    default_sla         TEXT,
    required_documents  TEXT[],
    status              VARCHAR(16)  NOT NULL CHECK (status IN ('DRAFT','ACTIVE','DEPRECATED')),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    default_scope_level VARCHAR(16)  NOT NULL DEFAULT 'WHOLE',
    order_num           INTEGER,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_template_tenant_code_version UNIQUE (tenant_id, code, version),
    CONSTRAINT uk_template_order_num           UNIQUE (order_num)
);

CREATE INDEX idx_template_tenant_code   ON workflow.workflow_templates (tenant_id, code);
CREATE INDEX idx_template_status        ON workflow.workflow_templates (status);
CREATE INDEX idx_template_phase_type    ON workflow.workflow_templates (phase_type);
CREATE INDEX idx_templates_scope_level  ON workflow.workflow_templates (default_scope_level);
CREATE INDEX idx_template_order_num     ON workflow.workflow_templates (order_num);

COMMENT ON COLUMN workflow.workflow_templates.order_num IS 'Construction phase order (1-23)';
COMMENT ON COLUMN workflow.workflow_templates.default_scope_level IS 'WHOLE, FLOOR, PART';

CREATE OR REPLACE FUNCTION workflow.update_template_is_active()
RETURNS TRIGGER AS $$
BEGIN
    NEW.is_active := (NEW.status = 'ACTIVE');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_template_is_active
    BEFORE UPDATE OF status ON workflow.workflow_templates
    FOR EACH ROW
    EXECUTE FUNCTION workflow.update_template_is_active();

-- ============================================================
-- TEMPLATE ELEMENT COLLECTIONS (@ElementCollection join tables)
-- ============================================================
CREATE TABLE workflow.workflow_template_applicable_scope_levels (
    workflow_template_id UUID        NOT NULL REFERENCES workflow.workflow_templates (id) ON DELETE CASCADE,
    applicable_scope_level VARCHAR(16) NOT NULL,
    PRIMARY KEY (workflow_template_id, applicable_scope_level)
);
CREATE INDEX idx_template_scope_levels_template ON workflow.workflow_template_applicable_scope_levels (workflow_template_id);

CREATE TABLE workflow.workflow_template_required_documents (
    workflow_template_id UUID       NOT NULL REFERENCES workflow.workflow_templates (id) ON DELETE CASCADE,
    required_document    VARCHAR(64) NOT NULL,
    PRIMARY KEY (workflow_template_id, required_document)
);
CREATE INDEX idx_template_required_docs_template ON workflow.workflow_template_required_documents (workflow_template_id);

-- ============================================================
-- WORKFLOW TEMPLATE STEPS
-- ============================================================
CREATE TABLE workflow.workflow_template_steps (
    id                UUID         PRIMARY KEY,
    template_id       UUID         NOT NULL REFERENCES workflow.workflow_templates (id) ON DELETE CASCADE,
    step_order        INTEGER      NOT NULL,
    code              VARCHAR(64)  NOT NULL,
    name              VARCHAR(255) NOT NULL,
    step_type         VARCHAR(16)  NOT NULL CHECK (step_type IN ('TASK','INSPECTION','APPROVAL','MILESTONE')),
    assignee_roles    TEXT[],
    requires_evidence  BOOLEAN      NOT NULL DEFAULT FALSE,
    requires_approval  BOOLEAN      NOT NULL DEFAULT FALSE,
    due_offset_days   INTEGER,
    sla_hours         INTEGER,
    weight            NUMERIC(5,2) DEFAULT 1.0,
    checklists        TEXT,
    gating_condition  TEXT,
    CONSTRAINT uk_template_step_code UNIQUE (template_id, code)
);
CREATE INDEX idx_template_steps_template ON workflow.workflow_template_steps (template_id, step_order);

-- Normalized assignee roles
CREATE TABLE workflow.workflow_template_step_assignee_roles (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    template_step_id UUID         NOT NULL REFERENCES workflow.workflow_template_steps (id) ON DELETE CASCADE,
    role_name        VARCHAR(64)  NOT NULL,
    role_order       INTEGER      NOT NULL DEFAULT 1,
    is_primary       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_step_role UNIQUE (template_step_id, role_name)
);
CREATE INDEX idx_step_assignee_roles_step ON workflow.workflow_template_step_assignee_roles (template_step_id);
CREATE INDEX idx_step_assignee_roles_role ON workflow.workflow_template_step_assignee_roles (role_name);

CREATE OR REPLACE VIEW workflow.workflow_template_steps_with_roles AS
SELECT
    sts.id,
    sts.template_id,
    sts.step_order,
    sts.code,
    sts.name,
    sts.step_type,
    sts.requires_evidence,
    sts.requires_approval,
    sts.due_offset_days,
    sts.sla_hours,
    sts.weight,
    sts.checklists,
    sts.gating_condition,
    ARRAY_AGG(wtsar.role_name ORDER BY wtsar.role_order) AS assignee_roles
FROM workflow.workflow_template_steps sts
LEFT JOIN workflow.workflow_template_step_assignee_roles wtsar ON sts.id = wtsar.template_step_id
GROUP BY sts.id;

-- ============================================================
-- WORKFLOW INSTANCES
-- ============================================================
CREATE TABLE workflow.workflows (
    id               UUID         PRIMARY KEY,
    tenant_id        VARCHAR(255) NOT NULL,
    project_id       UUID         NOT NULL,
    subproject_id    UUID         NOT NULL,
    template_id      UUID         NOT NULL REFERENCES workflow.workflow_templates (id),
    template_version INTEGER      NOT NULL,
    code             VARCHAR(64)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    creation_date    DATE         NOT NULL DEFAULT CURRENT_DATE,
    start_date       DATE,
    end_date         DATE,
    due_date         DATE,
    delivery_date    DATE,
    progress_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    status           VARCHAR(16)  NOT NULL CHECK (status IN (
                         'PLANNED','IN_PROGRESS','BLOCKED','ON_HOLD',
                         'COMPLETED','APPROVED','REJECTED','CANCELED')),
    scope_level      VARCHAR(16)  NOT NULL DEFAULT 'WHOLE',
    next_step_id     UUID,
    who_has_done     UUID,
    who_has_approved UUID,
    created_by       VARCHAR(128),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_tenant_project_subproject ON workflow.workflows (tenant_id, project_id, subproject_id);
CREATE INDEX idx_workflow_tenant_status             ON workflow.workflows (tenant_id, status);
CREATE INDEX idx_workflow_template                  ON workflow.workflows (template_id);
CREATE INDEX idx_workflows_scope_level              ON workflow.workflows (scope_level);

COMMENT ON COLUMN workflow.workflows.scope_level IS 'WHOLE, FLOOR, PART';

CREATE OR REPLACE VIEW workflow.workflows_view AS SELECT * FROM workflow.workflows;

-- @ElementCollection join tables for floor/part scope
CREATE TABLE workflow.workflow_floor_ids (
    workflow_id UUID        NOT NULL REFERENCES workflow.workflows (id) ON DELETE CASCADE,
    floor_id    VARCHAR(64) NOT NULL,
    PRIMARY KEY (workflow_id, floor_id)
);
CREATE INDEX idx_workflow_floor_ids_workflow ON workflow.workflow_floor_ids (workflow_id);

CREATE TABLE workflow.workflow_part_ids (
    workflow_id UUID        NOT NULL REFERENCES workflow.workflows (id) ON DELETE CASCADE,
    part_id     VARCHAR(64) NOT NULL,
    PRIMARY KEY (workflow_id, part_id)
);
CREATE INDEX idx_workflow_part_ids_workflow ON workflow.workflow_part_ids (workflow_id);

-- ============================================================
-- WORKFLOW STEP INSTANCES
-- ============================================================
CREATE TABLE workflow.workflow_steps (
    id               UUID         PRIMARY KEY,
    workflow_id      UUID         NOT NULL REFERENCES workflow.workflows (id) ON DELETE CASCADE,
    template_step_id UUID,
    step_order       INTEGER      NOT NULL,
    code             VARCHAR(64)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    status           VARCHAR(16)  NOT NULL CHECK (status IN (
                         'NOT_STARTED','IN_PROGRESS','BLOCKED','COMPLETED','APPROVED','REJECTED')),
    assignee_user_id UUID,
    assignee_role    VARCHAR(64),
    due_at           TIMESTAMPTZ,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    approved_at      TIMESTAMPTZ,
    approved_by      UUID,
    evidence         TEXT,
    notes            TEXT,
    weight           NUMERIC(5,2) DEFAULT 1.0
);
CREATE INDEX idx_wf_steps_workflow ON workflow.workflow_steps (workflow_id, step_order);
CREATE INDEX idx_wf_steps_status   ON workflow.workflow_steps (workflow_id, status);

-- ============================================================
-- WORKFLOW NOTES
-- ============================================================
CREATE TABLE workflow.workflow_notes (
    id          UUID        PRIMARY KEY,
    workflow_id UUID        NOT NULL REFERENCES workflow.workflows (id) ON DELETE CASCADE,
    step_id     UUID        REFERENCES workflow.workflow_steps (id) ON DELETE SET NULL,
    author_id   UUID        NOT NULL,
    note        TEXT        NOT NULL,
    note_type   VARCHAR(16) NOT NULL CHECK (note_type IN ('GENERAL','RISK','ISSUE','DECISION')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wf_notes_workflow   ON workflow.workflow_notes (workflow_id);
CREATE INDEX idx_wf_notes_step       ON workflow.workflow_notes (step_id);
CREATE INDEX idx_wf_notes_created_at ON workflow.workflow_notes (created_at);

-- ============================================================
-- TEMPLATE PHASE MAPPINGS
-- ============================================================
CREATE TABLE workflow.template_phase_mappings (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             VARCHAR(255) NOT NULL,
    phase_type            VARCHAR(64)  NOT NULL,
    building_type_group   VARCHAR(32)  NOT NULL,
    workflow_template_code VARCHAR(64) NOT NULL,
    priority              INT          NOT NULL DEFAULT 0,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_template_phase_mapping UNIQUE (tenant_id, phase_type, building_type_group, workflow_template_code),
    CONSTRAINT chk_building_type_group   CHECK (building_type_group IN ('RESIDENTIAL_TYPE','PUBLIC_TYPE'))
);
CREATE INDEX idx_template_phase_mappings_tenant   ON workflow.template_phase_mappings (tenant_id);
CREATE INDEX idx_template_phase_mappings_phase    ON workflow.template_phase_mappings (phase_type);
CREATE INDEX idx_template_phase_mappings_template ON workflow.template_phase_mappings (workflow_template_code);

-- ============================================================
-- ORGANISATION WORKFLOW CONFIG
-- ============================================================
CREATE TABLE workflow.organisation_workflow_config (
    id                                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                           VARCHAR(255) NOT NULL,
    org_id                              UUID         NOT NULL,
    auto_generate_on_subproject_creation BOOLEAN     NOT NULL DEFAULT FALSE,
    selected_template_codes             JSONB        NOT NULL DEFAULT '[]'::jsonb,
    custom_phase_mappings               JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at                          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_organisation_workflow_config UNIQUE (tenant_id, org_id)
);
CREATE INDEX idx_organisation_workflow_config_tenant ON workflow.organisation_workflow_config (tenant_id);
CREATE INDEX idx_organisation_workflow_config_org    ON workflow.organisation_workflow_config (org_id);

-- ============================================================
-- OUTBOX EVENTS (transactional outbox pattern)
-- ============================================================
CREATE TABLE workflow.outbox_events (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type     VARCHAR(255) NOT NULL,
    saga_type      VARCHAR(255) NOT NULL,
    saga_id        VARCHAR(255) NOT NULL,
    aggregate_id   UUID,
    aggregate_type VARCHAR(255),
    tenant_id      VARCHAR(255),
    user_id        VARCHAR(255),
    event_data     TEXT,
    metadata       TEXT,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PUBLISHED','FAILED')),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMP,
    error_message  TEXT,
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    version        INTEGER      NOT NULL DEFAULT 1
);
CREATE INDEX idx_outbox_events_status_created ON workflow.outbox_events (status, created_at);
CREATE INDEX idx_outbox_events_saga_id        ON workflow.outbox_events (saga_id);
CREATE INDEX idx_outbox_events_aggregate      ON workflow.outbox_events (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_events_tenant         ON workflow.outbox_events (tenant_id);

-- ============================================================
-- SAGA EVENTS (saga state persistence)
-- ============================================================
CREATE TABLE workflow.saga_events (
    id             VARCHAR(255) PRIMARY KEY,
    saga_type      VARCHAR(255) NOT NULL,
    tenant_id      VARCHAR(255) NOT NULL,
    status         VARCHAR(255) NOT NULL,
    current_step   VARCHAR(255),
    next_step      VARCHAR(255),
    steps_json     TEXT,
    last_error_json TEXT,
    user_id        VARCHAR(255),
    project_id     VARCHAR(255),
    data_json      TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_saga_events_tenant_status      ON workflow.saga_events (tenant_id, status);
CREATE INDEX idx_saga_events_saga_type_status   ON workflow.saga_events (saga_type, status);
CREATE INDEX idx_saga_events_updated_at         ON workflow.saga_events (updated_at);
CREATE INDEX idx_saga_events_user_id            ON workflow.saga_events (user_id);
CREATE INDEX idx_saga_events_project_id         ON workflow.saga_events (project_id);

-- ============================================================
-- TENANTS
-- ============================================================
CREATE TABLE workflow.tenants (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tenants_code      ON workflow.tenants (code);
CREATE INDEX idx_tenants_is_active ON workflow.tenants (is_active);
