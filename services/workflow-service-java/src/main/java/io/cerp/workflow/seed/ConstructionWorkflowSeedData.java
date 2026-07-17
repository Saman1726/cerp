package io.cerp.workflow.seed;

import java.util.List;

/**
 * Seed data for all 23 construction phase workflow templates.
 * 
 * Per Section 23, each major construction phase has its own workflow because every phase involves different teams, documents, inspections, and approvals.
 * 
 * Template codes (23 phases): 1. PRE_CONSTRUCTION 2. SITE_PREP 3. FOUNDATION 4. BASEMENT 5. SUPERSTRUCTURE 6. ROOF 7. BUILDING_ENVELOPE 8. INTERIOR_PARTITIONS 9.
 * MEP_ROUGHIN 10. INTERIOR_FINISH 11. EXTERIOR_WORKS 12. TESTING_COMMISSIONING 13. QUALITY_ASSURANCE 14. SAFETY_MANAGEMENT 15. PROCUREMENT 16. EQUIPMENT_MANAGEMENT 17.
 * WORKFORCE_MANAGEMENT 18. SUBCONTRACTOR_MANAGEMENT 19. PROGRESS_MONITORING 20. COST_CONTROL 21. SNAGGING 22. HANDOVER 23. FACILITY_MANAGEMENT
 */
public final class ConstructionWorkflowSeedData {

    private ConstructionWorkflowSeedData() {
        // Utility class
    }

    /**
     * Returns all 23 predefined template codes.
     */
    public static List<String> getAllTemplateCodes() {
        return List.of(
            "PRE_CONSTRUCTION",
            "SITE_PREP",
            "FOUNDATION",
            "BASEMENT",
            "SUPERSTRUCTURE",
            "ROOF",
            "BUILDING_ENVELOPE",
            "INTERIOR_PARTITIONS",
            "MEP_ROUGHIN",
            "INTERIOR_FINISH",
            "EXTERIOR_WORKS",
            "TESTING_COMMISSIONING",
            "QUALITY_ASSURANCE",
            "SAFETY_MANAGEMENT",
            "PROCUREMENT",
            "EQUIPMENT_MANAGEMENT",
            "WORKFORCE_MANAGEMENT",
            "SUBCONTRACTOR_MANAGEMENT",
            "PROGRESS_MONITORING",
            "COST_CONTROL",
            "SNAGGING",
            "HANDOVER",
            "FACILITY_MANAGEMENT");
    }

    /**
     * Returns the standard phases that should be created per SubProject. These are the core construction workflows typically instantiated for every building.
     */
    public static List<String> getStandardPhases() {
        return List.of(
            "PRE_CONSTRUCTION",
            "SITE_PREP",
            "FOUNDATION",
            "BASEMENT",
            "SUPERSTRUCTURE",
            "ROOF",
            "BUILDING_ENVELOPE",
            "INTERIOR_PARTITIONS",
            "MEP_ROUGHIN",
            "INTERIOR_FINISH",
            "EXTERIOR_WORKS",
            "TESTING_COMMISSIONING",
            "SNAGGING",
            "HANDOVER");
    }

    /**
     * Returns the continuous management workflows that run throughout construction.
     */
    public static List<String> getManagementPhases() {
        return List.of(
            "QUALITY_ASSURANCE",
            "SAFETY_MANAGEMENT",
            "PROCUREMENT",
            "EQUIPMENT_MANAGEMENT",
            "WORKFORCE_MANAGEMENT",
            "SUBCONTRACTOR_MANAGEMENT",
            "PROGRESS_MONITORING",
            "COST_CONTROL");
    }

    /**
     * Returns the post-handover workflows.
     */
    public static List<String> getPostHandoverPhases() {
        return List.of("FACILITY_MANAGEMENT");
    }
}
