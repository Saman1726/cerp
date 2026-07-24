package io.cerp.workflow.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.domain.WorkflowNote;
import io.cerp.workflow.domain.WorkflowStep;
import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.repository.WorkflowNoteRepository;
import io.cerp.workflow.service.WorkflowService;

/**
 * REST API for Workflow instances.
 *
 * <p>
 * Endpoints per Section 23:
 * <ul>
 * <li>POST /api/v1/workflows - instantiate from template</li>
 * <li>GET /api/v1/workflows - list</li>
 * <li>GET /api/v1/workflows/{id} - get by ID</li>
 * <li>PATCH /api/v1/workflows/{id} - update dates/description</li>
 * <li>POST /api/v1/workflows/{id}/start</li>
 * <li>POST /api/v1/workflows/{id}/block</li>
 * <li>POST /api/v1/workflows/{id}/unblock</li>
 * <li>POST /api/v1/workflows/{id}/complete</li>
 * <li>POST /api/v1/workflows/{id}/approve</li>
 * <li>POST /api/v1/workflows/{id}/reject</li>
 * <li>POST /api/v1/workflows/{id}/cancel</li>
 * <li>POST /api/v1/workflows/{id}/notes</li>
 * <li>POST /api/v1/workflows/{id}/steps/{stepId}/start</li>
 * <li>POST /api/v1/workflows/{id}/steps/{stepId}/complete</li>
 * <li>POST /api/v1/workflows/{id}/steps/{stepId}/approve</li>
 * <li>POST /api/v1/workflows/{id}/steps/{stepId}/reject</li>
 * <li>POST /api/v1/workflows/{id}/steps/{stepId}/block</li>
 * <li>POST /api/v1/workflows/{id}/steps/{stepId}/unblock</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowNoteRepository workflowNoteRepository;

    public WorkflowController(WorkflowService workflowService, WorkflowNoteRepository workflowNoteRepository) {
        this.workflowService = workflowService;
        this.workflowNoteRepository = workflowNoteRepository;
    }

    // ── Workflow CRUD ──

    /**
     * Create a new workflow instance from a template.
     *
     * <p>
     * This is the primary POST /api/workflows endpoint. It instantiates a workflow from a template for a given project/sub-project.
     * </p>
     *
     * @param request
     *            the instantiation request containing tenantId, projectId, subprojectId, templateId, and userId
     * @return the created Workflow instance
     */
    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@Valid @RequestBody InstantiateRequest request) {
        Workflow created = workflowService.instantiate(
            request.tenantId(), request.projectId(),
            request.subprojectId(), request.templateId(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Alias for createWorkflow - instantiate a workflow from template.
     *
     * @param request
     *            the instantiation request
     * @return the created Workflow instance
     */
    @PostMapping("/instantiate")
    public ResponseEntity<Workflow> instantiate(@Valid @RequestBody InstantiateRequest request) {
        return createWorkflow(request);
    }

    @GetMapping
    public ResponseEntity<List<Workflow>> list(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID subprojectId) {
        String tenantId = getTenantIdFromContext();
        if (subprojectId != null && projectId != null) {
            return ResponseEntity.ok(workflowService.listBySubproject(tenantId, projectId, subprojectId));
        }
        if (subprojectId != null) {
            return ResponseEntity.ok(workflowService.getWorkflowsBySubprojectId(tenantId, subprojectId));
        }
        if (projectId != null) {
            return ResponseEntity.ok(workflowService.listByProject(tenantId, projectId));
        }
        return ResponseEntity.ok(List.of());
    }

    /**
     * Get workflows for a subproject with steps. Returns: { workflows: [...], steps: [...] }
     *
     * <p>
     * Falls back to querying without tenant filter when TenantContext is empty (e.g., development, migration, or when X-Tenant-Id header is not provided).
     * </p>
     */
    @Transactional(readOnly = true)
    @GetMapping("/subprojects/{subprojectId}")
    public ResponseEntity<Map<String, Object>> getWorkflowsBySubproject(
            @PathVariable String subprojectId) {
        String tenantId = getTenantIdFromContext();
        UUID subprojectUuid = UUID.fromString(subprojectId);

        List<Workflow> workflows;
        if (tenantId != null && !tenantId.isEmpty()) {
            workflows = workflowService.getWorkflowsBySubprojectId(tenantId, subprojectUuid);
        } else {
            // Fall back to querying by subprojectId only (no tenant filter)
            workflows = workflowService.getWorkflowsBySubprojectIdWithoutTenant(subprojectUuid);
        }

        // Eagerly load steps for each workflow and replace with the loaded entity
        workflows = workflows.stream()
            .map(w -> workflowService.getWorkflowWithSteps(w.getId()).orElse(w))
            .toList();

        // Collect all steps for all workflows into a flat list
        List<WorkflowStep> allSteps = workflows.stream()
            .flatMap(w -> w.getSteps().stream())
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("workflows", workflows);
        response.put("steps", allSteps);

        return ResponseEntity.ok(response);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getById(@PathVariable String id) {
        Workflow workflow = workflowService.getById(UUID.fromString(id));
        // Collect all steps for all workflows
        Optional<Workflow> withSteps = workflowService.getWorkflowWithSteps(workflow.getId());

        return ResponseEntity.ok(withSteps.get());
    }

    /**
     * Get workflow detail with steps and notes. Returns: { workflow: {...}, steps: [...], notes: [...] }
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<Map<String, Object>> getWorkflowDetail(@PathVariable String id) {
        Workflow workflow = workflowService.getById(UUID.fromString(id));
        Optional<Workflow> withSteps = workflowService.getWorkflowWithSteps(workflow.getId());

        List<WorkflowStep> steps = withSteps.map(Workflow::getSteps).orElse(List.of());
        List<WorkflowNote> workflowNotes = withSteps
            .map(w -> workflowNoteRepository.findByWorkflowIdOrderByCreatedAtDesc(w.getId()))
            .orElse(List.of());

        Map<String, Object> response = new HashMap<>();
        response.put("workflow", workflow);
        response.put("steps", steps);
        response.put("notes", workflowNotes);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Workflow> updateDates(@PathVariable String id,
            @Valid @RequestBody UpdateWorkflowRequest request) {
        Workflow workflow = workflowService.getById(UUID.fromString(id));
        if (request.startDate() != null) {
            workflow.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            workflow.setDueDate(request.dueDate());
        }
        if (request.description() != null) {
            workflow.setDescription(request.description());
        }
        return ResponseEntity.ok(workflow);
    }

    // ── Step listing ──

    @GetMapping("/{workflowId}/steps")
    public ResponseEntity<List<WorkflowStep>> getSteps(@PathVariable String workflowId) {
        Optional<Workflow> workflowOpt = workflowService.getWorkflowWithSteps(UUID.fromString(workflowId));
        List<WorkflowStep> steps = workflowOpt.map(Workflow::getSteps).orElse(List.of());
        return ResponseEntity.ok(steps);
    }

    // ── Workflow Status Transitions ──

    @PostMapping("/{id}/start")
    public ResponseEntity<Workflow> start(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.startWorkflow(UUID.fromString(id)));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<Workflow> block(@PathVariable String id,
            @Valid @RequestBody StatusReasonRequest request) {
        return ResponseEntity.ok(workflowService.blockWorkflow(UUID.fromString(id), request.reason()));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<Workflow> unblock(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.unblockWorkflow(UUID.fromString(id)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Workflow> complete(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.completeWorkflow(UUID.fromString(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Workflow> approve(@PathVariable String id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(workflowService.approveWorkflow(UUID.fromString(id), request.userId()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Workflow> reject(@PathVariable String id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(workflowService.rejectWorkflow(UUID.fromString(id), request.userId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Workflow> cancel(@PathVariable String id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(workflowService.cancelWorkflow(UUID.fromString(id), request.userId()));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Workflow> pause(@PathVariable String id,
            @Valid @RequestBody StatusReasonRequest request) {
        return ResponseEntity.ok(workflowService.blockWorkflow(UUID.fromString(id), request.reason()));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Workflow> resume(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.unblockWorkflow(UUID.fromString(id)));
    }

    // ── Step Operations ──

    @PostMapping("/{workflowId}/steps/{stepId}/start")
    public ResponseEntity<WorkflowStep> startStep(@PathVariable String workflowId,
            @PathVariable String stepId, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(workflowService.startStep(
            UUID.fromString(workflowId), UUID.fromString(stepId), request.userId()));
    }

    @PostMapping("/{workflowId}/steps/{stepId}/complete")
    public ResponseEntity<WorkflowStep> completeStep(@PathVariable String workflowId,
            @PathVariable String stepId, @Valid @RequestBody CompleteStepRequest request) {
        return ResponseEntity.ok(workflowService.completeStep(
            UUID.fromString(workflowId), UUID.fromString(stepId),
            request.evidence(), request.notes(), request.userId()));
    }

    @PostMapping("/{workflowId}/steps/{stepId}/approve")
    public ResponseEntity<WorkflowStep> approveStep(@PathVariable String workflowId,
            @PathVariable String stepId, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(workflowService.approveStep(
            UUID.fromString(workflowId), UUID.fromString(stepId),
            request.notes(), request.userId()));
    }

    @PostMapping("/{workflowId}/steps/{stepId}/reject")
    public ResponseEntity<WorkflowStep> rejectStep(@PathVariable String workflowId,
            @PathVariable String stepId, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(workflowService.rejectStep(
            UUID.fromString(workflowId), UUID.fromString(stepId),
            request.notes(), request.userId()));
    }

    @PostMapping("/{workflowId}/steps/{stepId}/block")
    public ResponseEntity<WorkflowStep> blockStep(@PathVariable String workflowId,
            @PathVariable String stepId, @Valid @RequestBody StatusReasonRequest request) {
        return ResponseEntity.ok(workflowService.blockStep(
            UUID.fromString(workflowId), UUID.fromString(stepId), request.reason()));
    }

    @PostMapping("/{workflowId}/steps/{stepId}/unblock")
    public ResponseEntity<WorkflowStep> unblockStep(@PathVariable String workflowId,
            @PathVariable String stepId) {
        return ResponseEntity.ok(workflowService.unblockStep(
            UUID.fromString(workflowId), UUID.fromString(stepId)));
    }

    // ── Notes ──

    @GetMapping("/{workflowId}/notes")
    public ResponseEntity<List<WorkflowNote>> getNotes(@PathVariable String workflowId) {
        List<WorkflowNote> workflowNotes = workflowNoteRepository
            .findByWorkflowIdOrderByCreatedAtDesc(UUID.fromString(workflowId));
        return ResponseEntity.ok(workflowNotes);
    }

    @PostMapping("/{workflowId}/notes")
    public ResponseEntity<WorkflowNote> addNote(@PathVariable String workflowId,
            @Valid @RequestBody AddNoteRequest request) {
        // Notes will be implemented in next iteration
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    // ── Helpers ──

    /**
     * Extract the tenant ID from TenantContext.
     *
     * @return the tenant ID string if set, null otherwise
     */
    private String getTenantIdFromContext() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId.toString() : null;
    }

    // ── DTOs ──

    public record InstantiateRequest(
            @NotBlank String tenantId,
            UUID projectId,
            UUID subprojectId,
            UUID templateId,
            UUID userId) {
    }

    public record UpdateWorkflowRequest(
            LocalDate startDate,
            LocalDate dueDate,
            String description) {
    }

    public record StatusReasonRequest(
            @NotBlank String reason) {
    }

    public record UserRequest(
            @NotEmpty UUID userId,
            String notes) {
    }

    public record CompleteStepRequest(
            @NotEmpty UUID userId,
            String evidence,
            String notes) {
    }

    public record AddNoteRequest(
            @NotEmpty UUID userId,
            UUID stepId,
            @NotBlank String note,
            @NotBlank String type) {
    }
}
