-- Seed default template phase mappings for all 23 construction phases
-- Based on ConstructHub_Concept_vNext.md Section 23
--
-- Building type groups:
--   RESIDENTIAL_TYPE: RESIDENTIAL, COMMERCIAL, INDUSTRIAL, MIXED_USE
--   PUBLIC_TYPE: PUBLIC, PARKING, UTILITY, INFRASTRUCTURE, OTHER
--
-- Templates marked with runs_continuously=true are instantiated once per SubProject
-- and run across multiple phases.

-- Use a placeholder tenant_id that must be replaced per-tenant in production.
-- For seeding during development, we use '00000000-0000-0000-0000-000000000001'.
-- Production systems should insert these per-tenant during org onboarding.

INSERT INTO
    template_phase_mappings (
        tenant_id,
        phase_type,
        building_type_group,
        workflow_template_code,
        priority
    )
VALUES
    -- ============================================
    -- 1. PRE_CONSTRUCTION
    -- ============================================
    (
        '00000000-0000-0000-0000-000000000001',
        'PRE_CONSTRUCTION',
        'RESIDENTIAL_TYPE',
        'PRE_CONSTRUCTION',
        1
    ),
    (
        '00000000-0000-0000-0000-000000000001',
        'PRE_CONSTRUCTION',
        'RESIDENTIAL_TYPE',
        'SAFETY_MANAGEMENT',
        10
    ),
    (
        '00000000-0000-0000-0000-000000000001',
        'PRE_CONSTRUCTION',
        'RESIDENTIAL_TYPE',
        'QUALITY_ASSURANCE',
        11
    ),
    (
        '00000000-0000-0000-0000-000000000001',
        'PRE_CONSTRUCTION',
        'PUBLIC_TYPE',
        'PRE_CONSTRUCTION',
        1
    ),
    (
        '00000000-0000-0000-0000-000000000001',
        'PRE_CONSTRUCTION',
        'PUBLIC_TYPE',
        'SAFETY_MANAGEMENT',
        10
    ),
    (
        '00000000-0000-0000-0000-000000000001',
        'PRE_CONSTRUCTION',
        'PUBLIC_TYPE',
        'QUALITY_ASSURANCE',
        11
    ),

-- ============================================
-- 2. SITE_PREPARATION
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'SITE_PREPARATION',
    'RESIDENTIAL_TYPE',
    'SITE_PREPARATION',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SITE_PREPARATION',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'SITE_PREPARATION',
    'PUBLIC_TYPE',
    'SITE_PREPARATION',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SITE_PREPARATION',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),

-- ============================================
-- 3. FOUNDATION
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'FOUNDATION',
    'RESIDENTIAL_TYPE',
    'FOUNDATION',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'FOUNDATION',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'FOUNDATION',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    11
),
(
    '00000000-0000-0000-0000-000000000001',
    'FOUNDATION',
    'PUBLIC_TYPE',
    'FOUNDATION',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'FOUNDATION',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'FOUNDATION',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    11
),

-- ============================================
-- 4. BASEMENT
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'BASEMENT',
    'RESIDENTIAL_TYPE',
    'BASEMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'BASEMENT',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'BASEMENT',
    'PUBLIC_TYPE',
    'BASEMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'BASEMENT',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),

-- ============================================
-- 5. SUPERSTRUCTURE
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'SUPERSTRUCTURE',
    'RESIDENTIAL_TYPE',
    'SUPERSTRUCTURE',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SUPERSTRUCTURE',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'SUPERSTRUCTURE',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    11
),
(
    '00000000-0000-0000-0000-000000000001',
    'SUPERSTRUCTURE',
    'PUBLIC_TYPE',
    'SUPERSTRUCTURE',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SUPERSTRUCTURE',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'SUPERSTRUCTURE',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    11
),

-- ============================================
-- 6. ROOF
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'ROOF',
    'RESIDENTIAL_TYPE',
    'ROOF',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'ROOF',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'ROOF',
    'PUBLIC_TYPE',
    'ROOF',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'ROOF',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),

-- ============================================
-- 7. BUILDING_ENVELOPE
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'BUILDING_ENVELOPE',
    'RESIDENTIAL_TYPE',
    'BUILDING_ENVELOPE',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'BUILDING_ENVELOPE',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'BUILDING_ENVELOPE',
    'PUBLIC_TYPE',
    'BUILDING_ENVELOPE',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'BUILDING_ENVELOPE',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    10
),

-- ============================================
-- 8. INTERIOR_PARTITIONS
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_PARTITIONS',
    'RESIDENTIAL_TYPE',
    'INTERIOR_PARTITIONS',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_PARTITIONS',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_PARTITIONS',
    'PUBLIC_TYPE',
    'INTERIOR_PARTITIONS',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_PARTITIONS',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    10
),

-- ============================================
-- 9. MEP_ROUGH_IN
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'MEP_ROUGH_IN',
    'RESIDENTIAL_TYPE',
    'MEP_ROUGH_IN',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'MEP_ROUGH_IN',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'MEP_ROUGH_IN',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    11
),
(
    '00000000-0000-0000-0000-000000000001',
    'MEP_ROUGH_IN',
    'PUBLIC_TYPE',
    'MEP_ROUGH_IN',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'MEP_ROUGH_IN',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'MEP_ROUGH_IN',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    11
),

-- ============================================
-- 10. INTERIOR_FINISH
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_FINISH',
    'RESIDENTIAL_TYPE',
    'INTERIOR_FINISH',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_FINISH',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_FINISH',
    'PUBLIC_TYPE',
    'INTERIOR_FINISH',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'INTERIOR_FINISH',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    10
),

-- ============================================
-- 11. EXTERIOR_WORKS
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'EXTERIOR_WORKS',
    'RESIDENTIAL_TYPE',
    'EXTERIOR_WORKS',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'EXTERIOR_WORKS',
    'PUBLIC_TYPE',
    'EXTERIOR_WORKS',
    1
),

-- ============================================
-- 12. TESTING_COMMISSIONING
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'TESTING_COMMISSIONING',
    'RESIDENTIAL_TYPE',
    'TESTING_COMMISSIONING',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'TESTING_COMMISSIONING',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'TESTING_COMMISSIONING',
    'PUBLIC_TYPE',
    'TESTING_COMMISSIONING',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'TESTING_COMMISSIONING',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),

-- ============================================
-- 13. QUALITY_ASSURANCE (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'QUALITY_ASSURANCE',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'QUALITY_ASSURANCE',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    1
),

-- ============================================
-- 14. SAFETY_MANAGEMENT (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'SAFETY_MANAGEMENT',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SAFETY_MANAGEMENT',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    1
),

-- ============================================
-- 15. PROCUREMENT (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'PROCUREMENT',
    'RESIDENTIAL_TYPE',
    'PROCUREMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'PROCUREMENT',
    'PUBLIC_TYPE',
    'PROCUREMENT',
    1
),

-- ============================================
-- 16. EQUIPMENT_MANAGEMENT (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'EQUIPMENT_MANAGEMENT',
    'RESIDENTIAL_TYPE',
    'EQUIPMENT_MANAGEMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'EQUIPMENT_MANAGEMENT',
    'PUBLIC_TYPE',
    'EQUIPMENT_MANAGEMENT',
    1
),

-- ============================================
-- 17. WORKFORCE_MANAGEMENT (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'WORKFORCE_MANAGEMENT',
    'RESIDENTIAL_TYPE',
    'WORKFORCE_MANAGEMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'WORKFORCE_MANAGEMENT',
    'PUBLIC_TYPE',
    'WORKFORCE_MANAGEMENT',
    1
),

-- ============================================
-- 18. SUBCONTRACTOR_MANAGEMENT (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'SUBCONTRACTOR_MANAGEMENT',
    'RESIDENTIAL_TYPE',
    'SUBCONTRACTOR_MANAGEMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SUBCONTRACTOR_MANAGEMENT',
    'PUBLIC_TYPE',
    'SUBCONTRACTOR_MANAGEMENT',
    1
),

-- ============================================
-- 19. PROGRESS_MONITORING (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'PROGRESS_MONITORING',
    'RESIDENTIAL_TYPE',
    'PROGRESS_MONITORING',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'PROGRESS_MONITORING',
    'PUBLIC_TYPE',
    'PROGRESS_MONITORING',
    1
),

-- ============================================
-- 20. COST_CONTROL (continuous)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'COST_CONTROL',
    'RESIDENTIAL_TYPE',
    'COST_CONTROL',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'COST_CONTROL',
    'PUBLIC_TYPE',
    'COST_CONTROL',
    1
),

-- ============================================
-- 21. SNAGGING
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'SNAGGING',
    'RESIDENTIAL_TYPE',
    'SNAGGING',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SNAGGING',
    'RESIDENTIAL_TYPE',
    'QUALITY_ASSURANCE',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'SNAGGING',
    'PUBLIC_TYPE',
    'SNAGGING',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'SNAGGING',
    'PUBLIC_TYPE',
    'QUALITY_ASSURANCE',
    10
),

-- ============================================
-- 22. HANDOVER
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'HANDOVER',
    'RESIDENTIAL_TYPE',
    'HANDOVER',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'HANDOVER',
    'RESIDENTIAL_TYPE',
    'SAFETY_MANAGEMENT',
    10
),
(
    '00000000-0000-0000-0000-000000000001',
    'HANDOVER',
    'PUBLIC_TYPE',
    'HANDOVER',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'HANDOVER',
    'PUBLIC_TYPE',
    'SAFETY_MANAGEMENT',
    10
),

-- ============================================
-- 23. FACILITY_MANAGEMENT (post-handover)
-- ============================================
(
    '00000000-0000-0000-0000-000000000001',
    'FACILITY_MANAGEMENT',
    'RESIDENTIAL_TYPE',
    'FACILITY_MANAGEMENT',
    1
),
(
    '00000000-0000-0000-0000-000000000001',
    'FACILITY_MANAGEMENT',
    'PUBLIC_TYPE',
    'FACILITY_MANAGEMENT',
    1
);