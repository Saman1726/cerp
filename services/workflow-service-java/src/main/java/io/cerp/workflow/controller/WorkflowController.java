package io.cerp.workflow.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
import io.cerp.workflow.service.WorkflowService;

/**
 * REST API for Workflow instances.
 *
 * <p>
 * Endpoints per Section 23:
 * <ul>
 * <li>POST /api/v1/workflows – instantiate from template</li>
 * <li>GET /api/v1/workflows – list</li>
 * <li>GET /api/v1/workflows/{id} – get by ID</li>
 * <li>PATCH /api/v1/workflows/{id} – update dates/description</li>
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

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
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
        // Implementation will be enhanced with tenant context
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getById(@PathVariable String id) {
        Workflow workflow = workflowService.getById(UUID.fromString(id));
        return ResponseEntity.ok(workflow);
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

    @PostMapping("/{workflowId}/notes")
    public ResponseEntity<WorkflowNote> addNote(@PathVariable String workflowId,
            @Valid @RequestBody AddNoteRequest request) {
        // Notes will be implemented in next iteration
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
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
