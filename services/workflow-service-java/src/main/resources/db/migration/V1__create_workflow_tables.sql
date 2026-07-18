-- Workflow Templates
CREATE TABLE workflow_templates (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    phase_type VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    parameters_schema TEXT,
    default_sla TEXT,
    required_documents TEXT[],
    status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'DEPRECATED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_template_tenant_code_version UNIQUE (tenant_id, code, version)
);

CREATE INDEX idx_template_tenant_code ON workflow_templates (tenant_id, code);

CREATE INDEX idx_template_status ON workflow_templates (status);

CREATE INDEX idx_template_phase_type ON workflow_templates (phase_type);

-- Workflow Template Steps
CREATE TABLE workflow_template_steps (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES workflow_templates(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    step_type VARCHAR(16) NOT NULL CHECK (step_type IN ('TASK', 'INSPECTION', 'APPROVAL', 'MILESTONE')),
    assignee_roles TEXT[],
    requires_evidence BOOLEAN NOT NULL DEFAULT FALSE,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    due_offset_days INTEGER,
    sla_hours INTEGER,
    weight NUMERIC(5,2) DEFAULT 1.0,
    checklists TEXT,
    gating_condition TEXT
);

CREATE INDEX idx_template_steps_template ON workflow_template_steps (template_id, step_order);

-- Workflow Instances
CREATE TABLE workflows (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    project_id UUID NOT NULL,
    subproject_id UUID NOT NULL,
    template_id UUID NOT NULL REFERENCES workflow_templates (id),
    template_version INTEGER NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    creation_date DATE NOT NULL DEFAULT CURRENT_DATE,
    start_date DATE,
    end_date DATE,
    due_date DATE,
    delivery_date DATE,
    progress_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL CHECK (
        status IN (
            'PLANNED',
            'IN_PROGRESS',
            'BLOCKED',
            'ON_HOLD',
            'COMPLETED',
            'APPROVED',
            'REJECTED',
            'CANCELED'
        )
    ),
    next_step_id UUID,
    who_has_done UUID,
    who_has_approved UUID,
    created_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_tenant_project_subproject ON workflows (
    tenant_id,
    project_id,
    subproject_id
);

CREATE INDEX idx_workflow_tenant_status ON workflows (tenant_id, status);

CREATE INDEX idx_workflow_template ON workflows (template_id);

-- Workflow Step Instances
CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    template_step_id UUID,
    step_order INTEGER NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (
        status IN (
            'NOT_STARTED',
            'IN_PROGRESS',
            'BLOCKED',
            'COMPLETED',
            'APPROVED',
            'REJECTED'
        )
    ),
    assignee_user_id UUID,
    assignee_role VARCHAR(64),
    due_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    approved_by UUID,
    evidence TEXT,
    notes TEXT,
    weight NUMERIC(5, 2) DEFAULT 1.0
);

CREATE INDEX idx_wf_steps_workflow ON workflow_steps (workflow_id, step_order);

CREATE INDEX idx_wf_steps_status ON workflow_steps (workflow_id, status);

-- Workflow Notes
CREATE TABLE workflow_notes (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    step_id UUID REFERENCES workflow_steps (id) ON DELETE SET NULL,
    author_id UUID NOT NULL,
    note TEXT NOT NULL,
    note_type VARCHAR(16) NOT NULL CHECK (
        note_type IN (
            'GENERAL',
            'RISK',
            'ISSUE',
            'DECISION'
        )
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wf_notes_workflow ON workflow_notes (workflow_id);

CREATE INDEX idx_wf_notes_step ON workflow_notes (step_id);

CREATE INDEX idx_wf_notes_created_at ON workflow_notes (created_at);