package io.cerp.workflow.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.workflow.config.KafkaEventPublisher;
import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.domain.WorkflowStep;
import io.cerp.workflow.domain.WorkflowTemplate;
import io.cerp.workflow.domain.WorkflowTemplateStep;
import io.cerp.workflow.repository.WorkflowRepository;
import io.cerp.workflow.repository.WorkflowTemplateRepository;

/**
 * Service layer for Workflow instance aggregate.
 *
 * <p>
 * Handles instantiation from templates, status transitions, step management, and progress recalculation.
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowTemplateRepository templateRepository;

    public WorkflowService(WorkflowRepository workflowRepository,
            WorkflowTemplateRepository templateRepository) {
        this.workflowRepository = workflowRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Instantiate a new Workflow from a template for a SubProject.
     *
     * <p>
     * Creates step instances from the template's step definitions, computing due dates using dueOffsetDays relative to workflow start date.
     *
     * @param tenantId
     *            tenant identifier
     * @param projectId
     *            project identifier
     * @param subprojectId
     *            subproject (building) identifier
     * @param templateId
     *            template to clone steps from
     * @param userId
     *            creator user ID
     * @return the created Workflow instance
     */
    @Transactional
    public Workflow instantiate(String tenantId, UUID projectId, UUID subprojectId,
            UUID templateId, UUID userId) {
        WorkflowTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (template.getStatus() != WorkflowTemplate.TemplateStatus.ACTIVE) {
            throw new IllegalStateException("Template must be ACTIVE to instantiate, current status: " + template.getStatus());
        }

        // Generate unique code: {SUBPROJECT_CODE}-{TEMPLATE_CODE}
        String workflowCode = generateWorkflowCode(subprojectId, template.getCode());

        String workflowName = template.getName() + " (" + subprojectId + ")";

        Workflow workflow = new Workflow(tenantId, projectId, subprojectId, templateId,
            template.getVersion(), workflowCode, workflowName);
        workflow.setCreatedBy(userId.toString());
        workflow.setStatus(Workflow.WorkflowStatus.PLANNED);

        // Create step instances from template steps
        for (WorkflowTemplateStep templateStep : template.getSteps()) {
            WorkflowStep step = new WorkflowStep(
                templateStep.getId(), templateStep.getOrder(),
                templateStep.getCode(), templateStep.getName());

            // Use the primary assignee role from the normalized entities (falling back to TEXT[] for backward compatibility)
            step.setAssigneeRole(templateStep.getPrimaryAssigneeRole());
            step.setWeight(templateStep.getWeight());

            // Compute due date
            if (templateStep.getDueOffsetDays() != null && templateStep.getDueOffsetDays() > 0) {
                step.setDueAt(workflow.getCreationDate().atStartOfDay()
                    .plus(templateStep.getDueOffsetDays(), ChronoUnit.DAYS));
            }

            workflow.addStep(step);
        }

        // Set next step
        WorkflowStep firstStep = workflow.findNextPendingStep();
        if (firstStep != null) {
            workflow.setNextStep(firstStep.getId());
        }

        Workflow saved = workflowRepository.save(workflow);

        log.info("Instantiated workflow {} (code: {}) from template {} for subproject {}",
            saved.getId(), saved.getCode(), templateId, subprojectId);
        return saved;
    }

    /**
     * Get a workflow by ID.
     */
    @Transactional(readOnly = true)
    public Workflow getById(UUID id) {
        return workflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
    }

    /**
     * List workflows for a project.
     */
    @Transactional(readOnly = true)
    public List<Workflow> listByProject(String tenantId, UUID projectId) {
        return workflowRepository.findByTenantIdAndProjectId(tenantId, projectId);
    }

    /**
     * List workflows for a subproject (building).
     */
    @Transactional(readOnly = true)
    public List<Workflow> listBySubproject(String tenantId, UUID projectId, UUID subprojectId) {
        return workflowRepository.findByTenantIdAndProjectIdAndSubprojectId(tenantId, projectId, subprojectId);
    }

    /**
     * Get workflows for a subproject by tenant and subproject ID only.
     */
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflowsBySubprojectId(String tenantId, UUID subprojectId) {
        return workflowRepository.findByTenantIdAndSubprojectId(tenantId, subprojectId);
    }

    /**
     * Get workflows for a subproject without tenant filter.
     * 
     * <p>
     * Used as a fallback when TenantContext is empty (e.g., development, migration, or when the X-Tenant-Id header is not provided and we want to return whatever data
     * exists).
     * </p>
     * 
     * @param subprojectId
     *            the subproject UUID
     * @return list of workflows for the subproject regardless of tenant
     */
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflowsBySubprojectIdWithoutTenant(UUID subprojectId) {
        return workflowRepository.findBySubprojectIdOnly(subprojectId);
    }

    /**
     * Get a workflow with its steps by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Workflow> getWorkflowWithSteps(UUID id) {
        return workflowRepository.findByIdWithSteps(id);
    }

    /**
     * Start a workflow. PLANNED -> IN_PROGRESS.
     */
    @Transactional
    public Workflow startWorkflow(UUID workflowId) {
        Workflow workflow = getById(workflowId);
        workflow.start();
        Workflow saved = workflowRepository.save(workflow);
        updateNextStep(saved);
        log.info("Started workflow {}", workflowId);
        return saved;
    }

    /**
     * Block a workflow. IN_PROGRESS -> BLOCKED.
     */
    @Transactional
    public Workflow blockWorkflow(UUID workflowId, String reason) {
        Workflow workflow = getById(workflowId);
        workflow.block();
        Workflow saved = workflowRepository.save(workflow);
        log.info("Blocked workflow {}: {}", workflowId, reason);
        return saved;
    }

    /**
     * Unblock a workflow. BLOCKED -> IN_PROGRESS.
     */
    @Transactional
    public Workflow unblockWorkflow(UUID workflowId) {
        Workflow workflow = getById(workflowId);
        workflow.unblock();
        Workflow saved = workflowRepository.save(workflow);
        updateNextStep(saved);
        log.info("Unblocked workflow {}", workflowId);
        return saved;
    }

    /**
     * Complete a workflow. IN_PROGRESS -> COMPLETED.
     */
    @Transactional
    public Workflow completeWorkflow(UUID workflowId) {
        Workflow workflow = getById(workflowId);
        workflow.complete();
        Workflow saved = workflowRepository.save(workflow);
        log.info("Completed workflow {}", workflowId);
        return saved;
    }

    /**
     * Approve a workflow. COMPLETED -> APPROVED.
     */
    @Transactional
    public Workflow approveWorkflow(UUID workflowId, UUID userId) {
        Workflow workflow = getById(workflowId);
        workflow.approve();
        workflow.setWhoHasApproved(userId);
        Workflow saved = workflowRepository.save(workflow);
        log.info("Approved workflow {} by user {}", workflowId, userId);
        return saved;
    }

    /**
     * Reject a workflow. COMPLETED -> REJECTED.
     */
    @Transactional
    public Workflow rejectWorkflow(UUID workflowId, UUID userId) {
        Workflow workflow = getById(workflowId);
        workflow.reject();
        workflow.setWhoHasApproved(userId);
        Workflow saved = workflowRepository.save(workflow);
        log.info("Rejected workflow {} by user {}", workflowId, userId);
        return saved;
    }

    /**
     * Cancel a workflow. PLANNED/IN_PROGRESS -> CANCELED.
     */
    @Transactional
    public Workflow cancelWorkflow(UUID workflowId, UUID userId) {
        Workflow workflow = getById(workflowId);
        workflow.cancel();
        Workflow saved = workflowRepository.save(workflow);
        log.info("Cancelled workflow {} by user {}", workflowId, userId);
        return saved;
    }

    /**
     * Start a step. NOT_STARTED -> IN_PROGRESS.
     */
    @Transactional
    public WorkflowStep startStep(UUID workflowId, UUID stepId, UUID userId) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        step.start(userId);
        workflow.setNextStep(findNextPendingStepId(workflowId));
        workflow.setWhoHasDone(userId);
        workflowRepository.save(workflow);

        log.info("Started step {} of workflow {} by user {}", stepId, workflowId, userId);
        return step;
    }

    /**
     * Complete a step. IN_PROGRESS -> COMPLETED.
     */
    @Transactional
    public WorkflowStep completeStep(UUID workflowId, UUID stepId, String evidence,
            String notes, UUID userId) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        step.complete(evidence, notes, userId);
        workflow.setWhoHasDone(userId);
        workflow.recalculateProgress();
        workflow.setNextStep(findNextPendingStepId(workflowId));
        workflowRepository.save(workflow);

        log.info("Completed step {} of workflow {} by user {}", stepId, workflowId, userId);
        return step;
    }

    /**
     * Approve a step. COMPLETED -> APPROVED (for steps requiring approval).
     */
    @Transactional
    public WorkflowStep approveStep(UUID workflowId, UUID stepId, String notes, UUID userId) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        step.approve(notes, userId);
        workflow.setWhoHasApproved(userId);
        workflow.recalculateProgress();
        workflow.setNextStep(findNextPendingStepId(workflowId));
        workflowRepository.save(workflow);

        log.info("Approved step {} of workflow {} by user {}", stepId, workflowId, userId);
        return step;
    }

    /**
     * Reject a step. COMPLETED -> REJECTED.
     */
    @Transactional
    public WorkflowStep rejectStep(UUID workflowId, UUID stepId, String notes, UUID userId) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        step.reject(notes, userId);
        workflow.setWhoHasApproved(userId);
        workflowRepository.save(workflow);

        log.info("Rejected step {} of workflow {} by user {}", stepId, workflowId, userId);
        return step;
    }

    /**
     * Block a step. IN_PROGRESS -> BLOCKED.
     */
    @Transactional
    public WorkflowStep blockStep(UUID workflowId, UUID stepId, String reason) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        step.block(reason);
        workflow.setNextStep(findNextPendingStepId(workflowId));
        workflowRepository.save(workflow);

        log.info("Blocked step {} of workflow {}", stepId, workflowId);
        return step;
    }

    /**
     * Unblock a step. BLOCKED -> IN_PROGRESS.
     */
    @Transactional
    public WorkflowStep unblockStep(UUID workflowId, UUID stepId) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        step.unblock();
        workflow.setNextStep(findNextPendingStepId(workflowId));
        workflowRepository.save(workflow);

        log.info("Unblocked step {} of workflow {}", stepId, workflowId);
        return step;
    }

    // ── Helpers ──

    private WorkflowStep findStepById(UUID workflowId, UUID stepId) {
        return workflowRepository.findByIdWithSteps(workflowId)
            .flatMap(w -> w.getSteps().stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst())
            .orElseThrow(() -> new IllegalArgumentException("Step " + stepId + " not found in workflow " + workflowId));
    }

    private UUID findNextPendingStepId(UUID workflowId) {
        return workflowRepository.findByIdWithSteps(workflowId)
            .map(w -> {
                WorkflowStep next = w.findNextPendingStep();
                return next != null ? next.getId() : null;
            })
            .orElse(null);
    }

    private void updateNextStep(Workflow workflow) {
        UUID nextStepId = workflow.findNextPendingStep() != null
                ? workflow.findNextPendingStep().getId()
                : null;
        workflow.setNextStep(nextStepId);
    }

    private String generateWorkflowCode(UUID subprojectId, String templateCode) {
        // Format: {SUBPROJECT_SHORT}-{TEMPLATE_CODE}
        String shortId = subprojectId.toString().substring(0, 8).toUpperCase();
        return shortId + "-" + templateCode;
    }
}
