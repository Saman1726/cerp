-- Seed workflow template steps for all 23 construction phases
-- Based on ConstructHub_Concept_vNext.md Section 23
--
-- Type mapping:
--   TASK: Default step type for general work items
--   INSPECTION: Quality checks, verifications, inspections
--   APPROVAL: Formal approval/gate steps
--   MILESTONE: Key completion events (pour, handover)
--
-- Weight defaults: TASK=1.0, INSPECTION=1.5, APPROVAL=2.0, MILESTONE=1.5
-- SLA defaults: TASK=72h, INSPECTION=48h, APPROVAL=48h, MILESTONE=120h
-- Due offset defaults: TASK=3, INSPECTION=2, APPROVAL=2, MILESTONE=5
--
-- requires_evidence: true for INSPECTION, MILESTONE types
-- requires_approval: true for APPROVAL type + items marked "approval" in the document

-- ============================================
-- PRE_CONSTRUCTION (12 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'LAND_ACQUISITION', 'Land acquisition', 'APPROVAL', ARRAY['Project Manager','Finance'], true, true, 4, 96, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'SITE_SURVEY', 'Site survey', 'INSPECTION', ARRAY['Site Engineer'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'GEOTECHNICAL_INVESTIGATION', 'Geotechnical investigation', 'INSPECTION', ARRAY['Geotechnical Engineer','QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'FEASIBILITY_STUDY', 'Feasibility study', 'TASK', ARRAY['Project Manager','PM'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'BUDGET_ESTIMATION', 'Budget estimation', 'APPROVAL', ARRAY['Finance','Project Manager'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'DESIGN_PREPARATION', 'Design preparation', 'TASK', ARRAY['Architect','Structural Engineer'], true, false, 5, 120, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'STRUCTURAL_CALCULATIONS', 'Structural calculations', 'APPROVAL', ARRAY['Structural Engineer'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'MEP_DESIGN', 'MEP design', 'TASK', ARRAY['MEP Engineer'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'PERMIT_APPLICATIONS', 'Permit applications', 'APPROVAL', ARRAY['Project Manager'], true, true, 5, 120, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 10, 'CONTRACTOR_SELECTION', 'Contractor selection', 'APPROVAL', ARRAY['Project Manager','PM'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 11, 'PROCUREMENT_PLANNING', 'Procurement planning', 'TASK', ARRAY['Procurement Manager'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 12, 'CONSTRUCTION_SCHEDULE_APPROVAL', 'Construction schedule approval', 'APPROVAL', ARRAY['Project Manager','PM'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PRE_CONSTRUCTION'
ON CONFLICT DO NOTHING;

-- ============================================
-- SITE_PREPARATION (10 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'SITE_FENCING', 'Site fencing', 'TASK', ARRAY['Site Engineer','Crew'], false, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'TEMPORARY_UTILITIES', 'Temporary utilities', 'TASK', ARRAY['MEP Lead'], false, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'SITE_OFFICE_SETUP', 'Site office setup', 'TASK', ARRAY['Site Engineer'], false, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'SAFETY_SIGNAGE', 'Safety signage', 'TASK', ARRAY['Safety Officer'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'DEMOLITION', 'Demolition (if required)', 'TASK', ARRAY['Crew'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'CLEARING_VEGETATION', 'Clearing vegetation', 'TASK', ARRAY['Crew'], false, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'EARTHWORK', 'Earthwork', 'TASK', ARRAY['Civil Crew'], false, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'EXCAVATION_SITE', 'Excavation', 'TASK', ARRAY['Civil Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'SOIL_COMPACTION', 'Soil compaction', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 10, 'SITE_INSPECTION_APPROVAL', 'Site inspection / Approval to start foundation', 'APPROVAL', ARRAY['QA Inspector','Structural Engineer'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='SITE_PREPARATION'
ON CONFLICT DO NOTHING;

-- ============================================
-- FOUNDATION (9 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'EXCAVATION_VERIFY', 'Excavation verification', 'INSPECTION', ARRAY['Site Engineer','QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'REBAR_INSTALL', 'Rebar installation', 'TASK', ARRAY['Steel Crew Lead'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'FORMWORK', 'Formwork', 'TASK', ARRAY['Formwork Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'UG_UTILS', 'Underground utilities', 'TASK', ARRAY['MEP Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'CONCRETE_POUR', 'Foundation concrete pour', 'MILESTONE', ARRAY['Concrete Lead'], true, false, 2, 120, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'CURING', 'Curing', 'TASK', ARRAY['Site Engineer'], false, false, 5, 120, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'WATERPROOF', 'Waterproofing', 'TASK', ARRAY['Waterproofing Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'FOUNDATION_INSPECTION', 'Foundation inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'STRUCT_APPROVAL', 'Structural approval', 'APPROVAL', ARRAY['Structural Engineer'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='FOUNDATION'
ON CONFLICT DO NOTHING;

-- ============================================
-- BASEMENT (7 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'BASEMENT_WALLS', 'Basement walls', 'TASK', ARRAY['Concrete Lead'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BASEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'BASEMENT_COLUMNS', 'Columns', 'TASK', ARRAY['Steel Crew Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BASEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'BASEMENT_WATERPROOFING', 'Waterproofing', 'TASK', ARRAY['Waterproofing Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BASEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'BASEMENT_DRAINAGE', 'Drainage', 'TASK', ARRAY['MEP Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BASEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'BASEMENT_SLAB', 'Slab construction', 'MILESTONE', ARRAY['Concrete Lead'], true, false, 3, 72, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='BASEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'MECHANICAL_ROOM', 'Mechanical room construction', 'TASK', ARRAY['MEP Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BASEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'BASEMENT_INSPECTION', 'Inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='BASEMENT'
ON CONFLICT DO NOTHING;

-- ============================================
-- SUPERSTRUCTURE (10 steps per floor)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'SUPER_COLUMNS', 'Columns', 'TASK', ARRAY['Steel Crew Lead'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'SUPER_BEAMS', 'Beams', 'TASK', ARRAY['Steel Crew Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'SLAB_REINFORCEMENT', 'Slab reinforcement', 'TASK', ARRAY['Steel Crew Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'ELECTRICAL_CONDUITS', 'Electrical conduits', 'TASK', ARRAY['MEP Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'PLUMBING_ROUGH_IN', 'Plumbing rough-in', 'TASK', ARRAY['MEP Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'CONCRETE_POUR_SUPER', 'Concrete pour', 'MILESTONE', ARRAY['Concrete Lead'], true, false, 2, 120, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'CURING_SUPER', 'Curing', 'TASK', ARRAY['Site Engineer'], false, false, 5, 120, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'FORMWORK_REMOVAL', 'Formwork removal', 'TASK', ARRAY['Formwork Lead'], false, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'SUPER_INSPECTION', 'Structural inspection', 'INSPECTION', ARRAY['QA Inspector','Structural Engineer'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 10, 'SUPER_APPROVAL', 'Structural approval', 'APPROVAL', ARRAY['Structural Engineer'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='SUPERSTRUCTURE'
ON CONFLICT DO NOTHING;

-- ============================================
-- ROOF (7 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'ROOF_STRUCTURE', 'Roof structure', 'TASK', ARRAY['Steel Crew Lead'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='ROOF'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'WATERPROOF_MEMBRANE', 'Waterproof membrane', 'TASK', ARRAY['Waterproofing Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='ROOF'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'INSULATION_ROOF', 'Insulation', 'TASK', ARRAY['Crew'], false, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='ROOF'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'DRAIN_INSTALL', 'Drain installation', 'TASK', ARRAY['MEP Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='ROOF'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'ROOF_CONCRETE', 'Roof concrete', 'MILESTONE', ARRAY['Concrete Lead'], true, false, 2, 120, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='ROOF'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'ROOF_EQUIPMENT_BASES', 'Roof equipment bases', 'TASK', ARRAY['MEP Lead'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='ROOF'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'ROOF_INSPECTION', 'Inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='ROOF'
ON CONFLICT DO NOTHING;

-- ============================================
-- BUILDING_ENVELOPE (8 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'EXT_WALLS', 'Exterior walls', 'TASK', ARRAY['Masonry Crew'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'BRICKWORK', 'Brickwork', 'TASK', ARRAY['Masonry Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'BLOCKWORK', 'Blockwork', 'TASK', ARRAY['Masonry Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'CURTAIN_WALLS', 'Curtain walls', 'TASK', ARRAY['Facade Crew'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'WINDOWS', 'Windows', 'TASK', ARRAY['Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'EXT_DOORS', 'Exterior doors', 'TASK', ARRAY['Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'FACADE_INSULATION', 'Facade insulation', 'TASK', ARRAY['Insulation Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'FACADE_INSPECTION', 'Facade inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='BUILDING_ENVELOPE'
ON CONFLICT DO NOTHING;

-- ============================================
-- INTERIOR_PARTITIONS (7 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'WALL_FRAMING', 'Wall framing', 'TASK', ARRAY['Carpenter'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_PARTITIONS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'DRYWALL', 'Drywall', 'TASK', ARRAY['Drywall Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_PARTITIONS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'MASONRY_PARTITIONS', 'Masonry partitions', 'TASK', ARRAY['Masonry Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_PARTITIONS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'DOOR_FRAMES', 'Door frames', 'TASK', ARRAY['Carpenter'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_PARTITIONS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'CEILING_FRAMING', 'Ceiling framing', 'TASK', ARRAY['Carpenter'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_PARTITIONS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'INTERIOR_PARTITION_INSPECTION', 'Inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_PARTITIONS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'PARTITION_APPROVAL', 'Partition approval', 'APPROVAL', ARRAY['Site Engineer'], true, true, 1, 24, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_PARTITIONS'
ON CONFLICT DO NOTHING;

-- ============================================
-- MEP_ROUGH_IN (20 steps - Electrical/Plumbing/HVAC/Fire)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'MAIN_PANEL', 'Main panel', 'TASK', ARRAY['Electrician'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'CABLE_TRAYS', 'Cable trays', 'TASK', ARRAY['Electrician'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'WIRING', 'Wiring', 'TASK', ARRAY['Electrician'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'SWITCH_BOXES', 'Switch boxes', 'TASK', ARRAY['Electrician'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'GROUNDING', 'Grounding', 'INSPECTION', ARRAY['QA Inspector'], true, false, 1, 24, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'WATER_SUPPLY', 'Water supply', 'TASK', ARRAY['Plumber'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'DRAINAGE', 'Drainage', 'TASK', ARRAY['Plumber'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'VENT_PIPES', 'Vent pipes', 'TASK', ARRAY['Plumber'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'PRESSURE_TEST', 'Pressure testing', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 10, 'DUCT_INSTALL', 'Duct installation', 'TASK', ARRAY['HVAC Tech'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 11, 'CHILLERS', 'Chillers', 'TASK', ARRAY['HVAC Tech'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 12, 'AHU_INSTALL', 'AHU installation', 'TASK', ARRAY['HVAC Tech'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 13, 'PIPE_INSULATION', 'Pipe insulation', 'TASK', ARRAY['HVAC Tech'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 14, 'VENTILATION', 'Ventilation', 'TASK', ARRAY['HVAC Tech'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 15, 'SPRINKLERS', 'Sprinklers', 'TASK', ARRAY['Fire Protection Tech'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 16, 'FIRE_PUMPS', 'Fire pumps', 'TASK', ARRAY['Fire Protection Tech'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 17, 'FIRE_CABINETS', 'Fire cabinets', 'TASK', ARRAY['Fire Protection Tech'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 18, 'FIRE_ALARM_WIRING', 'Fire alarm wiring', 'TASK', ARRAY['Electrician'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 19, 'MEP_INSPECTION', 'MEP inspection (after each system)', 'INSPECTION', ARRAY['QA Inspector','MEP Engineer'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 20, 'MEP_APPROVAL', 'MEP rough-in approval', 'APPROVAL', ARRAY['MEP Engineer','Site Engineer'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='MEP_ROUGH_IN'
ON CONFLICT DO NOTHING;

-- ============================================
-- INTERIOR_FINISH (11 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'PLASTER', 'Plaster', 'TASK', ARRAY['Plasterer'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'PUTTY', 'Putty', 'TASK', ARRAY['Painter'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'PAINTING', 'Painting', 'TASK', ARRAY['Painter'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'FLOORING', 'Flooring', 'TASK', ARRAY['Flooring Specialist'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'TILES', 'Tiles', 'TASK', ARRAY['Tile Setter'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'CEILING_INSTALL', 'Ceiling installation', 'TASK', ARRAY['Carpenter'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'INTERIOR_DOORS', 'Interior doors', 'TASK', ARRAY['Carpenter'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'KITCHEN_INSTALL', 'Kitchen installation', 'TASK', ARRAY['Cabinet Maker'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'BATHROOM_FIXTURES', 'Bathroom fixtures', 'TASK', ARRAY['Plumber'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 10, 'STAIR_FINISHING', 'Stair finishing', 'TASK', ARRAY['Metal Worker','Finisher'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 11, 'ELEVATOR_INSTALL', 'Elevator installation', 'MILESTONE', ARRAY['Elevator Specialist'], true, false, 5, 120, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='INTERIOR_FINISH'
ON CONFLICT DO NOTHING;

-- ============================================
-- EXTERIOR_WORKS (11 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'ROADS', 'Roads', 'TASK', ARRAY['Civil Crew'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'SIDEWALKS', 'Sidewalks', 'TASK', ARRAY['Civil Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'PARKING', 'Parking', 'TASK', ARRAY['Civil Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'LANDSCAPING', 'Landscaping', 'TASK', ARRAY['Landscaper'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'IRRIGATION', 'Irrigation', 'TASK', ARRAY['Irrigation Specialist'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'OUTDOOR_LIGHTING', 'Outdoor lighting', 'TASK', ARRAY['Electrician'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'BOUNDARY_WALL', 'Boundary wall', 'TASK', ARRAY['Masonry Crew'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'GATES', 'Gates', 'TASK', ARRAY['Metal Worker'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'DRAINAGE_EXT', 'Drainage', 'TASK', ARRAY['Civil Crew'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 10, 'EXTERIOR_INSPECTION', 'Exterior works inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 11, 'EXTERIOR_APPROVAL', 'Exterior works approval', 'APPROVAL', ARRAY['Site Engineer','Project Manager'], true, true, 1, 24, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='EXTERIOR_WORKS'
ON CONFLICT DO NOTHING;

-- ============================================
-- TESTING_COMMISSIONING (10 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'ELECTRICAL_TESTING', 'Electrical testing', 'INSPECTION', ARRAY['Electrician','QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'PLUMBING_TESTING', 'Plumbing testing', 'INSPECTION', ARRAY['Plumber','QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'HVAC_BALANCING', 'HVAC balancing', 'INSPECTION', ARRAY['HVAC Tech','QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'FIRE_SYSTEM_TESTING', 'Fire system testing', 'INSPECTION', ARRAY['Fire Protection Tech','QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'ELEVATOR_CERTIFICATION', 'Elevator certification', 'MILESTONE', ARRAY['Elevator Specialist'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'GENERATOR_TESTING', 'Generator testing', 'INSPECTION', ARRAY['Electrician'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'UPS_TESTING', 'UPS testing', 'INSPECTION', ARRAY['Electrician'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'BUILDING_AUTOMATION_TESTING', 'Building automation testing', 'INSPECTION', ARRAY['BMS Technician','QA Inspector'], true, false, 3, 72, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'NETWORK_TESTING', 'Network testing', 'INSPECTION', ARRAY['IT Technician'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 10, 'FINAL_INSPECTIONS_TC', 'Final inspections', 'APPROVAL', ARRAY['QA Inspector','Building Authority'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='TESTING_COMMISSIONING'
ON CONFLICT DO NOTHING;

-- ============================================
-- QUALITY_ASSURANCE (6 steps - continuous)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'MATERIAL_APPROVAL', 'Material approval', 'APPROVAL', ARRAY['QA Inspector','Site Engineer'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='QUALITY_ASSURANCE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'MATERIAL_INSPECTION', 'Material inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='QUALITY_ASSURANCE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'SITE_INSPECTION_QA', 'Site inspection', 'INSPECTION', ARRAY['QA Inspector','Site Engineer'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='QUALITY_ASSURANCE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'NCR_MANAGEMENT', 'NCR management', 'TASK', ARRAY['QA Inspector'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='QUALITY_ASSURANCE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'CORRECTIVE_ACTIONS', 'Corrective actions', 'TASK', ARRAY['QA Inspector','Site Engineer'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='QUALITY_ASSURANCE'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'QUALITY_APPROVAL', 'Quality approval', 'APPROVAL', ARRAY['QA Manager'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='QUALITY_ASSURANCE'
ON CONFLICT DO NOTHING;

-- ============================================
-- SAFETY_MANAGEMENT (6 steps - continuous)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'DAILY_TOOLBOX_MEETING', 'Daily toolbox meeting', 'TASK', ARRAY['Safety Officer'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SAFETY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'SAFETY_INSPECTION', 'Safety inspection', 'INSPECTION', ARRAY['Safety Officer'], true, false, 1, 24, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SAFETY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'PPE_VERIFICATION', 'PPE verification', 'INSPECTION', ARRAY['Safety Officer'], true, false, 1, 24, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SAFETY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'INCIDENT_REPORTING', 'Incident reporting', 'TASK', ARRAY['Safety Officer'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SAFETY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'HAZARD_CORRECTION', 'Hazard correction', 'TASK', ARRAY['Safety Officer','Site Engineer'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SAFETY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'SAFETY_AUDIT', 'Safety audit', 'INSPECTION', ARRAY['Safety Manager'], true, true, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SAFETY_MANAGEMENT'
ON CONFLICT DO NOTHING;

-- ============================================
-- PROCUREMENT (7 steps - continuous)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'MATERIAL_REQUEST', 'Material request', 'TASK', ARRAY['Site Engineer'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PROCUREMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'RFQ', 'RFQ', 'TASK', ARRAY['Procurement Manager'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PROCUREMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'VENDOR_QUOTATION', 'Vendor quotation', 'APPROVAL', ARRAY['Procurement Manager','PM'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PROCUREMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'TECHNICAL_APPROVAL', 'Technical approval', 'APPROVAL', ARRAY['Site Engineer','QA Inspector'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PROCUREMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'PURCHASE_ORDER', 'Purchase order', 'APPROVAL', ARRAY['Project Manager','Finance'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PROCUREMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'DELIVERY_RECEIVING', 'Delivery receiving', 'INSPECTION', ARRAY['Warehouse Manager'], true, false, 1, 24, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='PROCUREMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'INVENTORY_UPDATE', 'Inventory update', 'TASK', ARRAY['Warehouse Manager'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PROCUREMENT'
ON CONFLICT DO NOTHING;

-- ============================================
-- EQUIPMENT_MANAGEMENT (7 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'EQUIPMENT_REQUEST', 'Equipment request', 'TASK', ARRAY['Site Engineer'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EQUIPMENT_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'EQUIPMENT_ALLOCATION', 'Allocation', 'APPROVAL', ARRAY['Equipment Manager'], true, true, 1, 24, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='EQUIPMENT_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'EQUIPMENT_DELIVERY', 'Delivery', 'TASK', ARRAY['Logistics'], false, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EQUIPMENT_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'EQUIPMENT_INSPECTION', 'Inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 1, 24, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='EQUIPMENT_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'EQUIPMENT_USAGE', 'Usage', 'TASK', ARRAY['Operator'], false, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EQUIPMENT_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'EQUIPMENT_MAINTENANCE', 'Maintenance', 'TASK', ARRAY['Maintenance Tech'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='EQUIPMENT_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'EQUIPMENT_RETURN', 'Return', 'INSPECTION', ARRAY['Equipment Manager'], true, false, 1, 24, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='EQUIPMENT_MANAGEMENT'
ON CONFLICT DO NOTHING;

-- ============================================
-- WORKFORCE_MANAGEMENT (7 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'WORKER_ASSIGNMENT', 'Worker assignment', 'TASK', ARRAY['Site Engineer'], false, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='WORKFORCE_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'ATTENDANCE', 'Attendance', 'TASK', ARRAY['Foreman'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='WORKFORCE_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'DAILY_REPORT', 'Daily report', 'TASK', ARRAY['Site Engineer'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='WORKFORCE_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'TIMESHEET', 'Timesheet', 'TASK', ARRAY['HR Coordinator'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='WORKFORCE_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'PRODUCTIVITY', 'Productivity', 'TASK', ARRAY['Site Engineer'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='WORKFORCE_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'PAYROLL_APPROVAL', 'Payroll approval', 'APPROVAL', ARRAY['Finance','HR Manager'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='WORKFORCE_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'WORKFORCE_REVIEW', 'Workforce review', 'INSPECTION', ARRAY['Site Engineer','PM'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='WORKFORCE_MANAGEMENT'
ON CONFLICT DO NOTHING;

-- ============================================
-- SUBCONTRACTOR_MANAGEMENT (9 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'CONTRACT_AWARD', 'Contract award', 'APPROVAL', ARRAY['Project Manager','Finance'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'WORK_ORDER', 'Work order', 'TASK', ARRAY['Project Manager'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'MOBILIZATION', 'Mobilization', 'TASK', ARRAY['Site Engineer'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'PROGRESS_REPORTING', 'Progress reporting', 'TASK', ARRAY['Subcontractor PM'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'SUB_INSPECTION', 'Inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'PAYMENT_APPLICATION', 'Payment application', 'APPROVAL', ARRAY['Finance','Site Engineer'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'RETENTION', 'Retention', 'TASK', ARRAY['Finance'], false, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'COMPLETION', 'Completion', 'APPROVAL', ARRAY['Project Manager','Site Engineer'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'SUB_FINAL_APPROVAL', 'Subcontractor final approval', 'APPROVAL', ARRAY['Project Manager','QA Manager'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='SUBCONTRACTOR_MANAGEMENT'
ON CONFLICT DO NOTHING;

-- ============================================
-- PROGRESS_MONITORING (6 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'DAILY_PROGRESS', 'Daily progress', 'TASK', ARRAY['Site Engineer'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PROGRESS_MONITORING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'WEEKLY_PROGRESS', 'Weekly progress', 'TASK', ARRAY['Site Engineer','Project Manager'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PROGRESS_MONITORING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'SCHEDULE_COMPARISON', 'Schedule comparison', 'TASK', ARRAY['Project Manager'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PROGRESS_MONITORING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'DELAY_ANALYSIS', 'Delay analysis', 'TASK', ARRAY['Project Manager'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='PROGRESS_MONITORING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'RECOVERY_PLAN', 'Recovery plan', 'APPROVAL', ARRAY['Project Manager'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PROGRESS_MONITORING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'OWNER_APPROVAL_PROGRESS', 'Owner approval', 'APPROVAL', ARRAY['Project Owner','PM'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='PROGRESS_MONITORING'
ON CONFLICT DO NOTHING;

-- ============================================
-- COST_CONTROL (7 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'BUDGET_ALLOCATION', 'Budget allocation', 'APPROVAL', ARRAY['Finance','Project Manager'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='COST_CONTROL'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'COST_RECORDING', 'Cost recording', 'TASK', ARRAY['Cost Engineer'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='COST_CONTROL'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'CHANGE_ORDERS', 'Change orders', 'APPROVAL', ARRAY['Project Manager','Finance'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='COST_CONTROL'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'VARIATION_ORDERS', 'Variation orders', 'APPROVAL', ARRAY['Project Manager','Site Engineer'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='COST_CONTROL'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'INVOICE_APPROVAL_CC', 'Invoice approval', 'APPROVAL', ARRAY['Finance','Project Manager'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='COST_CONTROL'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'PAYMENT_CERTIFICATE', 'Payment certificate', 'APPROVAL', ARRAY['Finance'], true, true, 3, 72, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='COST_CONTROL'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'COST_REPORT', 'Cost report', 'TASK', ARRAY['Cost Engineer','Finance'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='COST_CONTROL'
ON CONFLICT DO NOTHING;

-- ============================================
-- SNAGGING (6 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'WALKTHROUGH', 'Walkthrough', 'INSPECTION', ARRAY['QA Inspector','Site Engineer','PM'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SNAGGING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'ISSUE_IDENTIFICATION', 'Issue identification', 'TASK', ARRAY['QA Inspector'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SNAGGING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'ASSIGN_CONTRACTOR', 'Assign contractor', 'TASK', ARRAY['Project Manager'], false, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SNAGGING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'FIX_ISSUE', 'Fix issue', 'TASK', ARRAY['Subcontractor'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='SNAGGING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'RE_INSPECTION', 'Re-inspection', 'INSPECTION', ARRAY['QA Inspector'], true, false, 1, 24, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='SNAGGING'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'CLOSE_ISSUE', 'Close issue', 'APPROVAL', ARRAY['QA Inspector'], true, true, 1, 24, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='SNAGGING'
ON CONFLICT DO NOTHING;

-- ============================================
-- HANDOVER (9 steps)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'FINAL_CLEANING', 'Final cleaning', 'TASK', ARRAY['Crew'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'AS_BUILT_DRAWINGS', 'As-built drawings', 'TASK', ARRAY['Architect','Engineers'], true, false, 5, 120, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'OM_MANUALS', 'O&M manuals', 'TASK', ARRAY['Project Manager'], true, false, 5, 120, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'WARRANTY_DOCUMENTS', 'Warranty documents', 'TASK', ARRAY['Project Manager'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'FINAL_INSPECTION_HANDOVER', 'Final inspection', 'INSPECTION', ARRAY['QA Inspector','Building Authority'], true, false, 2, 48, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'OCCUPANCY_PERMIT', 'Occupancy permit', 'APPROVAL', ARRAY['Building Authority'], true, true, 5, 120, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'CLIENT_TRAINING', 'Client training', 'TASK', ARRAY['Project Manager'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 8, 'CLIENT_ACCEPTANCE', 'Client acceptance', 'APPROVAL', ARRAY['Project Owner'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 9, 'BUILDING_HANDOVER', 'Building handover', 'MILESTONE', ARRAY['Project Manager','Project Owner'], true, true, 2, 48, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='HANDOVER'
ON CONFLICT DO NOTHING;

-- ============================================
-- FACILITY_MANAGEMENT (7 steps - post-handover)
-- ============================================
INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 1, 'WARRANTY_TRACKING', 'Warranty tracking', 'TASK', ARRAY['Facility Manager'], false, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FACILITY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 2, 'PREVENTIVE_MAINTENANCE', 'Preventive maintenance', 'TASK', ARRAY['Facility Manager'], true, false, 5, 120, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FACILITY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 3, 'CORRECTIVE_MAINTENANCE', 'Corrective maintenance', 'TASK', ARRAY['Maintenance Tech'], true, false, 2, 48, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FACILITY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 4, 'ASSET_MANAGEMENT', 'Asset management', 'TASK', ARRAY['Facility Manager'], true, false, 3, 72, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FACILITY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 5, 'SERVICE_REQUESTS', 'Service requests', 'TASK', ARRAY['Facility Manager'], true, false, 1, 24, 1.0, NULL
FROM workflow_templates wt WHERE wt.code='FACILITY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 6, 'BUILDING_INSPECTIONS', 'Building inspections', 'INSPECTION', ARRAY['Facility Manager','QA Inspector'], true, false, 5, 120, 1.5, NULL
FROM workflow_templates wt WHERE wt.code='FACILITY_MANAGEMENT'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_template_steps (id, template_id, step_order, code, name, step_type, assignee_roles, requires_evidence, requires_approval, due_offset_days, sla_hours, weight, checklists)
SELECT gen_random_uuid(), wt.id, 7, 'RENOVATION_PROJECTS', 'Renovation projects', 'APPROVAL', ARRAY['Facility Manager','Project Manager'], true, true, 5, 120, 2.0, NULL
FROM workflow_templates wt WHERE wt.code='FACILITY_MANAGEMENT'
ON CONFLICT DO NOTHING;