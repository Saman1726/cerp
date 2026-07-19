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

        // Set a system tenant context for seed data (no HTTP request context during
        // startup)
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
            seedTemplate(PreConstructionTemplateDefinition.INSTANCE);
            seedTemplate(SitePrepTemplateDefinition.INSTANCE);
            seedTemplate(BasementTemplateDefinition.INSTANCE);
            seedTemplate(RoofTemplateDefinition.INSTANCE);
            seedTemplate(BuildingEnvelopeTemplateDefinition.INSTANCE);
            seedTemplate(InteriorPartitionsTemplateDefinition.INSTANCE);
            seedTemplate(ExteriorWorksTemplateDefinition.INSTANCE);
            seedTemplate(TestCommissioningTemplateDefinition.INSTANCE);
            seedTemplate(SnaggingTemplateDefinition.INSTANCE);
            seedTemplate(FacilityManagementTemplateDefinition.INSTANCE);
            seedTemplate(EquipmentManagementTemplateDefinition.INSTANCE);
            seedTemplate(WorkforceManagementTemplateDefinition.INSTANCE);
            seedTemplate(SubcontractorManagementTemplateDefinition.INSTANCE);
            seedTemplate(ProgressMonitoringTemplateDefinition.INSTANCE);
            seedTemplate(CostControlTemplateDefinition.INSTANCE);
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

    // ── Missing Template Definitions (16 templates) ──

    /**
     * Pre-Construction template - initial project setup and planning phase.
     */
    static class PreConstructionTemplateDefinition implements TemplateDefinition {

        static final PreConstructionTemplateDefinition INSTANCE = new PreConstructionTemplateDefinition();

        @Override
        public String code() {
            return "PRE_CONSTRUCTION";
        }

        @Override
        public String name() {
            return "Pre-Construction Workflow";
        }

        @Override
        public String description() {
            return "Project kickoff, design approval, permit acquisition, contractor selection, and pre-construction meetings.";
        }

        @Override
        public String phaseType() {
            return "PRE_CONSTRUCTION";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("PROJECT_PLAN", "DESIGN_DRAWINGS", "PERMIT_APPLICATION", "BUSINESS_CASE");
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
                    step(1, "PROJECT_KICKOFF", "Project kickoff meeting", "TASK", false),
                    step(2, "DESIGN_REVIEW", "Design documents review", "INSPECTION", true),
                    step(3, "PERMIT_ACQUISITION", "Permit and approval acquisition", "TASK", true),
                    step(4, "CONTRACTOR_SELECTION", "Contractor selection and bidding", "APPROVAL", true),
                    step(5, "PRE_CON_MEETING", "Pre-construction conference", "TASK", true),
                    step(6, "CONSTRUCTION_PLAN", "Construction plan approval", "APPROVAL", true),
                    step(7, "SITE_HANDOVER", "Site handover to contractor", "MILESTONE", true)));
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
                1,
                slaForPreConstruction(type),
                null, null, null);
        }

        private Integer slaForPreConstruction(String type) {
            if ("INSPECTION".equals(type))
                return 48;
            if ("APPROVAL".equals(type))
                return 72;
            return null;
        }
    }

    /**
     * Site Preparation template - ground work, clearing, and site setup.
     */
    static class SitePrepTemplateDefinition implements TemplateDefinition {

        static final SitePrepTemplateDefinition INSTANCE = new SitePrepTemplateDefinition();

        @Override
        public String code() {
            return "SITE_PREP";
        }

        @Override
        public String name() {
            return "Site Preparation Workflow";
        }

        @Override
        public String description() {
            return "Site clearing, grubbing, excavation, temporary utilities, erosion control, and site setup.";
        }

        @Override
        public String phaseType() {
            return "SITE_PREP";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("SITE_SURVEY", "EROSION_CONTROL_PLAN", "GRADING_PLAN");
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
                    step(1, "SITE_SURVEY", "Site survey and boundary verification", "INSPECTION", true),
                    step(2, "SITE_CLEARING", "Site clearing and grubbing", "TASK", true),
                    step(3, "DEMOLITION", "Demolition of existing structures", "TASK", true),
                    step(4, "EXCAVATION", "Major excavation and grading", "TASK", true),
                    step(5, "EROSION_CONTROL", "Erosion and sediment control installation", "INSPECTION", true),
                    step(6, "TEMP_UTILITIES", "Temporary utilities installation", "TASK", false),
                    step(7, "SITE_ACCESS", "Site access and road construction", "TASK", false),
                    step(8, "SITE_SETUP", "Site office and facilities setup", "TASK", false),
                    step(9, "SITE_INSPECTION", "Pre-foundation site inspection", "INSPECTION", true)));
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
                "INSPECTION".equals(type),
                1,
                "INSPECTION".equals(type) ? 24 : null,
                null, null, null);
        }
    }

    /**
     * Basement construction template.
     */
    static class BasementTemplateDefinition implements TemplateDefinition {

        static final BasementTemplateDefinition INSTANCE = new BasementTemplateDefinition();

        @Override
        public String code() {
            return "BASEMENT";
        }

        @Override
        public String name() {
            return "Basement Workflow";
        }

        @Override
        public String description() {
            return "Basement excavation, waterproofing, structural walls, floor slab, and drainage systems.";
        }

        @Override
        public String phaseType() {
            return "BASEMENT";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("WATERPROOFING_SPEC", "STRUCTURAL_DRAWINGS", "DRAINAGE_PLAN");
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
                    step(1, "BASEMENT_EXCAVATION", "Basement excavation", "TASK", true),
                    step(2, "PILE_CAP", "Pile cap construction", "TASK", true),
                    step(3, "BASEMENT_WALLS", "Basement wall construction", "TASK", true),
                    step(4, "BASEMENT_SLAB", "Basement floor slab pour", "MILESTONE", true),
                    step(5, "WATERPROOFING", "Waterproofing application", "TASK", true),
                    step(6, "DRAINAGE_SYS", "Drainage system installation", "TASK", true),
                    step(7, "BACKFILL", "Backfill and compaction", "TASK", false),
                    step(8, "BASEMENT_INSPECTION", "Basement structural inspection", "INSPECTION", true)));
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
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForBasement(type),
                null, null, null);
        }

        private Integer slaForBasement(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Roof construction template.
     */
    static class RoofTemplateDefinition implements TemplateDefinition {

        static final RoofTemplateDefinition INSTANCE = new RoofTemplateDefinition();

        @Override
        public String code() {
            return "ROOF";
        }

        @Override
        public String name() {
            return "Roof Workflow";
        }

        @Override
        public String description() {
            return "Roof structure, waterproofing, insulation, drainage, and finishing works.";
        }

        @Override
        public String phaseType() {
            return "ROOF";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("ROOF_SPEC", "WATERPROOFING_SPEC", "INSULATION_CERT");
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
                    step(1, "ROOF_STRUCT", "Roof structural works", "TASK", true),
                    step(2, "ROOF_SLOPE", "Slope preparation", "TASK", false),
                    step(3, "ROOF_WATERPROOF", "Waterproofing application", "TASK", true),
                    step(4, "ROOF_INSULATION", "Insulation installation", "TASK", true),
                    step(5, "ROOF_DRAINAGE", "Drainage and gutter installation", "TASK", true),
                    step(6, "ROOF_FINISH", "Roof finishing works", "TASK", false),
                    step(7, "ROOF_TEST", "Waterfall testing", "INSPECTION", true),
                    step(8, "ROOF_INSPECTION", "Roof inspection and approval", "INSPECTION", true)));
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
                List.of("Roofing Foreman"),
                requiresEvidence,
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForRoof(type),
                null, null, null);
        }

        private Integer slaForRoof(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Building Envelope template - facade, cladding, windows, and exterior finishes.
     */
    static class BuildingEnvelopeTemplateDefinition implements TemplateDefinition {

        static final BuildingEnvelopeTemplateDefinition INSTANCE = new BuildingEnvelopeTemplateDefinition();

        @Override
        public String code() {
            return "BUILDING_ENVELOPE";
        }

        @Override
        public String name() {
            return "Building Envelope Workflow";
        }

        @Override
        public String description() {
            return "Facade, cladding, windows, doors, exterior insulation, and waterproofing membrane application.";
        }

        @Override
        public String phaseType() {
            return "BUILDING_ENVELOPE";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("FACADE_DRAWINGS", "WINDOW_SPEC", "WATERPROOFING_SPEC");
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
                    step(1, "SCAFFOLDING", "Scaffolding erection", "TASK", false),
                    step(2, "FACADE_PREP", "Facade preparation and anchoring", "TASK", true),
                    step(3, "CLADDING", "Cladding panel installation", "TASK", true),
                    step(4, "WINDOWS", "Window and glazing installation", "TASK", true),
                    step(5, "DOORS_EXT", "Exterior door installation", "TASK", true),
                    step(6, "EXT_INSULATION", "Exterior insulation application", "TASK", true),
                    step(7, "WATERPROOF_EXT", "Exterior waterproofing", "TASK", true),
                    step(8, "FAÇADE_FINISH", "Facade finishing works", "TASK", false),
                    step(9, "AIR_TIGHTNESS", "Air tightness testing", "INSPECTION", true),
                    step(10, "ENVELOPE_INSPECTION", "Envelope inspection", "INSPECTION", true)));
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
                List.of("Facade Supervisor"),
                requiresEvidence,
                "INSPECTION".equals(type),
                1,
                "INSPECTION".equals(type) ? 24 : null,
                null, null, null);
        }
    }

    /**
     * Interior Partitions template.
     */
    static class InteriorPartitionsTemplateDefinition implements TemplateDefinition {

        static final InteriorPartitionsTemplateDefinition INSTANCE = new InteriorPartitionsTemplateDefinition();

        @Override
        public String code() {
            return "INTERIOR_PARTITIONS";
        }

        @Override
        public String name() {
            return "Interior Partitions Workflow";
        }

        @Override
        public String description() {
            return "Internal wall framing, drywall, sound insulation, and partition finishes.";
        }

        @Override
        public String phaseType() {
            return "INTERIOR_PARTITIONS";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("PARTITION_DRAWINGS", "FIRE_RATING_SPEC");
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
                    step(1, "PARTITION_LAYOUT", "Partition layout marking", "TASK", false),
                    step(2, "STUD_FRAMING", "Stud framing installation", "TASK", true),
                    step(3, "SOUND_INSUL", "Sound insulation installation", "TASK", true),
                    step(4, "DRYWALL_L1", "Drywall - first layer", "TASK", false),
                    step(5, "DRYWALL_L2", "Drywall - second layer (if required)", "TASK", false),
                    step(6, "JOINT_FINISH", "Joint taping and finishing", "TASK", false),
                    step(7, "PRIMER", "Primer application", "TASK", false),
                    step(8, "PARTITION_INSPECTION", "Partition quality inspection", "INSPECTION", true)));
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
                List.of("Drywall Foreman"),
                requiresEvidence,
                "INSPECTION".equals(type),
                1,
                "INSPECTION".equals(type) ? 24 : null,
                null, null, null);
        }
    }

    /**
     * Exterior Works template.
     */
    static class ExteriorWorksTemplateDefinition implements TemplateDefinition {

        static final ExteriorWorksTemplateDefinition INSTANCE = new ExteriorWorksTemplateDefinition();

        @Override
        public String code() {
            return "EXTERIOR_WORKS";
        }

        @Override
        public String name() {
            return "Exterior Works Workflow";
        }

        @Override
        public String description() {
            return "Landscaping, paving, external lighting, fencing, parking areas, and site utilities connection.";
        }

        @Override
        public String phaseType() {
            return "EXTERIOR_WORKS";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("LANDSCAPING_PLAN", "PAVING_PLAN", "EXTERNAL_LIGHTING_PLAN");
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
                    step(1, "SITE_GRADING", "Site grading and leveling", "TASK", true),
                    step(2, "PAVING", "Paving and hardscaping", "TASK", true),
                    step(3, "LANDSCAPING", "Landscaping and planting", "TASK", true),
                    step(4, "EXT_LIGHTING", "External lighting installation", "TASK", true),
                    step(5, "FENCING", "Fencing and boundary works", "TASK", true),
                    step(6, "PARKING", "Parking area marking and signage", "TASK", false),
                    step(7, "SITE_UTILITIES", "Site utilities connection", "TASK", true),
                    step(8, "EXT_INSPECTION", "Exterior works inspection", "INSPECTION", true)));
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
                List.of("Site Supervisor"),
                requiresEvidence,
                "INSPECTION".equals(type),
                1,
                "INSPECTION".equals(type) ? 24 : null,
                null, null, null);
        }
    }

    /**
     * Testing and Commissioning template.
     */
    static class TestCommissioningTemplateDefinition implements TemplateDefinition {

        static final TestCommissioningTemplateDefinition INSTANCE = new TestCommissioningTemplateDefinition();

        @Override
        public String code() {
            return "TESTING_COMMISSIONING";
        }

        @Override
        public String name() {
            return "Testing & Commissioning Workflow";
        }

        @Override
        public String description() {
            return "Systematic testing of all MEP systems, commissioning, performance verification, and integrated systems testing.";
        }

        @Override
        public String phaseType() {
            return "TESTING_COMMISSIONING";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("TEST_PROCEDURES", "COMMISSIONING_PLAN", "PERFORMANCE_CERTS");
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
                    step(1, "TEST_PLAN", "Testing and commissioning plan", "TASK", true),
                    step(2, "ELEC_TEST", "Electrical systems testing", "INSPECTION", true),
                    step(3, "PLUMBING_TEST", "Plumbing systems testing", "INSPECTION", true),
                    step(4, "HVAC_TEST", "HVAC systems testing", "INSPECTION", true),
                    step(5, "FIRE_TEST", "Fire protection systems testing", "INSPECTION", true),
                    step(6, "ELEVATOR_TEST", "Elevator/escalator testing", "INSPECTION", true),
                    step(7, "BMS_TEST", "Building management system testing", "INSPECTION", true),
                    step(8, "INTEGRATED_TEST", "Integrated systems testing", "INSPECTION", true),
                    step(9, "PERFORMANCE_VER", "Performance verification", "MILESTONE", true),
                    step(10, "COMMISSIONING_REPORT", "Commissioning report submission", "TASK", true)));
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
                List.of("Commissioning Agent"),
                requiresEvidence,
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForTesting(type),
                null, null, null);
        }

        private Integer slaForTesting(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Snagging template - punch list and defect identification.
     */
    static class SnaggingTemplateDefinition implements TemplateDefinition {

        static final SnaggingTemplateDefinition INSTANCE = new SnaggingTemplateDefinition();

        @Override
        public String code() {
            return "SNAGGING";
        }

        @Override
        public String name() {
            return "Snagging Workflow";
        }

        @Override
        public String description() {
            return "Systematic inspection for defects, punch list creation, defect rectification, and re-inspection.";
        }

        @Override
        public String phaseType() {
            return "SNAGGING";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("SNAG_LIST", "DEFECT_PHOTO", "RECTIFICATION_PLAN");
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
                    step(1, "SNAG_PLAN", "Snagging plan and scope definition", "TASK", false),
                    step(2, "INTERIOR_SNAG", "Interior inspection and snag list", "INSPECTION", true),
                    step(3, "EXT_SNAG", "Exterior inspection and snag list", "INSPECTION", true),
                    step(4, "MEP_SNAG", "MEP systems snag list", "INSPECTION", true),
                    step(5, "SNAG_COMPILATION", "Snag list compilation and prioritization", "TASK", true),
                    step(6, "RECTIFICATION", "Defect rectification works", "TASK", true),
                    step(7, "RE_INSPECTION", "Re-inspection of rectified items", "INSPECTION", true),
                    step(8, "FINAL_SNAG", "Final snag-free confirmation", "MILESTONE", true)));
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
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForSnagging(type),
                null, null, null);
        }

        private Integer slaForSnagging(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Facility Management template - post-handover operations and maintenance.
     */
    static class FacilityManagementTemplateDefinition implements TemplateDefinition {

        static final FacilityManagementTemplateDefinition INSTANCE = new FacilityManagementTemplateDefinition();

        @Override
        public String code() {
            return "FACILITY_MANAGEMENT";
        }

        @Override
        public String name() {
            return "Facility Management Workflow";
        }

        @Override
        public String description() {
            return "Property management, maintenance schedules, warranty management, and facility operations handover.";
        }

        @Override
        public String phaseType() {
            return "FACILITY_MANAGEMENT";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("FACILITY_MANUAL", "MAINTENANCE_PLAN", "WARRANTY_SUMMARY");
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
                    step(1, "FACILITY_HANDOVER", "Facility management team handover", "TASK", true),
                    step(2, "FM_MANUAL", "Facility management manual delivery", "TASK", true),
                    step(3, "MAINT_SCHEDULE", "Maintenance schedule setup", "TASK", true),
                    step(4, "WARRANTY_REG", "Warranty registration and tracking", "TASK", true),
                    step(5, "SPARE_PARTS", "Spare parts inventory handover", "TASK", true),
                    step(6, "FACILITY_TRAINING", "Facility operations training", "TASK", true),
                    step(7, "PM_SCHEDULE", "Preventive maintenance schedule activation", "MILESTONE", true),
                    step(8, "WARRANTY_CLOCK", "Warranty period clock start", "MILESTONE", true)));
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
                List.of("Facility Manager"),
                requiresEvidence,
                "MILESTONE".equals(type),
                1,
                slaForFacility(type),
                null, null, null);
        }

        private Integer slaForFacility(String type) {
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Equipment Management template.
     */
    static class EquipmentManagementTemplateDefinition implements TemplateDefinition {

        static final EquipmentManagementTemplateDefinition INSTANCE = new EquipmentManagementTemplateDefinition();

        @Override
        public String code() {
            return "EQUIPMENT_MANAGEMENT";
        }

        @Override
        public String name() {
            return "Equipment Management Workflow";
        }

        @Override
        public String description() {
            return "Equipment procurement, delivery, installation, calibration, and maintenance scheduling.";
        }

        @Override
        public String phaseType() {
            return "EQUIPMENT_MANAGEMENT";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("EQUIPMENT_LIST", "MANUFACTURER_SPECS", "MAINTENANCE Manuals");
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
                    step(1, "EQUIP_REQ", "Equipment requirement specification", "TASK", true),
                    step(2, "EQUIP_PROCURE", "Equipment procurement", "TASK", true),
                    step(3, "EQUIP_DELIVERY", "Equipment delivery to site", "MILESTONE", true),
                    step(4, "EQUIP_INSTALL", "Equipment installation", "TASK", true),
                    step(5, "EQUIP_CALIBRATE", "Calibration and setup", "INSPECTION", true),
                    step(6, "EQUIP_TEST", "Equipment testing and validation", "INSPECTION", true),
                    step(7, "EQUIP_REGISTER", "Equipment register creation", "TASK", true),
                    step(8, "MAINT_SCHEDULE_EQUIP", "Maintenance schedule setup", "TASK", true)));
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
                List.of("Equipment Manager"),
                requiresEvidence,
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForEquipment(type),
                null, null, null);
        }

        private Integer slaForEquipment(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Workforce Management template.
     */
    static class WorkforceManagementTemplateDefinition implements TemplateDefinition {

        static final WorkforceManagementTemplateDefinition INSTANCE = new WorkforceManagementTemplateDefinition();

        @Override
        public String code() {
            return "WORKFORCE_MANAGEMENT";
        }

        @Override
        public String name() {
            return "Workforce Management Workflow";
        }

        @Override
        public String description() {
            return "Labor planning, workforce deployment, attendance tracking, skills certification, and workforce safety training.";
        }

        @Override
        public String phaseType() {
            return "WORKFORCE_MANAGEMENT";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("LABOR_PLAN", "SKILLS_MATRIX", "SAFETY_CERTS");
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
                    step(1, "LABOR_PLAN", "Labor resource planning", "TASK", true),
                    step(2, "WORKFORCE_DEPLOY", "Workforce deployment and scheduling", "TASK", true),
                    step(3, "ATTENDANCE", "Attendance and timesheet tracking", "TASK", false),
                    step(4, "SKILLS_CHECK", "Skills and certification verification", "INSPECTION", true),
                    step(5, "SAFETY_TRAIN", "Safety training delivery", "TASK", true),
                    step(6, "PERFORMANCE_REVIEW", "Workforce performance review", "TASK", true),
                    step(7, "WORKFORCE_REPORT", "Workforce productivity report", "MILESTONE", true)));
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
                List.of("HR Manager"),
                requiresEvidence,
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForWorkforce(type),
                null, null, null);
        }

        private Integer slaForWorkforce(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Subcontractor Management template.
     */
    static class SubcontractorManagementTemplateDefinition implements TemplateDefinition {

        static final SubcontractorManagementTemplateDefinition INSTANCE = new SubcontractorManagementTemplateDefinition();

        @Override
        public String code() {
            return "SUBCONTRACTOR_MANAGEMENT";
        }

        @Override
        public String name() {
            return "Subcontractor Management Workflow";
        }

        @Override
        public String description() {
            return "Subcontractor pre-qualification, onboarding, performance evaluation, and payment certification.";
        }

        @Override
        public String phaseType() {
            return "SUBCONTRACTOR_MANAGEMENT";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("SUBCONTRACT_AGREEMENT", "INSURANCE_CERT", "PERFORMANCE_EVAL");
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
                    step(1, "SUBCON_PREQUAL", "Subcontractor pre-qualification", "APPROVAL", true),
                    step(2, "SUBCON_ONBOARD", "Subcontractor onboarding and induction", "TASK", true),
                    step(3, "SCOPE_BRIEF", "Scope of work briefing", "TASK", true),
                    step(4, "SUBCON_SAFETY", "Subcontractor safety orientation", "INSPECTION", true),
                    step(5, "PERF_MONITOR", "Performance monitoring and tracking", "TASK", true),
                    step(6, "PAYMENT_CERT", "Payment certification", "APPROVAL", true),
                    step(7, "SUBCON_EVAL", "Subcontractor performance evaluation", "APPROVAL", true)));
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
                List.of("Contract Manager"),
                requiresEvidence,
                "INSPECTION".equals(type) || "APPROVAL".equals(type),
                1,
                slaForSubcontractor(type),
                null, null, null);
        }

        private Integer slaForSubcontractor(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("APPROVAL".equals(type))
                return 48;
            return null;
        }
    }

    /**
     * Progress Monitoring template.
     */
    static class ProgressMonitoringTemplateDefinition implements TemplateDefinition {

        static final ProgressMonitoringTemplateDefinition INSTANCE = new ProgressMonitoringTemplateDefinition();

        @Override
        public String code() {
            return "PROGRESS_MONITORING";
        }

        @Override
        public String name() {
            return "Progress Monitoring Workflow";
        }

        @Override
        public String description() {
            return "Schedule tracking, progress reporting, milestone verification, and schedule variance analysis.";
        }

        @Override
        public String phaseType() {
            return "PROGRESS_MONITORING";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("SCHEDULE_UPDATE", "PROGRESS_REPORT", "MILESTONE_CERT");
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
                    step(1, "BASELINE_UPDATE", "Schedule baseline update", "TASK", true),
                    step(2, "PROGRESS_MEASURE", "Progress measurement and data collection", "TASK", true),
                    step(3, "MILESTONE_CHECK", "Milestone verification", "INSPECTION", true),
                    step(4, "VARIANCE_ANALYSIS", "Schedule variance analysis", "TASK", true),
                    step(5, "PROGRESS_REPORT_GEN", "Progress report generation", "TASK", true),
                    step(6, "STAKEHOLDER_UPDATE", "Stakeholder progress update", "TASK", true),
                    step(7, "RECOVERY_PLAN", "Recovery plan development (if behind)", "TASK", true),
                    step(8, "WEEKLY_PROGRESS", "Weekly progress review meeting", "MILESTONE", true)));
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
                List.of("Project Controller"),
                requiresEvidence,
                "INSPECTION".equals(type) || "MILESTONE".equals(type),
                1,
                slaForProgress(type),
                null, null, null);
        }

        private Integer slaForProgress(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }

    /**
     * Cost Control template.
     */
    static class CostControlTemplateDefinition implements TemplateDefinition {

        static final CostControlTemplateDefinition INSTANCE = new CostControlTemplateDefinition();

        @Override
        public String code() {
            return "COST_CONTROL";
        }

        @Override
        public String name() {
            return "Cost Control Workflow";
        }

        @Override
        public String description() {
            return "Budget tracking, cost forecasting, change order management, and financial reporting.";
        }

        @Override
        public String phaseType() {
            return "COST_CONTROL";
        }

        @Override
        public List<String> requiredDocuments() {
            return List.of("BUDGET_REPORT", "COST_FORECAST", "CHANGE_ORDER");
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
                    step(1, "BUDGET_TRACK", "Budget vs actual tracking", "TASK", true),
                    step(2, "COST_FORECAST", "Cost forecasting and projection", "TASK", true),
                    step(3, "CHANGE_ORDER_EVAL", "Change order evaluation", "APPROVAL", true),
                    step(4, "CHANGE_ORDER_APPROVE", "Change order approval", "APPROVAL", true),
                    step(5, "INVOICE_CHECK", "Invoice verification and approval", "INSPECTION", true),
                    step(6, "PAYMENT_APPROVE", "Payment certification approval", "APPROVAL", true),
                    step(7, "COST_REPORT", "Cost performance report generation", "TASK", true),
                    step(8, "VARIANCE_REVIEW", "Cost variance review meeting", "MILESTONE", true)));
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
                List.of("Cost Engineer"),
                requiresEvidence,
                "INSPECTION".equals(type) || "APPROVAL".equals(type) || "MILESTONE".equals(type),
                1,
                slaForCostControl(type),
                null, null, null);
        }

        private Integer slaForCostControl(String type) {
            if ("INSPECTION".equals(type))
                return 24;
            if ("APPROVAL".equals(type))
                return 48;
            if ("MILESTONE".equals(type))
                return 12;
            return null;
        }
    }
}
