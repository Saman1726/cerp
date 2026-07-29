package io.cerp.workflow.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    @GetMapping
    public ResponseEntity<List<SagaState>> getSagasByAggregate(
            @RequestParam String aggregateType,
            @RequestParam String aggregateId) {
        UUID uuid = UUID.fromString(aggregateId);
        List<SagaState> sagas = orchestrator.getSagaInstancesByAggregate(aggregateType, uuid);
        return ResponseEntity.ok(sagas);
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
     * Extract the tenant ID from TenantContext.
     */
    private String getTenantIdFromContext() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId.toString() : null;
    }
}
