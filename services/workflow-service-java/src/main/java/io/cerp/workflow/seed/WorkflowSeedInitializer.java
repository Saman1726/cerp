package io.cerp.workflow.seed;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.domain.WorkflowTemplate;
import io.cerp.workflow.domain.WorkflowTemplateStep;
import io.cerp.workflow.dto.WorkflowTemplateRequest;
import io.cerp.workflow.dto.WorkflowTemplateStepRequest;
import io.cerp.workflow.service.WorkflowTemplateService;

/**
 * Initializes construction workflow templates at startup.
 * 
 * <p>
 * Creates templates in DRAFT status. Admins can review, edit, and activate them via the UI or API.
 * 
 * <p>
 * See Section 23 for the full list of 23 construction phases.
 */
@Component
public class WorkflowSeedInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSeedInitializer.class);

    private final WorkflowTemplateService templateService;

    public WorkflowSeedInitializer(WorkflowTemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public void run(String... args) {
        log.info("Seed data: Initializing construction workflow templates...");

        // Set a system tenant context for seed data (no HTTP request context during startup)
        String originalTenant = TenantContext.getCurrentTenantId();
        try {
            TenantContext.setCurrentTenantId("__system__");

            seedTemplate(FoundationTemplateDefinition.INSTANCE);
            seedTemplate(SuperstructureTemplateDefinition.INSTANCE);
            seedTemplate(MEPRoughInTemplateDefinition.INSTANCE);
            seedTemplate(InteriorFinishTemplateDefinition.INSTANCE);
            seedTemplate(SafetyManagementTemplateDefinition.INSTANCE);
            seedTemplate(ProcurementTemplateDefinition.INSTANCE);
            seedTemplate(HandoverTemplateDefinition.INSTANCE);
            seedTemplate(QAAssuranceTemplateDefinition.INSTANCE);
        } finally {
            // Restore original tenant context
            if (originalTenant != null) {
                TenantContext.setCurrentTenantId(originalTenant);
            } else {
                TenantContext.clear();
            }
        }

        log.info("Seed data: Construction workflow templates initialization complete.");
    }

    private void seedTemplate(TemplateDefinition definition) {
        try {
            WorkflowTemplate existing = definition.findExisting(templateService);
            if (existing != null) {
                log.info("Seed data: Template {} already exists (id: {}), skipping",
                    definition.code(), existing.getId());
                return;
            }

            WorkflowTemplateRequest request = definition.toRequest();
            WorkflowTemplate created = templateService.createTemplate(request);

            // Automatically activate if not already active
            templateService.activateTemplate(definition.code());

            log.info("Seed data: Created and activated template {} (id: {}, code: {})",
                definition.name(), created.getId(), definition.code());
        } catch (Exception e) {
            log.error("Seed data: Failed to create template {}: {}", definition.code(), e.getMessage(), e);
        }
    }

    // ── Template Definitions ──

    /**
     * Foundation template with steps: excavation verification → rebar → formwork → underground utilities → concrete pour → curing → waterproofing → inspection →
     * structural approval.
     */
    interface TemplateDefinition {
        String code();

        String name();

        String description();

        String phaseType();

        default List<String> requiredDocuments() {
            return List.of("METHOD_STATEMENT", "INSPECTION_REPORT");
        }

        WorkflowTemplateRequest toRequest();

        WorkflowTemplate findExisting(WorkflowTemplateService service);
    }

    /**
     * Foundation workflow template.
     */
    static class FoundationTemplateDefinition implements TemplateDefinition {

        static final FoundationTemplateDefinition INSTANCE = new FoundationTemplateDefinition();

        @Override
        public String code() {
            return "FOUNDATION";
        }

        @Override
        public String name() {
            return "Foundation Workflow";
        }

        @Override
        public String description() {
            return "Excavation to structural approval for foundation works.";
        }

        @Override
        public String phaseType() {
            return "FOUNDATION";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("METHOD_STATEMENT", "MIX_DESIGN", "INSPECTION_REPORT");
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                null, // parametersSchema
                null, // defaultSla
                requiredDocuments(),
                List.of(
                    step(1, "EXCAVATION_VERIFY", "Excavation verification", "INSPECTION", true),
                    step(2, "REBAR_INSTALL", "Rebar installation", "TASK", true),
                    step(3, "FORMWORK", "Formwork", "TASK", false),
                    step(4, "UG_UTILS", "Underground utilities", "TASK", false),
                    step(5, "CONCRETE_POUR", "Foundation concrete pour", "MILESTONE", true),
                    step(6, "CURING", "Curing", "TASK", false),
                    step(7, "WATERPROOF", "Waterproofing", "TASK", false),
                    step(8, "FOUNDATION_INSPECTION", "Foundation inspection", "INSPECTION", true),
                    step(9, "STRUCT_APPROVAL", "Structural approval", "APPROVAL", true)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("Site Engineer"),
                requiresEvidence,
                "INSPECTION".equals(type) || "APPROVAL".equals(type),
                "INSPECTION".equals(type) ? 1 : null,
                "INSPECTION".equals(type) ? 24 : null,
                null, // weight
                null, // checklists
                null // gatingCondition
            );
        }
    }

    /**
     * Superstructure template - repeats per floor: columns, beams, slab, conduits, plumbing rough-in, pour, curing, formwork removal, inspection.
     */
    static class SuperstructureTemplateDefinition implements TemplateDefinition {

        static final SuperstructureTemplateDefinition INSTANCE = new SuperstructureTemplateDefinition();

        @Override
        public String code() {
            return "SUPERSTRUCTURE";
        }

        @Override
        public String name() {
            return "Superstructure (Frame) Workflow";
        }

        @Override
        public String description() {
            return "Repetitive floor-by-floor structural works: columns, beams, slabs, and all embedded services.";
        }

        @Override
        public String phaseType() {
            return "SUPERSTRUCTURE";
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                """
                        {
                          "type": "object",
                          "properties": {
                            "floors": {"type": "integer", "default": 5},
                            "repeatedFloorSteps": {"type": "boolean", "default": true}
                          }
                        }
                        """,
                null,
                List.of("METHOD_STATEMENT", "MIX_DESIGN", "STRUCTURAL_DRAWING"),
                List.of(
                    step(1, "COLUMN_REBAR", "Column rebar installation", "TASK", true),
                    step(2, "COLUMN_FORMWORK", "Column formwork", "TASK", false),
                    step(3, "COLUMN_CONCRETE", "Column concrete pour", "MILESTONE", true),
                    step(4, "BEAM_REBAR", "Beam rebar installation", "TASK", true),
                    step(5, "SLAB_FORMWORK", "Slab formwork", "TASK", false),
                    step(6, "SLAB_REBAR", "Slab rebar and conduits", "TASK", true),
                    step(7, "PLUMBINGROUGH", "Plumbing rough-in in slab", "TASK", true),
                    step(8, "SLAB_CONCRETE", "Slab concrete pour", "MILESTONE", true),
                    step(9, "CURING", "Curing (min 7 days)", "TASK", false),
                    step(10, "FORMWORK_REMOVAL", "Formwork removal", "TASK", false),
                    step(11, "STRUCTURAL_INSPECTION", "Structural inspection", "INSPECTION", true)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("Steel Crew Lead"),
                requiresEvidence,
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForSuperstructure(type),
                null, null, null);
        }

        private Integer slaForSuperstructure(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * MEP Rough-In template covering electrical, plumbing, HVAC, and fire protection.
     */
    static class MEPRoughInTemplateDefinition implements TemplateDefinition {

        static final MEPRoughInTemplateDefinition INSTANCE = new MEPRoughInTemplateDefinition();

        @Override
        public String code() {
            return "MEP_ROUGHIN";
        }

        @Override
        public String name() {
            return "MEP Rough-In Workflow";
        }

        @Override
        public String description() {
            return "Electrical wiring, plumbing, HVAC ductwork, and fire protection rough-in before interior finishing.";
        }

        @Override
        public String phaseType() {
            return "MEP_ROUGHIN";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("MEP_DRAWING", "INSPECTION_REPORT", "TEST_PROCEDURE");
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                null,
                null,
                requiredDocuments(),
                List.of(
                    step(1, "ELEC_PANEL", "Main panel installation", "TASK", true),
                    step(2, "CABLE_TRAYS", "Cable trays and conduits", "TASK", false),
                    step(3, "WIRING", "Wiring and switch boxes", "TASK", true),
                    step(4, "GROUNDING", "Grounding system", "TASK", true),
                    step(5, "PLUMBING_SUPPLY", "Water supply piping", "TASK", true),
                    step(6, "DRAINAGE", "Drainage and waste pipes", "TASK", true),
                    step(7, "VENT_PIPES", "Vent pipes installation", "TASK", false),
                    step(8, "HVAC_DUCT", "Duct installation", "TASK", true),
                    step(9, "AHU_INSTALL", "AHU unit installation", "TASK", true),
                    step(10, "FIRE_SPRINKLER", "Sprinkler system", "TASK", true),
                    step(11, "PRESSURE_TEST", "Pressure testing", "INSPECTION", true),
                    step(12, "MEP_INSPECTION", "MEP joint inspection", "INSPECTION", true)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("MEP Lead"),
                requiresEvidence,
                "INSPECTION".equals(type),
                "INSPECTION".equals(type) ? 1 : 2,
                "INSPECTION".equals(type) ? 48 : null,
                null, null, null);
        }
    }

    /**
     * Interior Finish template.
     */
    static class InteriorFinishTemplateDefinition implements TemplateDefinition {

        static final InteriorFinishTemplateDefinition INSTANCE = new InteriorFinishTemplateDefinition();

        @Override
        public String code() {
            return "INTERIOR_FINISH";
        }

        @Override
        public String name() {
            return "Interior Finishing Workflow";
        }

        @Override
        public String description() {
            return "Plaster, painting, flooring, tiles, ceilings, doors, kitchen, bathroom fixtures, and stair finishing.";
        }

        @Override
        public String phaseType() {
            return "INTERIOR_FINISH";
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                null,
                null,
                List.of("FINISH_SCHEDULE", "SAMPLE_APPROVAL"),
                List.of(
                    step(1, "PLASTER", "Plastering", "TASK", false),
                    step(2, "PUTTY", "Putty application", "TASK", false),
                    step(3, "PAINTING", "Painting", "TASK", false),
                    step(4, "FLOORING", "Flooring installation", "TASK", true),
                    step(5, "TILES", "Tile installation", "TASK", true),
                    step(6, "CEILING", "Ceiling installation", "TASK", true),
                    step(7, "INTERIOR_DOORS", "Interior doors and frames", "TASK", false),
                    step(8, "KITCHEN", "Kitchen installation", "TASK", true),
                    step(9, "BATH_FIXTURES", "Bathroom fixtures", "TASK", true),
                    step(10, "STAIR_FINISH", "Stair finishing", "TASK", false),
                    step(11, "INTERIOR_INSPECTION", "Interior quality inspection", "INSPECTION", true)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("Finish Foreman"),
                requiresEvidence,
                "INSPECTION".equals(type),
                1,
                "INSPECTION".equals(type) ? 24 : null,
                null, null, null);
        }
    }

    /**
     * Safety Management template - runs throughout construction.
     */
    static class SafetyManagementTemplateDefinition implements TemplateDefinition {

        static final SafetyManagementTemplateDefinition INSTANCE = new SafetyManagementTemplateDefinition();

        @Override
        public String code() {
            return "SAFETY_MANAGEMENT";
        }

        @Override
        public String name() {
            return "Safety Management Workflow";
        }

        @Override
        public String description() {
            return "Daily toolbox meetings, safety inspections, PPE verification, incident reporting, and safety audits.";
        }

        @Override
        public String phaseType() {
            return "SAFETY_MANAGEMENT";
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                null,
                null,
                List.of("SAFETY_PLAN", "INCIDENT_REPORT"),
                List.of(
                    step(1, "TOOLBOX", "Daily toolbox meeting", "TASK", false),
                    step(2, "PPE_CHECK", "PPE verification", "INSPECTION", true),
                    step(3, "SAFETY_INSPECTION", "Site safety inspection", "INSPECTION", true),
                    step(4, "HAZARD_REPORT", "Hazard identification and report", "TASK", true),
                    step(5, "HAZARD_CORRECTION", "Hazard correction action", "TASK", true),
                    step(6, "INCIDENT_REPORT", "Incident reporting (if applicable)", "TASK", true),
                    step(7, "SAFETY_AUDIT", "Weekly safety audit", "INSPECTION", true)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("Safety Officer"),
                requiresEvidence,
                "INSPECTION".equals(type),
                1,
                "INSPECTION".equals(type) ? 48 : null,
                null, null, null);
        }
    }

    /**
     * Procurement template - continuous workflow.
     */
    static class ProcurementTemplateDefinition implements TemplateDefinition {

        static final ProcurementTemplateDefinition INSTANCE = new ProcurementTemplateDefinition();

        @Override
        public String code() {
            return "PROCUREMENT";
        }

        @Override
        public String name() {
            return "Procurement Workflow";
        }

        @Override
        public String description() {
            return "Material request through RFQ, vendor quotation, purchase order, delivery, inspection, and inventory update.";
        }

        @Override
        public String phaseType() {
            return "PROCUREMENT";
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                null,
                null,
                List.of("MATERIAL_REQUEST", "VENDOR_QUOTATION", "PURCHASE_ORDER"),
                List.of(
                    step(1, "MATERIAL_REQ", "Material request creation", "TASK", false),
                    step(2, "RFQ_ISSUE", "RFQ issuance to vendors", "TASK", true),
                    step(3, "VENDOR_QUOTE", "Vendor quotation receipt", "TASK", true),
                    step(4, "TECHNICAL_APPROVAL", "Technical approval", "APPROVAL", true),
                    step(5, "PO_ISSUE", "Purchase order issuance", "APPROVAL", true),
                    step(6, "DELIVERY", "Material delivery to site", "MILESTONE", true),
                    step(7, "WAREHOUSE_RECEIVE", "Warehouse receiving", "TASK", true),
                    step(8, "MATERIAL_INSPECT", "Material quality inspection", "INSPECTION", true),
                    step(9, "INVENTORY_UPDATE", "Inventory update", "TASK", false)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("Procurement Officer"),
                requiresEvidence,
                "INSPECTION".equals(type) || "APPROVAL".equals(type),
                1,
                slaForProcurement(type),
                null, null, null);
        }

        private Integer slaForProcurement(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("APPROVAL".equals(type))
                return 48;
            return null;
        }
    }

    /**
     * Handover template - final project phase.
     */
    static class HandoverTemplateDefinition implements TemplateDefinition {

        static final HandoverTemplateDefinition INSTANCE = new HandoverTemplateDefinition();

        @Override
        public String code() {
            return "HANDOVER";
        }

        @Override
        public String name() {
            return "Handover Workflow";
        }

        @Override
        public String description() {
            return "Final cleaning, as-built drawings, O&M manuals, warranty documents, client training, and building handover.";
        }

        @Override
        public String phaseType() {
            return "HANDOVER";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("AS_BUILT_DRAWINGS", "O_MANUALS", "WARRANTY_DOCS", "OCCUPANCY_PERMIT");
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                null,
                null,
                requiredDocuments(),
                List.of(
                    step(1, "FINAL_CLEAN", "Final cleaning", "TASK", false),
                    step(2, "AS_BUILT_DRAW", "As-built drawings preparation", "TASK", true),
                    step(3, "O_MANUALS", "Operation & maintenance manuals", "TASK", true),
                    step(4, "WARRANTY_DOCS", "Warranty documents", "TASK", true),
                    step(5, "FINAL_INSPECTION", "Final inspection", "INSPECTION", true),
                    step(6, "OCCUPANCY_PERMIT", "Occupancy permit acquisition", "TASK", true),
                    step(7, "CLIENT_TRAINING", "Client training", "TASK", true),
                    step(8, "CLIENT_ACCEPTANCE", "Client acceptance sign-off", "APPROVAL", true),
                    step(9, "BUILDING_HANDOVER", "Building handover", "MILESTONE", true)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("Project Manager"),
                requiresEvidence,
                "INSPECTION".equals(type) || "APPROVAL".equals(type),
                2,
                slaForHandover(type),
                null, null, null);
        }

        private Integer slaForHandover(String type) {
            if ("INSPECTION".equals(type))
                return 48;
            if ("APPROVAL".equals(type))
                return 72;
            return null;
        }
    }

    /**
     * QA & Assurance template - runs throughout construction.
     */
    static class QAAssuranceTemplateDefinition implements TemplateDefinition {

        static final QAAssuranceTemplateDefinition INSTANCE = new QAAssuranceTemplateDefinition();

        @Override
        public String code() {
            return "QUALITY_ASSURANCE";
        }

        @Override
        public String name() {
            return "Quality Assurance Workflow";
        }

        @Override
        public String description() {
            return "Material approval, inspections, NCR management, corrective actions, and quality approvals.";
        }

        @Override
        public String phaseType() {
            return "QUALITY_ASSURANCE";
        }

        @Override
        public WorkflowTemplateRequest toRequest() {
            return new WorkflowTemplateRequest(
                code(),
                name(),
                description(),
                phaseType(),
                null,
                null,
                List.of("QA_PLAN", "INSPECTION_REPORT", "NCR_FORM"),
                List.of(
                    step(1, "MATERIAL_APPROVAL", "Material approval request", "APPROVAL", true),
                    step(2, "MATERIAL_INSPECTION", "Material inspection", "INSPECTION", true),
                    step(3, "SITE_INSPECTION", "Site quality inspection", "INSPECTION", true),
                    step(4, "NCR_CREATION", "Non-conformance report creation", "TASK", true),
                    step(5, "CORRECTIVE_ACTION", "Corrective action implementation", "TASK", true),
                    step(6, "QUALITY_APPROVAL", "Quality approval sign-off", "APPROVAL", true)));
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getActiveTemplate(code());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private WorkflowTemplateStepRequest step(int order, String code, String name, String type,
                boolean requiresEvidence) {
            return new WorkflowTemplateStepRequest(
                order,
                code,
                name,
                WorkflowTemplateStepRequest.StepType.valueOf(type),
                List.of("QA Inspector"),
                requiresEvidence,
                "INSPECTION".equals(type) || "APPROVAL".equals(type),
                1,
                slaForQA(type),
                null, null, null);
        }

        private Integer slaForQA(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("APPROVAL".equals(type))
                return 48;
            return null;
        }
    }
}
