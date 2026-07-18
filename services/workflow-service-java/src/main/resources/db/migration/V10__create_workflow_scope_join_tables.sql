-- V10: Create ElementCollection join tables for floor_ids and part_ids
-- JPA @ElementCollection without @JoinTable creates separate join tables

CREATE TABLE workflow_floor_ids (
    workflow_id UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    floor_ids VARCHAR(64) NOT NULL,
    PRIMARY KEY (workflow_id, floor_ids)
);

CREATE INDEX idx_workflow_floor_ids_workflow ON workflow_floor_ids (workflow_id);

CREATE TABLE workflow_part_ids (
    workflow_id UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    part_ids VARCHAR(64) NOT NULL,
    PRIMARY KEY (workflow_id, part_ids)
);

CREATE INDEX idx_workflow_part_ids_workflow ON workflow_part_ids (workflow_id);