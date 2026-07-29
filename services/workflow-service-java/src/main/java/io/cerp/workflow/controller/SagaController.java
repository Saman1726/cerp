package io.cerp.workflow.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.saga.DefaultSagaOrchestrator;
import io.cerp.workflow.saga.SagaContext;
import io.cerp.workflow.saga.SagaError;
import io.cerp.workflow.saga.SagaException;
import io.cerp.workflow.saga.SagaState;

/**
 * REST API for Saga orchestration.
 *
 * <p>
 * Endpoints for the saga-based workflow orchestration system:
 * <ul>
 * <li>POST /api/v1/sagas - Start a new saga</li>
 * <li>GET /api/v1/sagas/{sagaId} - Get saga state</li>
 * <li>POST /api/v1/sagas/{sagaId}/steps - Execute a saga step</li>
 * <li>POST /api/v1/sagas/{sagaId}/compensate - Run compensation</li>
 * <li>GET /api/v1/sagas - Query sagas by aggregate</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/sagas")
public class SagaController {

    private final DefaultSagaOrchestrator orchestrator;

    public SagaController(DefaultSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Start a new saga instance.
     *
     * <p>
     * Creates a new saga with the given saga type and initial context data.
     * </p>
     *
     * @param request
     *            the saga start request containing sagaType and initial data
     * @return the created saga with its sagaId
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> startSaga(@RequestBody StartSagaRequest request) {
        UUID sagaId = request.sagaId() != null ? request.sagaId() : UUID.randomUUID();

        Map<String, Object> initialData = request.data() != null ? request.data() : new HashMap<>();
        initialData.put("sagaId", sagaId.toString());
        initialData.put("sagaType", request.sagaType());

        SagaContext context = new SagaContext(
            sagaId,
            request.sagaType(),
            getTenantIdFromContext(),
            initialData);

        UUID startedId = orchestrator.startSaga(request.sagaType(), context);

        Map<String, Object> response = new HashMap<>();
        response.put("sagaId", startedId);
        response.put("sagaType", request.sagaType());
        response.put("status", "RUNNING");
        response.put("createdAt", java.time.Instant.now().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get the current state of a saga.
     *
     * @param sagaId
     *            the saga identifier
     * @return the saga state including current step, status, and steps history
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaState> getSagaState(@PathVariable String sagaId) {
        SagaState state = orchestrator.getSagaState(UUID.fromString(sagaId));
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * Execute the next step in a saga.
     *
     * <p>
     * Executes the specified command through the appropriate saga handler. On success, the saga advances to the next step. On failure, compensating transactions may be
     * triggered.
     * </p>
     *
     * @param sagaId
     *            the saga identifier
     * @param request
     *            the step execution request containing command and optional payload
     * @return the updated saga context
     */
    @PostMapping("/{sagaId}/steps")
    public ResponseEntity<Map<String, Object>> executeStep(
            @PathVariable String sagaId,
            @RequestBody ExecuteStepRequest request) {
        try {
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                request.command(),
                request.payload());

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("command", request.command());
            response.put("status", "SUCCESS");
            response.put("context", context.getData());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "SAGA_COMPLETED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "EXECUTION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Run compensation for a specific step in a saga.
     *
     * <p>
     * Compensates the specified step by executing its compensating transaction. This is typically used after a saga failure to roll back completed steps.
     * </p>
     *
     * @param sagaId
     *            the saga identifier
     * @param request
     *            the compensation request containing the step code to compensate
     * @return the compensated saga context
     */
    @PostMapping("/{sagaId}/compensate")
    public ResponseEntity<Map<String, Object>> compensateStep(
            @PathVariable String sagaId,
            @RequestBody CompensateStepRequest request) {
        try {
            SagaContext context = orchestrator.compensateStep(
                UUID.fromString(sagaId),
                request.stepCode());

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("compensatedStep", request.stepCode());
            response.put("status", "COMPENSATED");
            response.put("context", context.getData());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "COMPENSATION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Query saga instances by aggregate type and ID.
     *
     * @param aggregateType
     *            the aggregate type (e.g., "ProcurementRequest", "Delivery")
     * @param aggregateId
     *            the aggregate identifier
     * @return list of saga instances associated with the aggregate
     */
    @GetMapping(params = "aggregateId")
    public ResponseEntity<List<SagaState>> getSagasByAggregate(
            @RequestParam String aggregateType,
            @RequestParam String aggregateId) {
        UUID uuid = UUID.fromString(aggregateId);
        List<SagaState> sagas = orchestrator.getSagaInstancesByAggregate(aggregateType, uuid);
        return ResponseEntity.ok(sagas);
    }

    /**
     * List all sagas with filtering and pagination (frontend-compatible endpoint).
     *
     * @param sagaType
     *            optional saga type filter
     * @param status
     *            optional status filter
     * @param tenantId
     *            optional tenant ID filter
     * @param page
     *            page number (default 1)
     * @param pageSize
     *            page size (default 25)
     * @param sortBy
     *            sort field (createdAt, updatedAt, status)
     * @param sortOrder
     *            sort direction (asc, desc)
     * @return paginated saga list with metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listSagas(
            @RequestParam(required = false) String sagaType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        // Get all sagas from the orchestrator and filter in memory
        List<SagaState> allSagas = new ArrayList<>(orchestrator.getActiveSagas());

        // Apply filters
        List<SagaState> filtered = allSagas.stream()
            .filter(s -> sagaType == null || sagaType.isEmpty() || sagaType.equals(s.getSagaType()))
            .filter(s -> status == null || status.isEmpty() || status.equals(s.getStatus()))
            .filter(s -> tenantId == null || tenantId.isEmpty() ||
                    (s.getTenantId() != null && s.getTenantId().toString().equals(tenantId)))
            .collect(Collectors.toList());

        // Apply sorting
        filtered.sort((a, b) -> {
            int cmp = 0;
            switch (sortBy.toLowerCase()) {
                case "createdAt":
                    cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                    break;
                case "updatedAt":
                    cmp = a.getUpdatedAt().compareTo(b.getUpdatedAt());
                    break;
                case "status":
                    cmp = a.getStatus().compareTo(b.getStatus());
                    break;
                default:
                    cmp = 0;
            }
            return "asc".equals(sortOrder) ? cmp : -cmp;
        });

        // Apply pagination
        int total = filtered.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<SagaState> paged = fromIndex < filtered.size() ? filtered.subList(fromIndex, toIndex) : List.of();

        Map<String, Object> response = new HashMap<>();
        response.put("items", paged);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all events for a saga (frontend-compatible endpoint). Currently returns an empty list as events are not yet stored persistently.
     *
     * @param sagaId
     *            the saga identifier
     * @return list of saga events
     */
    @GetMapping("/{sagaId}/events")
    public ResponseEntity<List<Map<String, String>>> getSagaEvents(@PathVariable String sagaId) {
        // Events are published via Kafka but not stored persistently yet
        return ResponseEntity.ok(List.of());
    }

    /**
     * Execute the next step in a saga (frontend-compatible endpoint).
     *
     * @param sagaId
     *            the saga identifier
     * @param request
     *            the execution request containing command and optional data
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/execute")
    public ResponseEntity<Map<String, Object>> executeStepFrontend(
            @PathVariable String sagaId,
            @RequestBody ExecuteStepFrontendRequest request) {
        try {
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                request.command(),
                request.data());

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("command", request.command());
            response.put("status", "SUCCESS");
            response.put("context", context.getData());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "EXECUTION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Perform a generic workflow action on a saga (frontend-compatible endpoint).
     *
     * @param sagaId
     *            the saga identifier
     * @param request
     *            the action request
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/action")
    public ResponseEntity<Map<String, Object>> performActionFrontend(
            @PathVariable String sagaId,
            @RequestBody ActionRequest request) {
        try {
            switch (request.action()) {
                case "resume":
                    return resumeSagaFrontend(sagaId, null);
                case "escalate":
                    return escalateSagaFrontend(sagaId, request.reason());
                case "complete_step":
                    return completeStepFrontend(sagaId, request.stepCode());
                case "insert_step":
                case "remove_step":
                    Map<String, Object> unsupported = new HashMap<>();
                    unsupported.put("error", "UNSUPPORTED");
                    unsupported.put("message", request.action() + " is not yet supported");
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(unsupported);
                default:
                    Map<String, Object> unknown = new HashMap<>();
                    unknown.put("error", "UNKNOWN_ACTION");
                    unknown.put("message", "Unknown action: " + request.action());
                    return ResponseEntity.badRequest().body(unknown);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "ACTION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Resume a paused saga (frontend-compatible endpoint).
     *
     * @param sagaId
     *            the saga identifier
     * @param stepCode
     *            optional step code to resume from
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/resume")
    public ResponseEntity<Map<String, Object>> resumeSagaFrontend(
            @PathVariable String sagaId,
            @RequestParam(required = false) String stepCode) {
        try {
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                "RESUME",
                java.util.Map.of("stepCode", stepCode));

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("action", "resume");
            response.put("status", "RUNNING");
            response.put("context", context.getData());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "RESUME_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Escalate a saga step (frontend-compatible endpoint).
     *
     * @param sagaId
     *            the saga identifier
     * @param reason
     *            optional escalation reason
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/escalate")
    public ResponseEntity<Map<String, Object>> escalateSagaFrontend(
            @PathVariable String sagaId,
            @RequestParam(required = false) String reason) {
        try {
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                "ESCALATE",
                java.util.Map.of("reason", reason));

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("action", "escalate");
            response.put("status", "ESCALATED");
            response.put("context", context.getData());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "ESCALATION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Complete a specific step in a saga.
     *
     * @param sagaId
     *            the saga identifier
     * @param stepCode
     *            the step code to complete
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/complete_step")
    public ResponseEntity<Map<String, Object>> completeStepFrontend(
            @PathVariable String sagaId,
            @RequestParam(required = false) String stepCode) {
        try {
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                "COMPLETE_STEP",
                java.util.Map.of("stepCode", stepCode));

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("action", "complete_step");
            response.put("status", "SUCCESS");
            response.put("context", context.getData());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "COMPLETE_STEP_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ── DTOs ──

    /**
     * Request body for starting a new saga.
     */
    public record StartSagaRequest(
            String sagaType,
            UUID sagaId,
            Map<String, Object> data) {
    }

    /**
     * Request body for executing a saga step.
     */
    public record ExecuteStepRequest(
            String command,
            Map<String, Object> payload) {
    }

    /**
     * Request body for compensating a saga step.
     */
    public record CompensateStepRequest(
            String stepCode) {
    }

    /**
     * Request body for executing a saga step via frontend-compatible endpoint.
     */
    public record ExecuteStepFrontendRequest(
            String command,
            Map<String, Object> data) {
    }

    /**
     * Request body for performing a generic workflow action.
     */
    public record ActionRequest(
            String action,
            String stepCode,
            String reason,
            Map<String, Object> data) {
    }

    /**
     * Extract the tenant ID from TenantContext.
     */
    private String getTenantIdFromContext() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId.toString() : null;
    }
}
