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
    private final long noteCount;
    private final long attachmentCount;

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
        this(sagaId, sagaType, tenantId, status, currentStep, nextStep, steps, lastError, createdAt, updatedAt,
            data, 0L, 0L);
    }

    // Counts are populated only at the HTTP read-boundary methods in DefaultSagaOrchestrator
    // (getSagaState/getSagaInstancesByAggregate/getActiveSagas), via withCounts() below - never by
    // the internal state-transition constructor calls above, which is why those keep delegating
    // to this constructor with 0L, 0L rather than threading two more args through every one of
    // them (the same "one of N call sites forgets it" bug class a prior feature already hit once).
    public SagaState(UUID sagaId, String sagaType, String tenantId, String status,
            String currentStep, String nextStep, List<SagaStep> steps,
            SagaError lastError, Instant createdAt, Instant updatedAt,
            Map<String, Object> data, long noteCount, long attachmentCount) {
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
        this.noteCount = noteCount;
        this.attachmentCount = attachmentCount;
    }

    /** Returns a copy with counts populated; the copy is never written back into any internal cache. */
    public SagaState withCounts(long noteCount, long attachmentCount) {
        return new SagaState(sagaId, sagaType, tenantId, status, currentStep, nextStep, steps,
            lastError, createdAt, updatedAt, data, noteCount, attachmentCount);
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

    public long getNoteCount() {
        return noteCount;
    }

    public long getAttachmentCount() {
        return attachmentCount;
    }
}
