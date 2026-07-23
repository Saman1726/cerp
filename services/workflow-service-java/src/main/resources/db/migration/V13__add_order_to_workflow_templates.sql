-- Add order_num column to workflow_templates for defining the right order of construction workflows
ALTER TABLE workflow_templates ADD COLUMN order_num INTEGER;

-- Set order values based on Section 23 of ConstructHub_Concept_vNext.md
-- Construction Workflows (23 phases in sequence)
UPDATE workflow_templates
SET
    order_num = 1
WHERE
    code = 'PRE_CONSTRUCTION';

UPDATE workflow_templates SET order_num = 2 WHERE code = 'SITE_PREP';

UPDATE workflow_templates
SET
    order_num = 3
WHERE
    code = 'FOUNDATION';

UPDATE workflow_templates SET order_num = 4 WHERE code = 'BASEMENT';

UPDATE workflow_templates
SET
    order_num = 5
WHERE
    code = 'SUPERSTRUCTURE';

UPDATE workflow_templates SET order_num = 6 WHERE code = 'ROOF';

UPDATE workflow_templates
SET
    order_num = 7
WHERE
    code = 'BUILDING_ENVELOPE';

UPDATE workflow_templates
SET
    order_num = 8
WHERE
    code = 'INTERIOR_PARTITIONS';

UPDATE workflow_templates
SET
    order_num = 9
WHERE
    code = 'MEP_ROUGHIN';

UPDATE workflow_templates
SET
    order_num = 10
WHERE
    code = 'INTERIOR_FINISH';

UPDATE workflow_templates
SET
    order_num = 11
WHERE
    code = 'EXTERIOR_WORKS';

UPDATE workflow_templates
SET
    order_num = 12
WHERE
    code = 'TESTING_COMMISSIONING';

UPDATE workflow_templates
SET
    order_num = 13
WHERE
    code = 'QUALITY_ASSURANCE';

UPDATE workflow_templates
SET
    order_num = 14
WHERE
    code = 'SAFETY_MANAGEMENT';

UPDATE workflow_templates
SET
    order_num = 15
WHERE
    code = 'PROCUREMENT';

UPDATE workflow_templates
SET
    order_num = 16
WHERE
    code = 'EQUIPMENT_MANAGEMENT';

UPDATE workflow_templates
SET
    order_num = 17
WHERE
    code = 'WORKFORCE_MANAGEMENT';

UPDATE workflow_templates
SET
    order_num = 18
WHERE
    code = 'SUBCONTRACTOR_MANAGEMENT';

UPDATE workflow_templates
SET
    order_num = 19
WHERE
    code = 'PROGRESS_MONITORING';

UPDATE workflow_templates
SET
    order_num = 20
WHERE
    code = 'COST_CONTROL';

UPDATE workflow_templates SET order_num = 21 WHERE code = 'SNAGGING';

UPDATE workflow_templates SET order_num = 22 WHERE code = 'HANDOVER';

UPDATE workflow_templates
SET
    order_num = 23
WHERE
    code = 'FACILITY_MANAGEMENT';

-- Add unique constraint to ensure order values are unique
ALTER TABLE workflow_templates
ADD CONSTRAINT uk_template_order_num UNIQUE (order_num);

-- Add index for efficient ordering queries
CREATE INDEX idx_template_order_num ON workflow_templates (order_num);

-- Add comments
COMMENT ON COLUMN workflow_templates.order_num IS 'Order position of the construction workflow phase (1-23 per Section 23 of architecture docs)';