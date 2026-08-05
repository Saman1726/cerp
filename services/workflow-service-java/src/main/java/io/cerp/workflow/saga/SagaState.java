package io.cerp.workflow.saga;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * State representation of a saga instance.
 */
public class SagaState {
    private final UUID sagaId;
    private final String sagaType;
    private final String tenantId;
    private final String status;
    private final String currentStep;
    private final String nextStep;
    private final List<SagaStep> steps;
    private final SagaError lastError;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Map<String, Object> data;

    public SagaState(UUID sagaId, String sagaType, String tenantId, String status,
            String currentStep, String nextStep, List<SagaStep> steps,
            SagaError lastError, Instant createdAt, Instant updatedAt) {
        this(sagaId, sagaType, tenantId, status, currentStep, nextStep, steps, lastError, createdAt, updatedAt,
            Map.of());
    }

    public SagaState(UUID sagaId, String sagaType, String tenantId, String status,
            String currentStep, String nextStep, List<SagaStep> steps,
            SagaError lastError, Instant createdAt, Instant updatedAt,
            Map<String, Object> data) {
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.tenantId = tenantId;
        this.status = status;
        this.currentStep = currentStep;
        this.nextStep = nextStep;
        this.steps = steps;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.data = data != null ? Collections.unmodifiableMap(new HashMap<>(data)) : Collections.emptyMap();
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public String getNextStep() {
        return nextStep;
    }

    public List<SagaStep> getSteps() {
        return steps;
    }

    public SagaError getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Get the arbitrary data map for this saga state.
     */
    public Map<String, Object> getData() {
        return data;
    }
}
