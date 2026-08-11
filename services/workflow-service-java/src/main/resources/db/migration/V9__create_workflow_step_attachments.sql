-- Adds a metadata table for photo/document attachments on individual workflow steps. The actual
-- file bytes are uploaded directly from the browser to object storage via document-service's
-- existing, unchanged presign endpoint (POST /api/v1/media/presign) - this table only stores a
-- reference (object_key) plus descriptive metadata, mirroring how document.site_captures stores
-- metadata for the unrelated Site Capture feature.
--
-- id is CLIENT-GENERATED (no DEFAULT) and doubles as the idempotency key: the frontend's
-- offline-first upload queue may retry the "record attachment metadata" call after a dropped
-- connection, and retries must not create duplicate rows - see WorkflowService.addAttachment().
--
-- attachment_count is exposed on workflow_steps via a Hibernate @Formula computed at query time
-- (see WorkflowStep.attachmentCount) - no persisted column for it here.
CREATE TABLE workflow.workflow_step_attachments (
    id                  UUID         PRIMARY KEY,
    workflow_id         UUID         NOT NULL REFERENCES workflow.workflows (id) ON DELETE CASCADE,
    step_id             UUID         NOT NULL REFERENCES workflow.workflow_steps (id) ON DELETE CASCADE,
    uploaded_by         UUID         NOT NULL,
    kind                VARCHAR(16)  NOT NULL CHECK (kind IN ('PHOTO','DOCUMENT')),
    object_key          VARCHAR(512) NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    byte_size           BIGINT       NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wf_step_attachments_workflow   ON workflow.workflow_step_attachments (workflow_id);
CREATE INDEX idx_wf_step_attachments_step       ON workflow.workflow_step_attachments (step_id);
CREATE INDEX idx_wf_step_attachments_created_at ON workflow.workflow_step_attachments (created_at);
