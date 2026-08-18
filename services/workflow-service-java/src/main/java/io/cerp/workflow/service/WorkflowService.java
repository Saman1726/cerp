package io.cerp.workflow.service;

import java.time.temporal.ChronoUnit;
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

import io.cerp.workflow.config.KafkaEventPublisher;
import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.domain.WorkflowNote;
import io.cerp.workflow.domain.WorkflowStep;
import io.cerp.workflow.domain.WorkflowStepAttachment;
import io.cerp.workflow.domain.WorkflowTemplate;
import io.cerp.workflow.domain.WorkflowTemplateStep;
import io.cerp.workflow.repository.WorkflowNoteRepository;
import io.cerp.workflow.repository.WorkflowRepository;
import io.cerp.workflow.repository.WorkflowStepAttachmentRepository;
import io.cerp.workflow.repository.WorkflowStepRepository;
import io.cerp.workflow.repository.WorkflowTemplateRepository;
import io.cerp.workflow.security.IdentityPermissionClient;
import io.cerp.workflow.state.StateMachineEngine;
import io.cerp.workflow.state.WorkflowStateMachines;
import io.cerp.workflow.state.WorkflowStateStore;
import io.cerp.workflow.state.WorkflowStepStateStore;

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
    private final WorkflowStepRepository workflowStepRepository;
    private final StateMachineEngine stateMachineEngine;
    private final WorkflowStateStore workflowStateStore;
    private final WorkflowStepStateStore workflowStepStateStore;
    private final WorkflowNoteRepository workflowNoteRepository;
    private final WorkflowStepAttachmentRepository workflowStepAttachmentRepository;
    private final IdentityPermissionClient identityPermissionClient;

    public WorkflowService(WorkflowRepository workflowRepository,
            WorkflowTemplateRepository templateRepository,
            WorkflowStepRepository workflowStepRepository,
            StateMachineEngine stateMachineEngine,
            WorkflowStateStore workflowStateStore,
            WorkflowStepStateStore workflowStepStateStore,
            WorkflowNoteRepository workflowNoteRepository,
            WorkflowStepAttachmentRepository workflowStepAttachmentRepository,
            IdentityPermissionClient identityPermissionClient) {
        this.workflowRepository = workflowRepository;
        this.templateRepository = templateRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.stateMachineEngine = stateMachineEngine;
        this.workflowStateStore = workflowStateStore;
        this.workflowStepStateStore = workflowStepStateStore;
        this.workflowNoteRepository = workflowNoteRepository;
        this.workflowStepAttachmentRepository = workflowStepAttachmentRepository;
        this.identityPermissionClient = identityPermissionClient;
    }

    private void transition(String aggregateType, UUID aggregateId, String transitionCode,
            StateMachineEngine.StateStore store) {
        stateMachineEngine.executeTransition(aggregateType, aggregateId, transitionCode, store, Map.of());
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
        workflowRepository.save(workflow);
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

            step.setWorkflow(workflow);
            workflowStepRepository.save(step);
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
     * List all workflows for a tenant, regardless of project/subproject.
     */
    @Transactional(readOnly = true)
    public List<Workflow> listAll(String tenantId) {
        return workflowRepository.findByTenantIdWithSteps(tenantId);
    }

    /**
     * List workflows for a project.
     */
    @Transactional(readOnly = true)
    public List<Workflow> listByProject(String tenantId, UUID projectId) {
        return workflowRepository.findByTenantIdAndProjectIdAndDeletedFalse(tenantId, projectId);
    }

    /**
     * List workflows for a subproject (building).
     */
    @Transactional(readOnly = true)
    public List<Workflow> listBySubproject(String tenantId, UUID projectId, UUID subprojectId) {
        return workflowRepository.findByTenantIdAndProjectIdAndSubprojectIdAndDeletedFalse(tenantId, projectId, subprojectId);
    }

    /**
     * Get workflows for a subproject by tenant and subproject ID only.
     */
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflowsBySubprojectId(String tenantId, UUID subprojectId) {
        return workflowRepository.findByTenantIdAndSubprojectIdAndDeletedFalse(tenantId, subprojectId);
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
     * Archive every Workflow tied to a Project - called when the Project itself is deleted.
     */
    @Transactional
    public int archiveByProjectId(UUID projectId) {
        return workflowRepository.archiveByProjectId(projectId);
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
        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW, workflowId, WorkflowStateMachines.TRANSITION_START,
            workflowStateStore);
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
        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW, workflowId, WorkflowStateMachines.TRANSITION_BLOCK,
            workflowStateStore);
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
        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW, workflowId, WorkflowStateMachines.TRANSITION_UNBLOCK,
            workflowStateStore);
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
        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW, workflowId, WorkflowStateMachines.TRANSITION_COMPLETE,
            workflowStateStore);
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
        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW, workflowId, WorkflowStateMachines.TRANSITION_APPROVE,
            workflowStateStore);
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
        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW, workflowId, WorkflowStateMachines.TRANSITION_REJECT,
            workflowStateStore);
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
        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW, workflowId, WorkflowStateMachines.TRANSITION_CANCEL,
            workflowStateStore);
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

        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, stepId, WorkflowStateMachines.TRANSITION_START,
            workflowStepStateStore);
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

        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, stepId, WorkflowStateMachines.TRANSITION_COMPLETE,
            workflowStepStateStore);
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

        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, stepId, WorkflowStateMachines.TRANSITION_APPROVE,
            workflowStepStateStore);
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

        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, stepId, WorkflowStateMachines.TRANSITION_REJECT,
            workflowStepStateStore);
        step.reject(notes, userId);
        workflow.setWhoHasApproved(userId);
        workflowRepository.save(workflow);

        log.info("Rejected step {} of workflow {} by user {}", stepId, workflowId, userId);
        return step;
    }

    /**
     * Restart a rejected step. REJECTED -> IN_PROGRESS, so the assignee can redo the work and
     * resubmit for approval.
     */
    @Transactional
    public WorkflowStep restartStep(UUID workflowId, UUID stepId, UUID userId) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, stepId, WorkflowStateMachines.TRANSITION_RESTART,
            workflowStepStateStore);
        step.start(userId);
        workflow.setNextStep(findNextPendingStepId(workflowId));
        workflow.setWhoHasDone(userId);
        workflowRepository.save(workflow);

        log.info("Restarted step {} of workflow {} by user {}", stepId, workflowId, userId);
        return step;
    }

    /**
     * Block a step. IN_PROGRESS -> BLOCKED.
     */
    @Transactional
    public WorkflowStep blockStep(UUID workflowId, UUID stepId, String reason) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, stepId, WorkflowStateMachines.TRANSITION_BLOCK,
            workflowStepStateStore);
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

        transition(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, stepId, WorkflowStateMachines.TRANSITION_UNBLOCK,
            workflowStepStateStore);
        step.unblock();
        workflow.setNextStep(findNextPendingStepId(workflowId));
        workflowRepository.save(workflow);

        log.info("Unblocked step {} of workflow {}", stepId, workflowId);
        return step;
    }

    /**
     * Add a note to a workflow, optionally scoped to a specific step.
     */
    @Transactional
    public WorkflowNote addNote(UUID workflowId, UUID stepId, UUID authorId, String note,
            WorkflowNote.NoteType type) {
        Workflow workflow = getById(workflowId);
        WorkflowStep step = stepId != null ? findStepById(workflowId, stepId) : null;
        WorkflowNote created = new WorkflowNote(workflow, step, authorId, note, type);
        WorkflowNote saved = workflowNoteRepository.save(created);
        log.info("Added {} note to workflow {}{}", type, workflowId,
            stepId != null ? " step " + stepId : "");
        return saved;
    }

    /**
     * Record a photo/document attachment on a step. The file bytes are uploaded directly from the
     * browser to object storage via document-service's presign endpoint; this only stores the
     * reference metadata.
     *
     * <p>
     * Idempotent by {@code id}: a client-generated id lets the offline upload queue safely retry
     * this call after a dropped connection without creating a duplicate row.
     */
    @Transactional
    public WorkflowStepAttachment addAttachment(UUID workflowId, UUID stepId, UUID id,
            UUID uploadedBy, WorkflowStepAttachment.AttachmentKind kind, String objectKey,
            String originalFileName, String contentType, Long byteSize) {
        Optional<WorkflowStepAttachment> existing = workflowStepAttachmentRepository.findById(id);
        if (existing.isPresent()) {
            return existing.get();
        }

        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        WorkflowStepAttachment attachment = new WorkflowStepAttachment(id, workflow, step,
            uploadedBy, kind, objectKey, originalFileName, contentType, byteSize);
        WorkflowStepAttachment saved = workflowStepAttachmentRepository.save(attachment);
        log.info("Recorded {} attachment {} on step {} of workflow {}", kind, saved.getId(), stepId, workflowId);
        return saved;
    }

    /**
     * Delete a photo/document attachment from a step. Returns the deleted attachment's object
     * key so the caller (the frontend proxy) can also remove the underlying object from storage
     * via document-service -- this service has no direct dependency on document-service.
     */
    @Transactional
    public String deleteAttachment(UUID workflowId, UUID stepId, UUID attachmentId) {
        WorkflowStepAttachment attachment = workflowStepAttachmentRepository
            .findByIdAndWorkflowIdAndStepId(attachmentId, workflowId, stepId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Attachment " + attachmentId + " not found on step " + stepId + " of workflow " + workflowId));

        String objectKey = attachment.getObjectKey();
        workflowStepAttachmentRepository.delete(attachment);
        log.info("Deleted attachment {} from step {} of workflow {}", attachmentId, stepId, workflowId);
        return objectKey;
    }

    // ── Helpers ──

    /**
     * Assign a user to a workflow step by email.
     * Resolves the user via the identity service and stores userId, email, and name.
     */
    @Transactional
    public WorkflowStep assignStep(UUID workflowId, UUID stepId, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        Workflow workflow = getById(workflowId);
        WorkflowStep step = findStepById(workflowId, stepId);

        // Resolve user via identity service
        IdentityPermissionClient.UserByIdEmail user = identityPermissionClient.resolveUserByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found for email: " + email);
        }

        step.setAssigneeUserId(user.userId());
        step.setAssigneeEmail(user.email());
        step.setAssigneeName(user.name());
        return workflowStepRepository.save(step);
    }

    /**
     * List workflows where a user is the creator.
     */
    public List<Workflow> listByAccessCreated(String userId) {
        String tenantId = getTenantIdFromContext();
        if (tenantId == null || userId == null) {
            return List.of();
        }
        return workflowRepository.findByTenantIdAndCreatedBy(tenantId, userId);
    }

    /**
     * List workflows where a user has at least one assigned step.
     */
    @Transactional(readOnly = true)
    public List<Workflow> listByAccessAssigned(String userId) {
        String tenantId = getTenantIdFromContext();
        if (tenantId == null) {
            return List.of();
        }

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        List<WorkflowStep> ws = workflowStepRepository.findByAssigneeUserId(userUuid);
        Set<Workflow> workflows = new HashSet<>();
        ws.forEach(step -> workflows.add(step.getWorkflow()));

        return workflows.stream()
            .filter(w -> tenantId.equals(w.getTenantId()))
            .toList();
    }

    /**
     * Extract the tenant ID from TenantContext.
     */
    private String getTenantIdFromContext() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId.toString() : null;
    }

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
