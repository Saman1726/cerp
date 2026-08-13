package io.cerp.workflow.saga;

import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.seed.TenantSeedInitializer;
import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.repository.SagaAttachmentEntity;
import io.cerp.workflow.repository.SagaAttachmentRepository;
import io.cerp.workflow.repository.SagaEntity;
import io.cerp.workflow.repository.SagaNoteEntity;
import io.cerp.workflow.repository.SagaNoteRepository;
import io.cerp.workflow.repository.SagaRepository;
import io.cerp.workflow.service.EventPublisher;
import io.cerp.workflow.security.IdentityPermissionClient;
import io.cerp.workflow.security.IdentityPermissionClient.ResourceMembershipDto;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default implementation of the SagaOrchestrator using in-memory state with repository persistence.
 *
 * <p>
 * This orchestrator coordinates cross-service workflows by:
 * <ul>
 * <li>Maintaining saga state in the database</li>
 * <li>Executing commands through registered saga handlers</li>
 * <li>Publishing events to the transactional outbox</li>
 * <li>Running compensating transactions on failure</li>
 * <li>Retrying failed steps with exponential backoff</li>
 * </ul>
 */
@Service
public class DefaultSagaOrchestrator implements SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultSagaOrchestrator.class);

    private final SagaRepository sagaRepository;
    private final EventPublisher eventPublisher;
    private final SagaNoteRepository sagaNoteRepository;
    private final SagaAttachmentRepository sagaAttachmentRepository;
    private final IdentityPermissionClient identityPermissionClient;
    private final Map<String, SagaHandler> handlers = new ConcurrentHashMap<>();
    private final Map<UUID, SagaState> activeSagas = new ConcurrentHashMap<>();
    private final RetryConfig retryConfig;

    @Autowired
    public DefaultSagaOrchestrator(SagaRepository sagaRepository, EventPublisher eventPublisher,
            SagaNoteRepository sagaNoteRepository, SagaAttachmentRepository sagaAttachmentRepository,
            IdentityPermissionClient identityPermissionClient) {
        this(sagaRepository, eventPublisher, sagaNoteRepository, sagaAttachmentRepository, identityPermissionClient,
            new RetryConfig(3, Duration.ofSeconds(2), 2.0));
    }

    public DefaultSagaOrchestrator(SagaRepository sagaRepository, EventPublisher eventPublisher,
            SagaNoteRepository sagaNoteRepository, SagaAttachmentRepository sagaAttachmentRepository,
            RetryConfig retryConfig) {
        this(sagaRepository, eventPublisher, sagaNoteRepository, sagaAttachmentRepository,
            new IdentityPermissionClient("http://localhost:8081"), retryConfig);
    }

    public DefaultSagaOrchestrator(SagaRepository sagaRepository, EventPublisher eventPublisher,
            SagaNoteRepository sagaNoteRepository, SagaAttachmentRepository sagaAttachmentRepository,
            IdentityPermissionClient identityPermissionClient,
            RetryConfig retryConfig) {
        this.sagaRepository = sagaRepository;
        this.eventPublisher = eventPublisher;
        this.sagaNoteRepository = sagaNoteRepository;
        this.sagaAttachmentRepository = sagaAttachmentRepository;
        this.identityPermissionClient = identityPermissionClient;
        this.retryConfig = retryConfig;
    }

    @Override
    public UUID startSaga(String sagaType, SagaContext context) {
        UUID sagaId = context.getSagaId();
        String tenantId = TenantContext.get().map(Object::toString)
            .orElse(TenantSeedInitializer.DEFAULT_TENANT_ID.toString());

        // Build initial data map with userId and projectId from context
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("sagaId", sagaId.toString());
        initialData.put("sagaType", sagaType);
        if (context.getData() != null) {
            initialData.putAll(context.getData());
        }

        SagaState state = new SagaState(
            sagaId,
            sagaType,
            tenantId,
            "RUNNING",
            null,
            null,
            new ArrayList<>(),
            null,
            Instant.now(),
            Instant.now(),
            initialData);

        activeSagas.put(sagaId, state);
        sagaRepository.save(toEntity(state));

        log.info("Saga started: sagaId={}, type={}, tenantId={}", sagaId, sagaType, tenantId);

        try {
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(sagaType + ".SAGA_STARTED")
                .sagaType(sagaType)
                .sagaId(sagaId.toString())
                .tenantId(tenantId)
                .userId(context.getUserId())
                .data(context.getData())
                .build());
        } catch (Exception e) {
            log.warn("Failed to publish SAGA_STARTED event: sagaId={}", sagaId, e);
        }

        return sagaId;
    }

    @Override
    public SagaContext executeStep(UUID sagaId, String command, Map<String, Object> payload) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            // Rehydrate from DB after a service restart
            Optional<SagaEntity> entityOpt = sagaRepository.findById(sagaId.toString());
            state = entityOpt.map(this::toState).orElse(null);
            if (state != null) {
                activeSagas.put(sagaId, state);
            }
        }
        if (state == null) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }

        if ("COMPLETED".equals(state.getStatus()) || "ABORTED".equals(state.getStatus())) {
            throw new IllegalStateException("Saga is already " + state.getStatus() + ": " + sagaId);
        }

        SagaHandler handler = findHandlerForStep(state, command);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for command: " + command);
        }

        // Load context from state data
        SagaContext context = createContextFromState(state);

        try {
            // Execute the step — only business logic exceptions should trigger retry/abort
            context = handler.execute(context, command, payload);

            // Grant vendor access to their delivery saga after first step completes
            if ("DELIVERY_WORKFLOW".equals(state.getSagaType())) {
                grantDeliverySagaPermissions(sagaId, context);
            }

            // Determine next step
            String nextStep = handler.getNextStep(context, command);

            // Persist state — infrastructure failure here must not be reported as a saga business error.
            // If the DB write fails we log and continue; the step already succeeded.
            try {
                updateSagaState(state, command, nextStep, null, context);
            } catch (Exception persistEx) {
                log.error("Failed to persist saga state after step (step still succeeded): sagaId={}, step={}", sagaId, command, persistEx);
            }

            String stepTenantId = TenantContext.get().map(Object::toString)
                .orElse(state.getTenantId());

            // Build step data: Map.of() does not allow null values, so use HashMap when nextStep can be null
            Map<String, Object> stepEventData = new HashMap<>();
            stepEventData.put("step", command);
            stepEventData.put("nextStep", nextStep);

            // Publish step completed event — infrastructure failure must not abort a successful step.
            try {
                eventPublisher.publish(WorkflowEvent.builder()
                    .eventType(state.getSagaType() + ".STEP_COMPLETED")
                    .sagaType(state.getSagaType())
                    .sagaId(sagaId.toString())
                    .tenantId(stepTenantId)
                    .userId(context.getUserId())
                    .data(stepEventData)
                    .build());
            } catch (Exception publishEx) {
                log.warn("Failed to publish step completed event (step still succeeded): sagaId={}, step={}", sagaId, command, publishEx);
            }

            log.info("Saga step completed: sagaId={}, step={}, nextStep={}", sagaId, command, nextStep);

            // If no next step, saga is complete
            if (nextStep == null) {
                completeSaga(sagaId, context.getData());
            }

            return context;

        } catch (SagaException e) {
            handleStepFailure(state, command, e);
            throw new RuntimeException("Saga step failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unhandled exception in saga step: sagaId={}, step={}, error={}", sagaId, command, e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("step", command);
            if (e.toString() != null) {
                errorDetails.put("stackTrace", e.toString());
            }
            SagaException sagaEx = new SagaException("EXECUTION_ERROR", e.getMessage(), errorDetails);
            handleStepFailure(state, command, sagaEx);
            throw new RuntimeException("Saga step failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SagaContext compensateStep(UUID sagaId, String stepCode) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }

        // Find the handler that executed this step
        for (Map.Entry<String, SagaHandler> entry : handlers.entrySet()) {
            SagaHandler handler = entry.getValue();
            if (stepCode.equals(handler.getStepCode())) {
                try {
                    SagaContext context = createContextFromState(state);
                    context = handler.compensate(context, stepCode);

                    // Record compensation
                    updateSagaState(state, "COMPENSATE:" + stepCode, null, null, null);

                    log.info("Saga step compensated: sagaId={}, step={}", sagaId, stepCode);
                    return context;
                } catch (Exception e) {
                    log.error("Failed to compensate step: sagaId={}, step={}", sagaId, stepCode, e);
                }
            }
        }

        log.warn("No handler found to compensate step: sagaId={}, step={}", sagaId, stepCode);
        return createContextFromState(state);
    }

    @Override
    public void pauseSaga(UUID sagaId) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }

        if ("PAUSED".equals(state.getStatus())) {
            throw new IllegalStateException("Saga is already PAUSED: " + sagaId);
        }

        if ("COMPLETED".equals(state.getStatus()) || "ABORTED".equals(state.getStatus())) {
            throw new IllegalStateException("Saga is already " + state.getStatus() + ": " + sagaId);
        }

        // Extract userId from state data for the outbox event
        String userId = null;
        if (state.getData() != null) {
            Object userIdObj = state.getData().get("userId");
            if (userIdObj instanceof String s) {
                userId = s;
            }
        }

        // Build action-specific data payload
        Map<String, Object> pauseData = new HashMap<>(state.getData() != null ? state.getData() : Map.of());
        pauseData.put("action", "pause");

        // Use status-only update to preserve all other fields (user_id, project_id, steps_json, current_step, data_json)
        sagaRepository.updateStatusOnlyById(sagaId.toString(), "PAUSED", java.time.LocalDateTime.now());

        // Update in-memory cache
        SagaState newState = new SagaState(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            "PAUSED",
            state.getCurrentStep(),
            null,
            state.getSteps(),
            state.getLastError(),
            state.getCreatedAt(),
            Instant.now(),
            state.getData());
        activeSagas.put(sagaId, newState);

        log.info("Saga paused: sagaId={}", sagaId);

        try {
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(state.getSagaType() + ".SAGA_PAUSED")
                .sagaType(state.getSagaType())
                .sagaId(sagaId.toString())
                .tenantId(state.getTenantId())
                .userId(userId)
                .data(pauseData)
                .build());
        } catch (Exception e) {
            log.warn("Failed to publish SAGA_PAUSED event: sagaId={}", sagaId, e);
        }
    }

    @Override
    public void resumeSaga(UUID sagaId) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }

        if (!"PAUSED".equals(state.getStatus())) {
            throw new IllegalStateException("Saga is not PAUSED (current status: " + state.getStatus() + "): " + sagaId);
        }

        // Extract userId from state data for the outbox event
        String userId = null;
        if (state.getData() != null) {
            Object userIdObj = state.getData().get("userId");
            if (userIdObj instanceof String s) {
                userId = s;
            }
        }

        // Build action-specific data payload
        Map<String, Object> resumeData = new HashMap<>(state.getData() != null ? state.getData() : Map.of());
        resumeData.put("action", "resume");

        // Use status-only update to preserve all other fields (user_id, project_id, steps_json, current_step, data_json)
        sagaRepository.updateStatusOnlyById(sagaId.toString(), "RUNNING", java.time.LocalDateTime.now());

        // Update in-memory cache
        SagaState newState = new SagaState(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            "RUNNING",
            state.getCurrentStep(),
            null,
            state.getSteps(),
            state.getLastError(),
            state.getCreatedAt(),
            Instant.now(),
            state.getData());
        activeSagas.put(sagaId, newState);

        log.info("Saga resumed: sagaId={}", sagaId);

        try {
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(state.getSagaType() + ".SAGA_RESUMED")
                .sagaType(state.getSagaType())
                .sagaId(sagaId.toString())
                .tenantId(state.getTenantId())
                .userId(userId)
                .data(resumeData)
                .build());
        } catch (Exception e) {
            log.warn("Failed to publish SAGA_RESUMED event: sagaId={}", sagaId, e);
        }
    }

    @Override
    public void completeSaga(UUID sagaId, Map<String, Object> result) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            return;
        }

        // Preserve the original data map (including userId, projectId) for toEntity extraction
        SagaState newState = new SagaState(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            "COMPLETED",
            state.getCurrentStep(),
            null,
            state.getSteps(),
            state.getLastError(),
            state.getCreatedAt(),
            Instant.now(),
            state.getData()); // Preserve original data for userId/projectId extraction

        activeSagas.put(sagaId, newState);
        sagaRepository.save(toEntity(newState));

        log.info("Saga completed: sagaId={}", sagaId);

        try {
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(state.getSagaType() + ".SAGA_COMPLETED")
                .sagaType(state.getSagaType())
                .sagaId(sagaId.toString())
                .tenantId(state.getTenantId())
                .data(result)
                .build());
        } catch (Exception e) {
            log.warn("Failed to publish SAGA_COMPLETED event: sagaId={}", sagaId, e);
        }
    }

    @Override
    public void abortSaga(UUID sagaId, SagaError error) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            return;
        }

        // Preserve the original data map (including userId, projectId) for toEntity extraction
        SagaState newState = new SagaState(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            "ABORTED",
            state.getCurrentStep(),
            null,
            state.getSteps(),
            error,
            state.getCreatedAt(),
            Instant.now(),
            state.getData()); // Preserve original data for userId/projectId extraction

        activeSagas.put(sagaId, newState);
        sagaRepository.save(toEntity(newState));

        log.warn("Saga aborted: sagaId={}, error={}", sagaId, error.getErrorCode());

        // Attempt to compensate all completed steps in reverse order
        compensateCompletedSteps(newState);

        Map<String, Object> abortData = new HashMap<>();
        abortData.put("error", error.getErrorCode());
        abortData.put("message", error.getMessage());
        try {
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(state.getSagaType() + ".SAGA_ABORTED")
                .sagaType(state.getSagaType())
                .sagaId(sagaId.toString())
                .tenantId(state.getTenantId())
                .data(abortData)
                .build());
        } catch (Exception e) {
            log.warn("Failed to publish SAGA_ABORTED event: sagaId={}", sagaId, e);
        }
    }

    @Override
    public SagaState getSagaState(UUID sagaId) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            Optional<SagaEntity> entityOpt = sagaRepository.findById(sagaId.toString());
            state = entityOpt.map(this::toState).orElse(null);
            if (state != null) {
                activeSagas.put(sagaId, state);
            }
        }
        return decorateWithCounts(state);
    }

    @Override
    public java.util.Optional<SagaState> getSagaStateOptional(UUID sagaId) {
        return Optional.ofNullable(getSagaState(sagaId));
    }

    @Override
    public void registerHandler(String sagaType, SagaHandler handler) {
        handlers.put(sagaType, handler);
        log.info("Saga handler registered: type={}", sagaType);
    }

    @Override
    public SagaContext updateSagaData(UUID sagaId, Map<String, Object> newData) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }

        // Merge new data into existing data
        Map<String, Object> updatedData = new HashMap<>(state.getData());
        if (newData != null) {
            updatedData.putAll(newData);
        }

        // Update the saga state with the merged data while preserving status and steps
        SagaState newState = new SagaState(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            state.getStatus(),
            state.getCurrentStep(),
            state.getNextStep(),
            state.getSteps(),
            state.getLastError(),
            state.getCreatedAt(),
            Instant.now(),
            updatedData);

        activeSagas.put(sagaId, newState);
        sagaRepository.save(toEntity(newState));

        log.info("Saga data updated: sagaId={}", sagaId);

        return new SagaContext(
            newState.getSagaId(),
            newState.getSagaType(),
            newState.getTenantId(),
            updatedData);
    }

    @Override
    public List<SagaState> getSagaInstancesByAggregate(String aggregateType, UUID aggregateId) {
        List<SagaState> results = new ArrayList<>();

        // Search in memory
        for (SagaState state : activeSagas.values()) {
            String key = aggregateType + "Id";
            Object val = state.getData().get(key);
            if (val != null && val.toString().equals(aggregateId.toString())) {
                results.add(state);
            }
        }

        return decorateWithCounts(results);
    }

    /**
     * Get all active saga states (for listing endpoint).
     * Loads from in-memory cache first, then from database for any sagas not in memory.
     */
    public List<SagaState> getActiveSagas() {
        // Start with in-memory sagas
        List<SagaState> result = new ArrayList<>(activeSagas.values());

        // For sagas that exist in the database but not in memory, load them
        // This ensures sagas are visible after server restarts
        List<UUID> inMemoryIds = result.stream()
            .map(SagaState::getSagaId)
            .collect(java.util.stream.Collectors.toList());

        List<SagaEntity> allEntities = sagaRepository.findAll();
        for (SagaEntity entity : allEntities) {
            // entity.getId() returns String (saga identifier), convert to UUID for comparison
            UUID entityId = UUID.fromString(entity.getId());
            if (!inMemoryIds.contains(entityId)) {
                SagaState state = toState(entity);
                // Cache in memory for future lookups
                activeSagas.put(state.getSagaId(), state);
                result.add(state);
            }
        }

        return decorateWithCounts(result);
    }

    /**
     * Archive every Bidding/Procurement saga (or any other saga type) tied to a Project - called
     * when the Project itself is deleted. Does not touch bidding.bid_wallets or
     * bidding.bid_transactions, which are per-vendor financial ledgers shared across every project
     * a vendor has ever bid on, not project-scoped.
     */
    public int archiveByProjectId(String projectId) {
        return sagaRepository.archiveByProjectId(projectId);
    }

    /**
     * Count non-archived sagas tied to a Project.
     */
    public long countByProjectId(String projectId) {
        return sagaRepository.countByProjectIdAndDeletedFalse(projectId);
    }

    // --- Notes & Attachments ---
    //
    // Sagas have no individually-addressable step records (see SagaNoteEntity's Javadoc), so
    // unlike WorkflowService's equivalent methods there is no optional step-level scoping here -
    // every note/attachment is saga-level.

    /**
     * Add a note to a saga.
     */
    public SagaNoteEntity addNote(String sagaId, UUID authorId, String note, SagaNoteEntity.NoteType type) {
        if (!sagaRepository.existsById(sagaId)) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }
        SagaNoteEntity saved = sagaNoteRepository.save(new SagaNoteEntity(sagaId, authorId, note, type));
        log.info("Added {} note to saga {}", type, sagaId);
        return saved;
    }

    public List<SagaNoteEntity> listNotes(String sagaId) {
        return sagaNoteRepository.findBySagaIdOrderByCreatedAtDesc(sagaId);
    }

    /**
     * Record a photo/document attachment on a saga. The file bytes are uploaded directly from
     * the browser to object storage via document-service's presign endpoint; this only stores
     * the reference metadata.
     *
     * <p>
     * Idempotent by {@code id}: a client-generated id lets the offline upload queue safely retry
     * this call after a dropped connection without creating a duplicate row.
     */
    public SagaAttachmentEntity addAttachment(String sagaId, UUID id, UUID uploadedBy,
            SagaAttachmentEntity.AttachmentKind kind, String objectKey, String originalFileName,
            String contentType, Long byteSize) {
        Optional<SagaAttachmentEntity> existing = sagaAttachmentRepository.findById(id);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (!sagaRepository.existsById(sagaId)) {
            throw new IllegalArgumentException("Saga not found: " + sagaId);
        }

        SagaAttachmentEntity attachment = new SagaAttachmentEntity(id, sagaId, uploadedBy, kind,
            objectKey, originalFileName, contentType, byteSize);
        SagaAttachmentEntity saved = sagaAttachmentRepository.save(attachment);
        log.info("Recorded {} attachment {} on saga {}", kind, saved.getId(), sagaId);
        return saved;
    }

    public List<SagaAttachmentEntity> listAttachments(String sagaId) {
        return sagaAttachmentRepository.findBySagaIdOrderByCreatedAtDesc(sagaId);
    }

    /**
     * Delete a photo/document attachment from a saga. Returns the deleted attachment's object
     * key so the caller can also remove the underlying object from storage.
     */
    public String deleteAttachment(String sagaId, UUID attachmentId) {
        SagaAttachmentEntity attachment = sagaAttachmentRepository.findByIdAndSagaId(attachmentId, sagaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Attachment " + attachmentId + " not found on saga " + sagaId));

        String objectKey = attachment.getObjectKey();
        sagaAttachmentRepository.delete(attachment);
        log.info("Deleted attachment {} from saga {}", attachmentId, sagaId);
        return objectKey;
    }

    // --- Entity/State conversion ---

    private SagaEntity toEntity(SagaState state) {
        // Extract userId and projectId from state data if available
        String userId = null;
        String projectId = null;
        if (state.getData() != null) {
            Object userIdObj = state.getData().get("userId");
            if (userIdObj instanceof String s) {
                userId = s;
            }
            Object projectIdObj = state.getData().get("projectId");
            if (projectIdObj != null) {
                projectId = projectIdObj.toString();
            }
        }
        // Serialize the complete data map to JSON
        String dataJson = serializeStateData(state.getData());
        // Use sagaId.toString() as the primary key (id) - this is the saga identifier
        // JPA will UPDATE existing records when the same id is saved, instead of INSERT
        return new SagaEntity(
            state.getSagaId().toString(),  // id (primary key = saga identifier)
            state.getSagaType(),
            state.getTenantId(),
            state.getStatus(),
            state.getCurrentStep(),
            state.getNextStep(),
            serializeSteps(state.getSteps()),
            state.getLastError() != null ? serializeError(state.getLastError()) : null,
            dataJson,
            userId,
            projectId,
            LocalDateTime.now(),
            LocalDateTime.now());
    }

    /**
     * Convert a SagaEntity to SagaState, populating data from entity fields.
     */
    private SagaState toState(SagaEntity entity) {
        // Try to deserialize the complete data map from dataJson first
        Map<String, Object> data = deserializeStateData(entity.getDataJson());
        if (data == null) {
            data = new HashMap<>();
        }
        // Ensure projectId and userId are set from entity fields (for backwards compatibility)
        if (entity.getProjectId() != null && !data.containsKey("projectId")) {
            data.put("projectId", entity.getProjectId().toString());
        }
        if (entity.getUserId() != null && !data.containsKey("userId")) {
            data.put("userId", entity.getUserId());
        }
        // Store sagaType in data for frontend access
        if (!data.containsKey("sagaType")) {
            data.put("sagaType", entity.getSagaType());
        }

        // Convert String id (saga identifier) back to UUID for SagaState
        UUID sagaId = UUID.fromString(entity.getId());
        return new SagaState(
            sagaId,
            entity.getSagaType(),
            entity.getTenantId(),
            entity.getStatus(),
            entity.getCurrentStep(),
            entity.getNextStep(),
            deserializeSteps(entity.getStepsJson()),
            deserializeError(entity.getLastErrorJson()),
            entity.getCreatedAt() != null ? entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : Instant.now(),
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : Instant.now(),
            data);
    }

    // --- Note/attachment count decoration ---
    //
    // Applied only at the three methods above that hand a SagaState (or list of them) across the
    // HTTP boundary to SagaController (getSagaState, getSagaInstancesByAggregate, getActiveSagas)
    // - deliberately never inside toState()/toEntity() or written back into the activeSagas
    // cache, so internal state-transition logic never pays this cost and counts are always
    // recomputed fresh (cheap indexed COUNTs) on every external read rather than risking
    // staleness after a note/attachment is added.

    private SagaState decorateWithCounts(SagaState state) {
        if (state == null) {
            return null;
        }
        String sagaId = state.getSagaId().toString();
        return state.withCounts(sagaNoteRepository.countBySagaId(sagaId), sagaAttachmentRepository.countBySagaId(sagaId));
    }

    private List<SagaState> decorateWithCounts(List<SagaState> states) {
        if (states.isEmpty()) {
            return states;
        }
        List<String> sagaIds = states.stream().map(s -> s.getSagaId().toString()).collect(java.util.stream.Collectors.toList());
        Map<String, Long> noteCounts = toCountMap(sagaNoteRepository.countBySagaIdIn(sagaIds));
        Map<String, Long> attachmentCounts = toCountMap(sagaAttachmentRepository.countBySagaIdIn(sagaIds));
        return states.stream()
            .map(s -> {
                String sagaId = s.getSagaId().toString();
                return s.withCounts(noteCounts.getOrDefault(sagaId, 0L), attachmentCounts.getOrDefault(sagaId, 0L));
            })
            .collect(java.util.stream.Collectors.toList());
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }

    /**
     * Serialize the state data map to JSON string.
     */
    private String serializeStateData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deserialize the data JSON string back to a map.
     */
    private Map<String, Object> deserializeStateData(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            java.util.Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, java.util.Map.class);
            // Convert any numeric types to proper Java types
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    result.put(entry.getKey(), ((Integer) value).longValue());
                } else if (value instanceof Number && !(value instanceof Long) && !(value instanceof Double)) {
                    result.put(entry.getKey(), ((Number) value).doubleValue());
                } else {
                    result.put(entry.getKey(), value);
                }
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeSteps(List<SagaStep> steps) {
        try {
            java.util.List<Map<String, Object>> mapList = new ArrayList<>();
            for (SagaStep s : steps) {
                Map<String, Object> m = new HashMap<>();
                m.put("stepCode", s.getStepCode());
                m.put("stepName", s.getStepName());
                m.put("status", s.getStatus());
                mapList.add(m);
            }
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mapList);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<SagaStep> deserializeSteps(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            java.util.List<Map<String, Object>> mapList = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, java.util.List.class);
            List<SagaStep> steps = new ArrayList<>();
            for (Map<String, Object> m : mapList) {
                steps.add(new SagaStep(
                    (String) m.get("stepCode"),
                    (String) m.get("stepName"),
                    (String) m.get("status"),
                    Instant.now(),
                    Instant.now(),
                    null));
            }
            return steps;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String serializeError(SagaError error) {
        if (error == null) {
            return null;
        }
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("errorCode", error.getErrorCode());
            m.put("message", error.getMessage());
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private SagaError deserializeError(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, java.util.Map.class);
            return new SagaError(
                (String) m.get("errorCode"),
                (String) m.get("message"),
                Map.of());
        } catch (Exception e) {
            return null;
        }
    }

    // --- Private methods ---

    private SagaHandler findHandlerForStep(SagaState state, String command) {
        SagaHandler handler = handlers.get(state.getSagaType());
        if (handler != null) {
            return handler;
        }

        // Try to find handler by step code matching
        for (SagaHandler h : handlers.values()) {
            if (command.equals(h.getStepCode())) {
                return h;
            }
        }

        return null;
    }

    /**
     * Grant the vendor read/write access to their delivery saga immediately after the first step completes.
     * This ensures vendors can see and interact with their delivery schedules from the "My Purchase Orders" page.
     */
    private void grantDeliverySagaPermissions(UUID sagaId, SagaContext context) {
        try {
            Map<String, Object> data = context.getData();
            String vendorIdStr = (data != null) ? (String) data.get("vendorId") : null;
            String userIdStr = (data != null) ? (String) data.get("userId") : null;

            if (vendorIdStr != null && userIdStr != null) {
                UUID vendorUuid = UUID.fromString(vendorIdStr);
                UUID userUuid = UUID.fromString(userIdStr);
                ResourceMembershipDto membership = identityPermissionClient
                    .grantDeliverySagaAccess(sagaId, vendorUuid, userUuid);
                if (membership != null) {
                    log.info("Granted delivery saga access for vendor {} on saga {}", vendorIdStr, sagaId);
                } else {
                    log.warn("Failed to grant delivery saga access for vendor {} on saga {} (identity service returned null)",
                        vendorIdStr, sagaId);
                }
            } else {
                log.debug("Skipping delivery saga permission grant: vendorId={}, userId={}", vendorIdStr, userIdStr);
            }
        } catch (Exception e) {
            log.error("Error granting delivery saga permissions for saga {}: {}", sagaId, e.getMessage(), e);
            // Fail-open: do not block saga execution if permission grant fails
        }
    }

    private SagaContext createContextFromState(SagaState state) {
        // Start with a copy of all persisted data from state (includes vendorIds, inviteConfig, etc.)
        Map<String, Object> data = state.getData() != null ? new HashMap<>(state.getData()) : new HashMap<>();
        // Override with latest runtime values
        data.put("sagaStatus", state.getStatus());
        data.put("currentStep", state.getCurrentStep());
        // Ensure userId and projectId are present
        if (state.getData() != null) {
            Object userIdObj = state.getData().get("userId");
            if (userIdObj != null) {
                data.put("userId", userIdObj);
            }
            Object projectIdObj = state.getData().get("projectId");
            if (projectIdObj != null) {
                data.put("projectId", projectIdObj);
            }
        }
        return new SagaContext(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            data);
    }

    private void updateSagaState(SagaState state, String stepCode, String nextStep, SagaError error, SagaContext context) {
        List<SagaStep> steps = new ArrayList<>(state.getSteps());
        steps.add(new SagaStep(
            stepCode,
            stepCode,
            error == null ? "COMPLETED" : "FAILED",
            Instant.now(),
            error == null ? Instant.now() : null,
            error));

        // Use the updated currentStep from context if available, otherwise fall back to the old state value.
        // The handler sets currentStep in context.getData() via context.setCurrentStep(), but the SagaState
        // currentStep field was never updated from that context — causing the stale value bug.
        String updatedCurrentStep = (context != null && context.getCurrentStep() != null)
            ? context.getCurrentStep()
            : state.getCurrentStep();

        // Use post-execution context data so handler output (e.g. tenderId) is persisted.
        // context.getData() is a superset of state.getData() — userId and projectId are not lost.
        Map<String, Object> persistedData = (context != null && context.getData() != null)
            ? context.getData()
            : state.getData();
        SagaState newState = new SagaState(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            error == null ? "RUNNING" : "ABORTED",
            updatedCurrentStep,
            nextStep,
            steps,
            error,
            state.getCreatedAt(),
            Instant.now(),
            persistedData);

        activeSagas.put(newState.getSagaId(), newState);
        sagaRepository.save(toEntity(newState));
    }

    private void handleStepFailure(SagaState state, String stepCode, SagaException exception) {
        log.error("Saga step failed: sagaId={}, step={}, error={}", state.getSagaId(), stepCode,
            exception.getErrorCode());

        // Attempt retry with backoff
        int retryCount = 0;
        while (retryCount < retryConfig.maxRetries()) {
            try {
                Thread.sleep(retryConfig.baseDelay().multipliedBy(
                    (long) Math.pow(retryConfig.backoffMultiplier(), retryCount)).toMillis());

                // Try to re-execute
                executeStep(state.getSagaId(), stepCode, new HashMap<>());
                return; // Success on retry

            } catch (Exception retryEx) {
                retryCount++;
                log.warn("Retry {} failed for saga step: {}", retryCount, stepCode, retryEx);
            }
        }

        // All retries exhausted, abort saga
        abortSaga(state.getSagaId(), new SagaError(
            exception.getErrorCode(),
            exception.getMessage(),
            exception.getDetails()));
    }

    private void compensateCompletedSteps(SagaState state) {
        List<SagaStep> steps = state.getSteps();
        // Compensate in reverse order
        for (int i = steps.size() - 1; i >= 0; i--) {
            SagaStep step = steps.get(i);
            if ("COMPLETED".equals(step.getStatus())) {
                try {
                    compensateStep(state.getSagaId(), step.getStepCode());
                } catch (Exception e) {
                    log.error("Failed to compensate step: {}", step.getStepCode(), e);
                }
            }
        }
    }

    /**
     * Configuration for retry behavior.
     */
    public record RetryConfig(int maxRetries, java.time.Duration baseDelay, double backoffMultiplier) {
    }
}
