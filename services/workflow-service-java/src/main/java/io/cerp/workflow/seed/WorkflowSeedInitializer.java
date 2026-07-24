package io.cerp.workflow.seed;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.cerp.workflow.domain.WorkflowTemplate;
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

        /**
         * Order position in the construction sequence (1-23 per Section 23 of architecture docs).
         */
        int order();

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
        public int order() {
            return 3;
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
                null, // steps
                order()); // orderNum
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            // Check for ANY existing template with this code (regardless of status)
            // to avoid duplicate key violations on order_num and other unique constraints
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 5;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 9;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 10;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 14;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 15;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 22;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 13;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 1;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 2;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 4;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 6;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 7;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 8;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 11;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 12;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 21;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 23;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 16;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 17;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 18;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 19;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
        public int order() {
            return 20;
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
                null,
                order());
        }

        @Override
        public WorkflowTemplate findExisting(WorkflowTemplateService service) {
            try {
                return service.getTemplateByCode(this.code());
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
