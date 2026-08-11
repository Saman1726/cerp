-- Adds Saga-level notes and photo/document attachments, mirroring workflow_notes /
-- workflow_step_attachments (V9) but WITHOUT step-level scoping: sagas have no stable,
-- individually-addressable step identity (SagaStep/stepsJson is a lossily-(de)serialized JSON
-- array with no id column - see DefaultSagaOrchestrator's (de)serializeSteps). Notes/attachments
-- here attach to the saga as a whole, analogous to a workflow-level WorkflowNote (step = null).
--
-- saga_id is a plain VARCHAR(255) column matching saga_events.id's own type (see V1) - NOT a JPA
-- relationship. SagaEntity has no relationships to any other entity today; nothing here needs to
-- navigate note/attachment -> saga as an object graph (every access starts from a known sagaId
-- path parameter).
--
-- id on saga_notes has no DEFAULT (matches workflow_notes) - the app generates it via
-- @GeneratedValue(strategy = GenerationType.UUID). id on saga_attachments is CLIENT-GENERATED (no
-- DEFAULT either), exactly like workflow_step_attachments - it doubles as the idempotency key for
-- the offline upload queue's retries.
CREATE TABLE workflow.saga_notes (
    id          UUID         PRIMARY KEY,
    saga_id     VARCHAR(255) NOT NULL REFERENCES workflow.saga_events (id) ON DELETE CASCADE,
    author_id   UUID         NOT NULL,
    note        TEXT         NOT NULL,
    note_type   VARCHAR(16)  NOT NULL CHECK (note_type IN ('GENERAL','RISK','ISSUE','DECISION')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_saga_notes_saga_id    ON workflow.saga_notes (saga_id);
CREATE INDEX idx_saga_notes_created_at ON workflow.saga_notes (created_at);

CREATE TABLE workflow.saga_attachments (
    id                  UUID         PRIMARY KEY,
    saga_id             VARCHAR(255) NOT NULL REFERENCES workflow.saga_events (id) ON DELETE CASCADE,
    uploaded_by         UUID         NOT NULL,
    kind                VARCHAR(16)  NOT NULL CHECK (kind IN ('PHOTO','DOCUMENT')),
    object_key          VARCHAR(512) NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    byte_size           BIGINT       NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_saga_attachments_saga_id    ON workflow.saga_attachments (saga_id);
CREATE INDEX idx_saga_attachments_created_at ON workflow.saga_attachments (created_at);
