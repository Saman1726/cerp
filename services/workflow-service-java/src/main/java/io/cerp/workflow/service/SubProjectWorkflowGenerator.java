package io.cerp.workflow.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.workflow.domain.OrganisationWorkflowConfig;
import io.cerp.workflow.domain.TemplatePhaseMapping;
import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.domain.WorkflowTemplate;
import io.cerp.workflow.dto.WorkflowGenerationRequest;
import io.cerp.workflow.dto.WorkflowGenerationResponse;
import io.cerp.workflow.dto.WorkflowGenerationResponse.GeneratedWorkflowDetail;
import io.cerp.workflow.dto.WorkflowGenerationResponse.SkippedWorkflowDetail;
import io.cerp.workflow.repository.OrganisationWorkflowConfigRepository;
import io.cerp.workflow.repository.TemplatePhaseMappingRepository;
import io.cerp.workflow.repository.WorkflowRepository;
import io.cerp.workflow.repository.WorkflowStepRepository;
import io.cerp.workflow.repository.WorkflowTemplateRepository;

/**
 * SubProjectWorkflowGenerator - Generates workflow instances for a subproject based on template-phase mappings and organisation configuration.
 *
 * <p>
 * This service is idempotent: calling it multiple times for the same subproject and template combination will not create duplicate workflows.
 */
@Service
public class SubProjectWorkflowGenerator {

    private static final Logger log = LoggerFactory.getLogger(SubProjectWorkflowGenerator.class);

    private final TemplatePhaseMappingRepository templatePhaseMappingRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;

    private final OrganisationWorkflowConfigRepository organisationWorkflowConfigRepository;

    public SubProjectWorkflowGenerator(TemplatePhaseMappingRepository templatePhaseMappingRepository,
            WorkflowTemplateRepository workflowTemplateRepository,
            WorkflowRepository workflowRepository,
            OrganisationWorkflowConfigRepository organisationWorkflowConfigRepository,
            WorkflowStepRepository workflowStepRepository) {
        this.templatePhaseMappingRepository = templatePhaseMappingRepository;
        this.workflowTemplateRepository = workflowTemplateRepository;
        this.workflowRepository = workflowRepository;
        this.organisationWorkflowConfigRepository = organisationWorkflowConfigRepository;
        this.workflowStepRepository = workflowStepRepository;
    }

    /**
     * Generate workflows for a subproject based on template-phase mappings.
     *
     * <p>
     * The generation logic:
     * <ol>
     * <li>Look up organisation workflow config for custom mappings/overrides</li>
     * <li>If org has custom mappings, use those; otherwise fall back to tenant-level mappings</li>
     * <li>For each matching mapping, check if a workflow already exists (idempotency)</li>
     * <li>Create workflow instances from the matching templates</li>
     * </ol>
     *
     * @param request
     *            the generation request containing subproject, tenant, and org info
     * @return response with generated and skipped workflow details
     */
    @Transactional
    public WorkflowGenerationResponse generateWorkflows(WorkflowGenerationRequest request) {
        log.info("Generating workflows for subproject {} (tenant: {}, org: {})",
            request.getSubprojectId(), request.getTenantId(), request.getOrgId());

        String tenantId = resolveTenantId(request.getTenantId());
        UUID subprojectId = request.getSubprojectId();
        UUID orgId = request.getOrgId();
        UUID projectId = request.getProjectId();
        String buildingType = request.getBuildingType();
        List<String> phaseTypes = request.getPhaseTypes();
        Map<String, Object> workflowParameters = request.getWorkflowParameters();
        String constructionPhase = request.getConstructionPhase();

        List<GeneratedWorkflowDetail> generatedWorkflows = new ArrayList<>();
        List<SkippedWorkflowDetail> skippedDetails = new ArrayList<>();

        // Determine which building type group this belongs to
        TemplatePhaseMapping.BuildingTypeGroup buildingTypeGroup =
                determineBuildingTypeGroup(buildingType);

        // Get organisation config for custom overrides
        Optional<OrganisationWorkflowConfig> orgConfigOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

        // Get organisation custom phase mappings if they exist
        Map<String, Map<String, List<String>>> orgCustomMappings = Map.of();
        if (orgConfigOpt.isPresent()) {
            OrganisationWorkflowConfig orgConfig = orgConfigOpt.get();
            orgCustomMappings = orgConfig.getCustomPhaseMappings();
        }

        Optional<WorkflowTemplate> templateOpt =
                workflowTemplateRepository.findByTenantIdAndCodeAndStatus("default".equals(tenantId) ? "__system__" : tenantId, constructionPhase,
                    WorkflowTemplate.TemplateStatus.ACTIVE);

        // For each phase type, find matching templates
        if (templateOpt.isPresent()) {
            WorkflowTemplate template = templateOpt.get();

            if (workflowRepository.existsByTenantIdAndProjectIdAndSubprojectIdAndTemplateId(
                tenantId, projectId, subprojectId, template.getId())) {
                skippedDetails.add(new SkippedWorkflowDetail(
                    template.getCode(), constructionPhase,
                    "Workflow already exists for this template"));
            } else {
                Workflow generated = createWorkflowFromTemplate(
                    tenantId, projectId, subprojectId, template,
                    constructionPhase, buildingType, workflowParameters);

                // Generate workflow steps from template
                generateWorkflowSteps(generated, template);

                generatedWorkflows.add(new GeneratedWorkflowDetail(
                    generated.getId(), generated.getCode(),
                    constructionPhase, generated.getName(),
                    generated.getStatus().name()));
                log.info("Generated workflow {} for subproject {} (phase: {}, template: {})",
                    generated.getId(), subprojectId, constructionPhase, template.getCode());

            }

        }

        WorkflowGenerationResponse response = new WorkflowGenerationResponse(
            subprojectId, tenantId,
            generatedWorkflows.size(), skippedDetails.size(),
            generatedWorkflows, skippedDetails);

        log.info("Workflow generation complete for subproject {}: {} generated, {} skipped",
            subprojectId, generatedWorkflows.size(), skippedDetails.size());

        return response;
    }

    /**
     * Generate workflows triggered by subproject creation event (Kafka consumer).
     *
     * <p>
     * This method checks the organisation's auto-generation setting before proceeding.
     *
     * @param tenantId
     *            the tenant ID
     * @param orgId
     *            the organization ID
     * @param subprojectId
     *            the subproject ID
     * @param projectId
     *            the project ID
     * @param buildingType
     *            the building type of the subproject
     * @return response with generated workflow details
     */
    @Transactional
    public WorkflowGenerationResponse generateWorkflowsOnSubprojectCreation(
            String tenantId, UUID orgId, UUID subprojectId, UUID projectId,
            String buildingType, String constructionPhase) {

        log.info("Auto-generation triggered for subproject {} (tenant: {}, org: {})",
            subprojectId, tenantId, orgId);
        String default_id = "00000000-0000-0000-0000-000000000001";
        // Check organisation config for auto-generation setting
        Optional<OrganisationWorkflowConfig> orgConfigOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId("default".equals(tenantId) ? default_id : tenantId, orgId);

        if (orgConfigOpt.isEmpty() || !orgConfigOpt.get().getAutoGenerateOnSubprojectCreation()) {
            log.info("Auto-generation not enabled for org {} (tenant: {}). Skipping.", orgId, tenantId);
            return new WorkflowGenerationResponse(
                subprojectId, tenantId, 0, 0,
                new ArrayList<>(), new ArrayList<>());
        }

        OrganisationWorkflowConfig orgConfig = orgConfigOpt.get();

        // Build request and delegate to main generation method
        WorkflowGenerationRequest request = new WorkflowGenerationRequest();
        request.setSubprojectId(subprojectId);
        request.setTenantId(tenantId);
        request.setOrgId(orgId);
        request.setProjectId(projectId);
        request.setBuildingType(buildingType);
        request.setConstructionPhase(constructionPhase);

        return generateWorkflows(request);
    }

    /**
     * Check if workflows already exist for a subproject (idempotency check).
     */
    public boolean workflowsExistForSubproject(String tenantId, UUID subprojectId) {
        return workflowRepository.countByTenantIdAndSubprojectId(tenantId, subprojectId) > 0;
    }

    /**
     * Get existing workflow IDs for a subproject.
     */
    public List<UUID> getExistingWorkflowIds(String tenantId, UUID subprojectId) {
        return workflowRepository.findIdsByTenantIdAndSubprojectId(tenantId, subprojectId);
    }

    // ── Private helper methods ──

    /**
     * Resolve a tenant identifier that may already be a UUID (the normal case - the API
     * gateway and the Kafka subproject-created event both propagate the tenant as its UUID)
     * or, for legacy/test callers, a tenant code (e.g. "default") that still needs a
     * code-to-UUID lookup via TenantService.
     */
    private String resolveTenantId(String tenantIdOrCode) {
        if (tenantIdOrCode != null) {
            try {
                return UUID.fromString(tenantIdOrCode).toString();
            } catch (IllegalArgumentException notAUuid) {
                // Not a UUID - fall through to the code-based lookup below.
            }
        }
        return TenantService.getInstance().getTenantIdByCodeStr(tenantIdOrCode);
    }

    /**
     * Create a workflow instance from a template.
     */
    private Workflow createWorkflowFromTemplate(String tenantId, UUID projectId,
            UUID subprojectId, WorkflowTemplate template, String phaseType,
            String buildingType, Map<String, Object> parameters) {

        String workflowCode = generateWorkflowCode(template.getCode(), subprojectId);
        String workflowName = buildWorkflowName(template, phaseType, buildingType);

        Workflow workflow = new Workflow(
            tenantId, projectId, subprojectId,
            template.getId(), template.getVersion(),
            workflowCode, workflowName);

        workflow.setDescription(template.getDescription());

        // Set scope level from template default
        String scopeLevel = template.getDefaultScopeLevel();
        if (scopeLevel != null) {
            workflow.setScopeLevel(scopeLevel);
        }

        // Apply any custom parameters
        if (parameters != null) {
            applyWorkflowParameters(workflow, parameters);
        }

        workflowRepository.save(workflow);
        return workflow;
    }

    /**
     * Determine the building type group for a given building type.
     */
    private TemplatePhaseMapping.BuildingTypeGroup determineBuildingTypeGroup(
            String buildingType) {
        if (buildingType == null) {
            return TemplatePhaseMapping.BuildingTypeGroup.PUBLIC_TYPE;
        }

        switch (buildingType.toUpperCase()) {
            case "RESIDENTIAL":
            case "COMMERCIAL":
            case "INDUSTRIAL":
            case "MIXED_USE":
                return TemplatePhaseMapping.BuildingTypeGroup.RESIDENTIAL_TYPE;
            default:
                return TemplatePhaseMapping.BuildingTypeGroup.PUBLIC_TYPE;
        }
    }

    /**
     * Generate a unique workflow code based on template code and subproject.
     */
    private String generateWorkflowCode(String templateCode, UUID subprojectId) {
        String shortId = subprojectId.toString().substring(0, 8);
        return templateCode + "-" + shortId;
    }

    /**
     * Build a human-readable workflow name.
     */
    private String buildWorkflowName(WorkflowTemplate template, String phaseType,
            String buildingType) {
        StringBuilder name = new StringBuilder();
        name.append(template.getName());
        if (buildingType != null) {
            name.append(" (").append(buildingType).append(")");
        }
        name.append(" - ").append(phaseType);
        return name.toString();
    }

    /**
     * Generate workflow steps from a template and attach them to the workflow.
     *
     * <p>
     * For each step in the workflow template, a corresponding WorkflowStep instance is created with the same order, code, name, and weight. Steps start in NOT_STARTED
     * status.
     *
     * @param workflow
     *            the workflow instance to attach steps to
     * @param template
     *            the workflow template containing step definitions
     */
    private void generateWorkflowSteps(Workflow workflow, WorkflowTemplate template) {
        for (io.cerp.workflow.domain.WorkflowTemplateStep templateStep : template.getSteps()) {
            io.cerp.workflow.domain.WorkflowStep step = new io.cerp.workflow.domain.WorkflowStep(
                templateStep.getId(),
                templateStep.getOrder(),
                templateStep.getCode(),
                templateStep.getName());
            // Copy weight from template step
            if (templateStep.getWeight() != null) {
                step.setWeight(templateStep.getWeight());
            }
            step.setWorkflow(workflow);
            workflowStepRepository.save(step);
        }
    }

    /**
     * Apply custom parameters to a workflow instance.
     */
    private void applyWorkflowParameters(Workflow workflow, Map<String, Object> parameters) {
        // Apply start date if provided
        if (parameters.containsKey("startDate")) {
            Object startDate = parameters.get("startDate");
            if (startDate instanceof LocalDate) {
                workflow.setStartDate((LocalDate) startDate);
            }
        }

        // Apply due date if provided
        if (parameters.containsKey("dueDate")) {
            Object dueDate = parameters.get("dueDate");
            if (dueDate instanceof LocalDate) {
                workflow.setDueDate((LocalDate) dueDate);
            }
        }

        // Apply createdBy if provided
        if (parameters.containsKey("createdBy")) {
            Object createdBy = parameters.get("createdBy");
            if (createdBy instanceof String) {
                workflow.setCreatedBy((String) createdBy);
            }
        }
    }
}
