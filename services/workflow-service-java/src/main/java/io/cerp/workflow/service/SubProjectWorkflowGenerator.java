package io.cerp.workflow.service;

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
    private final OrganisationWorkflowConfigRepository organisationWorkflowConfigRepository;

    public SubProjectWorkflowGenerator(TemplatePhaseMappingRepository templatePhaseMappingRepository,
            WorkflowTemplateRepository workflowTemplateRepository,
            WorkflowRepository workflowRepository,
            OrganisationWorkflowConfigRepository organisationWorkflowConfigRepository) {
        this.templatePhaseMappingRepository = templatePhaseMappingRepository;
        this.workflowTemplateRepository = workflowTemplateRepository;
        this.workflowRepository = workflowRepository;
        this.organisationWorkflowConfigRepository = organisationWorkflowConfigRepository;
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

        String tenantId = request.getTenantId();
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

        // Get the set of phase types to process
        Set<String> phasesToProcess;
        if (phaseTypes != null && !phaseTypes.isEmpty()) {
            phasesToProcess = new HashSet<>(phaseTypes);
        } else {
            // If no phase types specified, get all from mappings
            phasesToProcess = new HashSet<>(
                templatePhaseMappingRepository.findDistinctPhaseTypesByTenantId(tenantId));
        }

        // Get organisation custom phase mappings if they exist
        Map<String, Map<String, List<String>>> orgCustomMappings = Map.of();
        if (orgConfigOpt.isPresent()) {
            OrganisationWorkflowConfig orgConfig = orgConfigOpt.get();
            orgCustomMappings = orgConfig.getCustomPhaseMappings();
        }

        // For each phase type, find matching templates
        for (String phaseType : phasesToProcess) {
            // Check if org has custom mappings for this phase
            if (!orgCustomMappings.isEmpty()
                    && orgCustomMappings.containsKey(phaseType)) {
                Map<String, List<String>> phaseGroups = orgCustomMappings.get(phaseType);
                if (phaseGroups.containsKey(buildingTypeGroup.name())) {
                    List<String> customTemplateCodes = phaseGroups.get(buildingTypeGroup.name());
                    // Load templates by code for this phase
                    for (String templateCode : customTemplateCodes) {
                        Optional<WorkflowTemplate> templateOpt =
                                workflowTemplateRepository.findByTenantIdAndCodeAndStatus(
                                    tenantId, templateCode, WorkflowTemplate.TemplateStatus.ACTIVE);
                        if (templateOpt.isPresent()) {
                            WorkflowTemplate template = templateOpt.get();
                            // Check idempotency - skip if workflow already exists
                            if (workflowRepository
                                .existsByTenantIdAndProjectIdAndSubprojectIdAndTemplateId(
                                    tenantId, projectId, subprojectId, template.getId())) {
                                skippedDetails.add(new SkippedWorkflowDetail(
                                    template.getCode(), phaseType,
                                    "Workflow already exists for this template"));
                                continue;
                            }
                            Workflow generated = createWorkflowFromTemplate(
                                tenantId, projectId, subprojectId, template,
                                phaseType, buildingType, workflowParameters);
                            generatedWorkflows.add(new GeneratedWorkflowDetail(
                                generated.getId(), generated.getCode(),
                                phaseType, generated.getName(),
                                generated.getStatus().name()));
                            log.info("Generated workflow {} for subproject {} (phase: {}, template: {})",
                                generated.getId(), subprojectId, phaseType, template.getCode());
                        }
                    }
                    continue;
                }
            }

            // Fall back to tenant-level mappings
            List<TemplatePhaseMapping> mappings = templatePhaseMappingRepository
                .findByTenantIdAndPhaseTypeAndBuildingTypeGroupAndEnabledTrue(
                    tenantId, phaseType, buildingTypeGroup);

            for (TemplatePhaseMapping mapping : mappings) {
                Optional<WorkflowTemplate> templateOpt =
                        workflowTemplateRepository.findByTenantIdAndCodeAndStatus(
                            tenantId, mapping.getWorkflowTemplateCode(),
                            WorkflowTemplate.TemplateStatus.ACTIVE);

                if (templateOpt.isEmpty()) {
                    log.warn("Template {} not found or not active for mapping {}",
                        mapping.getWorkflowTemplateCode(), mapping.getId());
                    skippedDetails.add(new SkippedWorkflowDetail(
                        mapping.getWorkflowTemplateCode(), phaseType,
                        "Template not found or not active"));
                    continue;
                }

                WorkflowTemplate template = templateOpt.get();

                // Idempotency check - skip if workflow already exists
                if (workflowRepository.existsByTenantIdAndProjectIdAndSubprojectIdAndTemplateId(
                    tenantId, projectId, subprojectId, template.getId())) {
                    skippedDetails.add(new SkippedWorkflowDetail(
                        template.getCode(), phaseType,
                        "Workflow already exists for this template"));
                    continue;
                }

                // Create the workflow instance
                Workflow generated = createWorkflowFromTemplate(
                    tenantId, projectId, subprojectId, template,
                    phaseType, buildingType, workflowParameters);

                generatedWorkflows.add(new GeneratedWorkflowDetail(
                    generated.getId(), generated.getCode(),
                    phaseType, generated.getName(),
                    generated.getStatus().name()));

                log.info("Generated workflow {} for subproject {} (phase: {}, template: {})",
                    generated.getId(), subprojectId, phaseType, template.getCode());
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

        // Check organisation config for auto-generation setting
        Optional<OrganisationWorkflowConfig> orgConfigOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

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
