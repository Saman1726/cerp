package io.cerp.workflow.saga;

import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.seed.TenantSeedInitializer;
import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.repository.SagaEntity;
import io.cerp.workflow.repository.SagaRepository;
import io.cerp.workflow.service.EventPublisher;
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
    private final Map<String, SagaHandler> handlers = new ConcurrentHashMap<>();
    private final Map<UUID, SagaState> activeSagas = new ConcurrentHashMap<>();
    private final RetryConfig retryConfig;

    @Autowired
    public DefaultSagaOrchestrator(SagaRepository sagaRepository, EventPublisher eventPublisher) {
        this(sagaRepository, eventPublisher, new RetryConfig(3, Duration.ofSeconds(2), 2.0));
    }

    public DefaultSagaOrchestrator(SagaRepository sagaRepository, EventPublisher eventPublisher,
            RetryConfig retryConfig) {
        this.sagaRepository = sagaRepository;
        this.eventPublisher = eventPublisher;
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

        // Publish saga started event
        eventPublisher.publish(WorkflowEvent.builder()
            .eventType(sagaType + ".SAGA_STARTED")
            .sagaType(sagaType)
            .sagaId(sagaId.toString())
            .tenantId(tenantId)
            .userId(context.getUserId())
            .data(context.getData())
            .build());

        return sagaId;
    }

    @Override
    public SagaContext executeStep(UUID sagaId, String command, Map<String, Object> payload) {
        SagaState state = activeSagas.get(sagaId);
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
            // Execute the step
            context = handler.execute(context, command, payload);

            // Determine next step
            String nextStep = handler.getNextStep(context, command);

            // Update saga state
            updateSagaState(state, command, nextStep, null);

            String stepTenantId = TenantContext.get().map(Object::toString)
                .orElse(state.getTenantId());

            // Publish step completed event
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(state.getSagaType() + ".STEP_COMPLETED")
                .sagaType(state.getSagaType())
                .sagaId(sagaId.toString())
                .tenantId(stepTenantId)
                .userId(context.getUserId())
                .data(Map.of("step", command, "nextStep", nextStep))
                .build());

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
            SagaException sagaEx = new SagaException("EXECUTION_ERROR", e.getMessage(), Map.of("step", command));
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
                    updateSagaState(state, "COMPENSATE:" + stepCode, null, null);

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

        eventPublisher.publish(WorkflowEvent.builder()
            .eventType(state.getSagaType() + ".SAGA_COMPLETED")
            .sagaType(state.getSagaType())
            .sagaId(sagaId.toString())
            .tenantId(state.getTenantId())
            .data(result)
            .build());
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

        eventPublisher.publish(WorkflowEvent.builder()
            .eventType(state.getSagaType() + ".SAGA_ABORTED")
            .sagaType(state.getSagaType())
            .sagaId(sagaId.toString())
            .tenantId(state.getTenantId())
            .data(Map.of("error", error.getErrorCode(), "message", error.getMessage()))
            .build());
    }

    @Override
    public SagaState getSagaState(UUID sagaId) {
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            Optional<SagaEntity> entityOpt = sagaRepository.findById(sagaId);
            state = entityOpt.map(this::toState).orElse(null);
            if (state != null) {
                activeSagas.put(sagaId, state);
            }
        }
        return state;
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

        return results;
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
            if (!inMemoryIds.contains(entity.getId())) {
                SagaState state = toState(entity);
                // Cache in memory for future lookups
                activeSagas.put(state.getSagaId(), state);
                result.add(state);
            }
        }

        return result;
    }

    // --- Entity/State conversion ---

    private SagaEntity toEntity(SagaState state) {
        // Extract userId and projectId from state data if available
        String userId = null;
        UUID projectId = null;
        if (state.getData() != null) {
            Object userIdObj = state.getData().get("userId");
            if (userIdObj instanceof String s) {
                userId = s;
            }
            Object projectIdObj = state.getData().get("projectId");
            if (projectIdObj instanceof String ps) {
                try {
                    projectId = UUID.fromString(ps);
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            } else if (projectIdObj instanceof UUID pu) {
                projectId = pu;
            }
        }
        return new SagaEntity(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            state.getStatus(),
            state.getCurrentStep(),
            state.getNextStep(),
            serializeSteps(state.getSteps()),
            state.getLastError() != null ? serializeError(state.getLastError()) : null,
            state.getSagaId().toString(),
            userId,
            projectId,
            LocalDateTime.now(),
            LocalDateTime.now());
    }

    /**
     * Convert a SagaEntity to SagaState, populating data from entity fields.
     */
    private SagaState toState(SagaEntity entity) {
        Map<String, Object> data = new HashMap<>();
        if (entity.getProjectId() != null) {
            data.put("projectId", entity.getProjectId().toString());
        }
        if (entity.getUserId() != null) {
            data.put("userId", entity.getUserId());
        }
        // Store sagaType in data for frontend access
        data.put("sagaType", entity.getSagaType());

        return new SagaState(
            entity.getId(),
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

    private SagaContext createContextFromState(SagaState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("sagaStatus", state.getStatus());
        data.put("currentStep", state.getCurrentStep());
        // Propagate userId and projectId from state data to saga context
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

    private void updateSagaState(SagaState state, String stepCode, String nextStep, SagaError error) {
        List<SagaStep> steps = new ArrayList<>(state.getSteps());
        steps.add(new SagaStep(
            stepCode,
            stepCode,
            error == null ? "COMPLETED" : "FAILED",
            Instant.now(),
            error == null ? Instant.now() : null,
            error));

        // Preserve the original data map (including userId, projectId) for toEntity extraction
        SagaState newState = new SagaState(
            state.getSagaId(),
            state.getSagaType(),
            state.getTenantId(),
            error == null ? "RUNNING" : "ABORTED",
            stepCode,
            nextStep,
            steps,
            error,
            state.getCreatedAt(),
            Instant.now(),
            state.getData()); // Preserve original data for userId/projectId extraction

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
