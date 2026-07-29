package io.cerp.workflow.saga;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Context holder for saga execution data. Contains aggregate IDs, workflow steps, and transient data.
 */
public class SagaContext {
    private final UUID sagaId;
    private final String sagaType;
    private final String tenantId;
    private final Map<String, Object> data;
    private final Instant createdAt;
    private Instant updatedAt;

    public SagaContext(UUID sagaId, String sagaType, String tenantId, Map<String, Object> data) {
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.tenantId = tenantId;
        this.data = data;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
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

    public Map<String, Object> getData() {
        return data;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void put(String key, Object value) {
        data.put(key, value);
        this.updatedAt = Instant.now();
    }

    public UUID getAggregateId(String aggregateType) {
        String key = aggregateType + "Id";
        Object val = data.get(key);
        if (val instanceof String s) {
            return UUID.fromString(s);
        }
        return val instanceof UUID u ? u : null;
    }

    public void setAggregateId(String aggregateType, UUID aggregateId) {
        data.put(aggregateType + "Id", aggregateId.toString());
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(data.get("sagaStatus"));
    }

    public boolean isAborted() {
        return "ABORTED".equals(data.get("sagaStatus"));
    }

    /**
     * Get the current workflow step being executed.
     */
    public String getCurrentStep() {
        Object step = data.get("currentStep");
        return step != null ? step.toString() : null;
    }

    /**
     * Set the current workflow step.
     */
    public void setCurrentStep(String step) {
        data.put("currentStep", step);
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the saga as completed.
     */
    public void complete() {
        data.put("sagaStatus", "COMPLETED");
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the saga as aborted.
     */
    public void abort() {
        data.put("sagaStatus", "ABORTED");
        this.updatedAt = Instant.now();
    }

    /**
     * Get the user ID associated with this saga.
     */
    public String getUserId() {
        Object userId = data.get("userId");
        return userId != null ? userId.toString() : null;
    }

    /**
     * Set the user ID associated with this saga.
     */
    public void setUserId(String userId) {
        data.put("userId", userId);
    }

    /**
     * Get the aggregate ID for the saga (defaults to sagaId).
     */
    public UUID getAggregateId() {
        Object aggId = data.get("aggregateId");
        if (aggId instanceof String s) {
            return UUID.fromString(s);
        }
        return aggId instanceof UUID u ? u : sagaId;
    }

    /**
     * Set the aggregate ID for the saga.
     */
    public void setAggregateId(UUID aggregateId) {
        data.put("aggregateId", aggregateId.toString());
    }
}
