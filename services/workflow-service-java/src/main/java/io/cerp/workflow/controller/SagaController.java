package io.cerp.workflow.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.repository.SagaAttachmentEntity;
import io.cerp.workflow.repository.SagaEntity;
import io.cerp.workflow.repository.SagaNoteEntity;
import io.cerp.workflow.repository.SagaRepository;
import io.cerp.workflow.saga.DefaultSagaOrchestrator;
import io.cerp.workflow.saga.SagaContext;
import io.cerp.workflow.saga.SagaError;
import io.cerp.workflow.saga.SagaException;
import io.cerp.workflow.saga.SagaState;
import io.cerp.workflow.security.IdentityPermissionClient;

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
    private final IdentityPermissionClient permissionClient;
    private final SagaRepository sagaRepository;

    public SagaController(DefaultSagaOrchestrator orchestrator, IdentityPermissionClient permissionClient, SagaRepository sagaRepository) {
        this.orchestrator = orchestrator;
        this.permissionClient = permissionClient;
        this.sagaRepository = sagaRepository;
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
    /**
     * Start a new saga instance.
     * Access control: User must have write permission on at least one project, or be tenant admin/manager.
     * If projectId is provided in data, user must have write permission on that specific project.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> startSaga(
            @RequestBody StartSagaRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            @RequestParam(value = "partyIds", required = false) String partyIds) {
        // Extract userId from request data for access control
        String userIdStr = null;
        if (request.data() != null) {
            userIdStr = (String) request.data().get("userId");
        }

        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                UUID userUuid = UUID.fromString(userIdStr);

                // Check if user is tenant admin/manager — trust header roles first.
                boolean isAdminOrManager = hasAdminOrManagerRole(userRoles)
                        || permissionClient.isTenantAdminOrManager(userUuid);

                if (!isAdminOrManager) {
                    // Check if user has any project write permission
                    boolean hasAnyWrite = permissionClient.hasAnyProjectWritePermission(userUuid);

                    if (!hasAnyWrite) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "FORBIDDEN");
                        error.put("message", "User does not have write permission on any project");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
                    }

                    // If projectId is provided, check specific project permission
                    if (request.data() != null) {
                        Object projectIdObj = request.data().get("projectId");
                        if (projectIdObj != null) {
                            try {
                                UUID projectId = UUID.fromString(projectIdObj.toString());
                                if (!permissionClient.canWriteOnProject(userUuid, projectId)) {
                                    Map<String, Object> error = new HashMap<>();
                                    error.put("error", "FORBIDDEN");
                                    error.put("message", "User does not have write permission on the specified project");
                                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
                                }
                            } catch (IllegalArgumentException e) {
                                // ignore invalid UUID
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If userId is invalid, proceed (fail open)
            }
        }

        UUID sagaId = request.sagaId() != null ? request.sagaId() : UUID.randomUUID();

        Map<String, Object> initialData = request.data() != null ? request.data() : new HashMap<>();
        initialData.put("sagaId", sagaId.toString());
        initialData.put("sagaType", request.sagaType());

        SagaContext context = new SagaContext(
            sagaId,
            request.sagaType(),
            getTenantIdFromContext(),
            initialData);

        // Set createdBy and assignedTo on the context so they are in the initial in-memory cache
        if (createdBy != null && !createdBy.isEmpty()) {
            context.setCreatedBy(createdBy);
        } else if (userIdStr != null && !userIdStr.isEmpty()) {
            context.setCreatedBy(userIdStr);
        }
        if (assignedTo != null && !assignedTo.isEmpty()) {
            context.setAssignedTo(assignedTo);
        }

        UUID startedId = orchestrator.startSaga(request.sagaType(), context);

        SagaState initialState = orchestrator.getSagaState(startedId);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("sagaId", startedId);
        responseData.put("sagaType", request.sagaType());
        // If the saga was returned from idempotency check, use its actual status
        String sagaStatus = "RUNNING";
        if (initialState != null) {
            sagaStatus = initialState.getStatus();
            responseData.put("data", initialState.getData());
        }
        responseData.put("status", sagaStatus);
        responseData.put("createdAt", java.time.Instant.now().toString());

        // Update the saga entity with creator, assignee, and party access
        if (initialState != null) {
            String sagaEntityId = startedId.toString();
            Optional<SagaEntity> sagaOpt = sagaRepository.findById(sagaEntityId);
            if (sagaOpt.isPresent()) {
                SagaEntity sagaEntity = sagaOpt.get();
                if (createdBy != null && !createdBy.isEmpty()) {
                    sagaEntity.setCreatedBy(createdBy);
                } else if (userIdStr != null && !userIdStr.isEmpty()) {
                    sagaEntity.setCreatedBy(userIdStr);
                }
                if (assignedTo != null && !assignedTo.isEmpty()) {
                    sagaEntity.setAssignedTo(assignedTo);
                }
                if (partyIds != null && !partyIds.isEmpty()) {
                    sagaEntity.setPartyIds(partyIds);
                }
                sagaEntity.setUpdatedAt(LocalDateTime.now());
                sagaRepository.save(sagaEntity);
            }
        }

        // Return 200 OK if saga already existed (idempotent), 201 CREATED if new
        HttpStatus httpStatus = sagaStatus.equals("RUNNING") && initialState != null && 
            !initialState.getCreatedAt().equals(java.time.Instant.now()) ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(httpStatus).body(responseData);
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
     * Update an existing saga's data without changing its state.
     *
     * <p>
     * This endpoint allows updating the data payload of an existing saga (e.g., editing procurement request details)
     * while preserving the saga's current state, steps, and history.
     * </p>
     *
     * @param sagaId
     *            the saga identifier
     * @param request
     *            the update request containing new data fields
     * @return the updated saga state
     */
    @PutMapping("/{sagaId}")
    public ResponseEntity<Map<String, Object>> updateSaga(
            @PathVariable String sagaId,
            @RequestBody Map<String, Object> data) {
        try {
            SagaState state = orchestrator.getSagaState(UUID.fromString(sagaId));
            if (state == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "NOT_FOUND");
                error.put("message", "Saga not found: " + sagaId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Check if saga is in a cancellable state (RUNNING or REQUIRES_MANUAL_INTERVENTION)
            if (!"RUNNING".equals(state.getStatus()) && !"REQUIRES_MANUAL_INTERVENTION".equals(state.getStatus())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "INVALID_STATE");
                error.put("message", "Saga cannot be updated in current state: " + state.getStatus());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Update the saga data by merging the new fields
            SagaContext updatedContext = orchestrator.updateSagaData(
                UUID.fromString(sagaId),
                data);

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("updated", true);
            response.put("context", updatedContext.getData());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "UPDATE_FAILED");
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
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String accessMode,
            @RequestParam(required = false) String vendorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extract userId from JWT if not passed as query param
        String effectiveUserId = extractUserIdFromJwt(authHeader, userId);

        List<SagaState> allSagas = new ArrayList<>();

        // Apply role-based access control using DB queries (not in-memory filtering)
        if (effectiveUserId != null && !effectiveUserId.isEmpty()) {
            try {
                UUID userUuid = UUID.fromString(effectiveUserId);

                // Check if user is tenant admin/manager — trust header roles first,
                // fall back to identity service for requests without role headers.
                boolean isAdminOrManager = hasAdminOrManagerRole(userRoles)
                        || permissionClient.isTenantAdminOrManager(userUuid);

                // if (!isAdminOrManager) {
                    // Get projects the user has access to
                    List<UUID> accessibleProjectIds = permissionClient.getAccessibleProjects(userUuid);
                    List<String> projectIdStrs = accessibleProjectIds.stream()
                        .map(UUID::toString)
                        .toList();

                    if (vendorId != null && !vendorId.isEmpty()) {
                        // No project-based filtering for vendor-specific requests
                        allSagas = new ArrayList<>(orchestrator.getActiveSagas());
                    } else if (accessMode == null || accessMode.isEmpty()) {
                        // Default: combined access using DB queries
                        allSagas = orchestrator.listSagasByAccess(effectiveUserId, null, projectIdStrs);
                    } else if ("created".equals(accessMode)) {
                        // DB query: sagas created by this user
                        allSagas = orchestrator.listSagasByAccess(effectiveUserId, "created", projectIdStrs);
                    } else if ("assigned".equals(accessMode)) {
                        // DB query: sagas assigned to this user
                        allSagas = orchestrator.listSagasByAccess(effectiveUserId, "assigned", projectIdStrs);
                    } else if ("member".equals(accessMode)) {
                        // DB query: sagas where user is creator OR assignee
                        allSagas = orchestrator.listSagasByAccess(effectiveUserId, "member", projectIdStrs);
                    } else if ("project".equals(accessMode) && projectIdStrs != null && !projectIdStrs.isEmpty()) {
                        // DB query: sagas belonging to accessible projects
                        allSagas = orchestrator.listSagasByAccess(effectiveUserId, "project", projectIdStrs);
                    } else {
                        // Default: combined access via DB queries
                        allSagas = orchestrator.listSagasByAccess(effectiveUserId, null, projectIdStrs);
                    }
                // } else {
                //     // Admin/manager — show all active sagas (no project-based filtering)
                //     allSagas = new ArrayList<>(orchestrator.getActiveSagas());
                // }
            } catch (Exception e) {
                // If userId is invalid, skip access control (fail open)
            }
        }

        // Apply vendorId filter if provided
        if (vendorId != null && !vendorId.isEmpty()) {
            final String vendorFilter = vendorId;
            allSagas = allSagas.stream()
                .filter(s -> {
                    Object vendorIdObj = s.getData().get("vendorId");
                    return vendorIdObj != null && vendorIdObj.toString().equals(vendorFilter);
                })
                .collect(Collectors.toList());
        }

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
                    return resumeSagaFrontend(sagaId, null, null);
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
     * Pause a running saga (frontend-compatible endpoint).
     *
     * @param sagaId
     *            the saga identifier
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/pause")
    public ResponseEntity<Map<String, Object>> pauseSagaFrontend(@PathVariable String sagaId) {
        try {
            orchestrator.pauseSaga(UUID.fromString(sagaId));

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("action", "pause");
            response.put("status", "PAUSED");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "INVALID_STATE");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "PAUSE_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Resume a paused saga (transition from PAUSED to RUNNING).
     *
     * @param sagaId
     *            the saga identifier
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/start")
    public ResponseEntity<Map<String, Object>> startPausedSagaFrontend(@PathVariable String sagaId) {
        try {
            orchestrator.resumeSaga(UUID.fromString(sagaId));

            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("action", "resume");
            response.put("status", "RUNNING");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "INVALID_STATE");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "RESUME_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Resume a paused saga (frontend-compatible endpoint).
     *
     * <p>
     * Accepts an optional stepCode either from the request body or as a query parameter.
     * The frontend sends { action: 'resume', stepCode } in the request body.
     * This is for the legacy resume flow where the saga is RUNNING but waiting for manual intervention.
     * </p>
     *
     * @param sagaId
     *            the saga identifier
     * @param requestBody
     *            optional request body containing stepCode
     * @param stepCodeQueryParam
     *            optional step code from query parameter (for backward compatibility)
     * @return the updated saga state
     */
    @PostMapping("/{sagaId}/resume")
    public ResponseEntity<Map<String, Object>> resumeSagaFrontend(
            @PathVariable String sagaId,
            @RequestBody(required = false) ResumeRequest requestBody,
            @RequestParam(required = false) String stepCodeQueryParam) {
        // Prefer stepCode from request body, fall back to query parameter
        String stepCode = (requestBody != null && requestBody.stepCode() != null)
                ? requestBody.stepCode()
                : stepCodeQueryParam;
        try {
            Map<String, Object> resumePayload = new HashMap<>();
            resumePayload.put("stepCode", stepCode);
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                "RESUME",
                resumePayload);

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
            Map<String, Object> escalatePayload = new HashMap<>();
            escalatePayload.put("reason", reason);
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                "ESCALATE",
                escalatePayload);

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
            Map<String, Object> completeStepPayload = new HashMap<>();
            completeStepPayload.put("stepCode", stepCode);
            SagaContext context = orchestrator.executeStep(
                UUID.fromString(sagaId),
                "COMPLETE_STEP",
                completeStepPayload);

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

    /**
     * Archive every Bidding/Procurement saga (or any other saga type) tied to a Project - called
     * by project-service when a Project is deleted. Does not touch bidding.bid_wallets or
     * bidding.bid_transactions, which are per-vendor financial ledgers shared across every project
     * a vendor has ever bid on, not project-scoped.
     */
    @PostMapping("/by-project/{projectId}/archive")
    public ResponseEntity<Void> archiveByProject(@PathVariable String projectId) {
        orchestrator.archiveByProjectId(projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Count non-archived sagas tied to a Project - used by the frontend to preview what a Project
     * deletion will archive, before the user confirms.
     */
    @GetMapping("/count-by-project/{projectId}")
    public ResponseEntity<Long> countByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(orchestrator.countByProjectId(projectId));
    }

    // ── Notes & Attachments ──
    //
    // Saga-level only (no step scoping) - see SagaNoteEntity's Javadoc for why. The frontend
    // reaches these through apps/web/app/api/sagas/[...path]/route.ts's generic passthrough,
    // which has no field-translation layer, so these response shapes must already be
    // frontend-friendly (no renaming needed for GET, unlike the workflow-step equivalents).

    @GetMapping("/{sagaId}/notes")
    public ResponseEntity<List<SagaNoteEntity>> getSagaNotes(@PathVariable String sagaId) {
        return ResponseEntity.ok(orchestrator.listNotes(sagaId));
    }

    @PostMapping("/{sagaId}/notes")
    public ResponseEntity<SagaNoteEntity> addSagaNote(@PathVariable String sagaId,
            @RequestBody AddSagaNoteRequest request) {
        SagaNoteEntity created = orchestrator.addNote(sagaId, request.authorId(), request.note(),
            SagaNoteEntity.NoteType.valueOf(request.type().toUpperCase()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{sagaId}/attachments")
    public ResponseEntity<List<SagaAttachmentEntity>> getSagaAttachments(@PathVariable String sagaId) {
        return ResponseEntity.ok(orchestrator.listAttachments(sagaId));
    }

    @PostMapping("/{sagaId}/attachments")
    public ResponseEntity<SagaAttachmentEntity> addSagaAttachment(@PathVariable String sagaId,
            @RequestBody AddSagaAttachmentRequest request) {
        SagaAttachmentEntity created = orchestrator.addAttachment(sagaId, request.id(), request.uploadedBy(),
            SagaAttachmentEntity.AttachmentKind.valueOf(request.kind().toUpperCase()),
            request.objectKey(), request.originalFileName(), request.contentType(), request.byteSize());
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{sagaId}/attachments/{attachmentId}")
    public ResponseEntity<DeleteSagaAttachmentResponse> deleteSagaAttachment(
            @PathVariable String sagaId, @PathVariable String attachmentId) {
        String objectKey = orchestrator.deleteAttachment(sagaId, UUID.fromString(attachmentId));
        return ResponseEntity.ok(new DeleteSagaAttachmentResponse(objectKey));
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
     * Request body for updating an existing saga.
     */
    public record UpdateSagaRequest(
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
     * Request body for the resume endpoint.
     */
    public record ResumeRequest(
            String stepCode) {
    }

    public record AddSagaNoteRequest(
            UUID authorId,
            String note,
            String type) {
    }

    public record AddSagaAttachmentRequest(
            UUID id,
            UUID uploadedBy,
            String kind,
            String objectKey,
            String originalFileName,
            String contentType,
            long byteSize) {
    }

    public record DeleteSagaAttachmentResponse(String objectKey) {
    }

    /**
     * Extract the tenant ID from TenantContext.
     */
    private String getTenantIdFromContext() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId.toString() : null;
    }

    private boolean hasAdminOrManagerRole(String userRoles) {
        if (userRoles == null || userRoles.isBlank()) return false;
        for (String role : userRoles.split(",")) {
            String r = role.strip();
            if ("admin".equalsIgnoreCase(r) || "manager".equalsIgnoreCase(r)) return true;
        }
        return false;
    }

    /**
     * Extract the internal_user_id claim from the Authorization Bearer JWT token.
     * Falls back to the userId request param if JWT decoding fails or no token is present.
     */
    private String extractUserIdFromJwt(String authHeader, String fallbackUserId) {
        // Priority 1: JWT "internal_user_id" claim (Keycloak custom claim)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                    com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                    com.fasterxml.jackson.databind.JsonNode internalUserId = root.get("internal_user_id");
                    if (internalUserId != null && internalUserId.isTextual()) {
                        return internalUserId.asText();
                    }
                }
            } catch (Exception e) {
                // JWT decode failed -- fall through to next priority
            }
        }
        // Priority 2: fallback userId from query param / forwarded value
        if (fallbackUserId != null && !fallbackUserId.isBlank()) {
            return fallbackUserId;
        }
        return null;
    }
}
