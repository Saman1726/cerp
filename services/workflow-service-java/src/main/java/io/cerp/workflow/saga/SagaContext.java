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
     * Get the saga creator's user ID.
     */
    public String getCreatedBy() {
        Object createdBy = data.get("createdBy");
        return createdBy != null ? createdBy.toString() : null;
    }

    /**
     * Set the saga creator's user ID.
     */
    public void setCreatedBy(String createdBy) {
        data.put("createdBy", createdBy);
    }

    /**
     * Get the assigned user ID.
     */
    public String getAssignedTo() {
        Object assignedTo = data.get("assignedTo");
        return assignedTo != null ? assignedTo.toString() : null;
    }

    /**
     * Set the assigned user ID.
     */
    public void setAssignedTo(String assignedTo) {
        data.put("assignedTo", assignedTo);
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

    /**
     * Get the project ID associated with this saga.
     */
    public UUID getProjectId() {
        Object projectId = data.get("projectId");
        if (projectId instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return projectId instanceof UUID u ? u : null;
    }

    /**
     * Set the project ID associated with this saga.
     */
    public void setProjectId(UUID projectId) {
        data.put("projectId", projectId != null ? projectId.toString() : null);
    }

    /**
     * Get the material ID associated with this saga.
     */
    public String getMaterialId() {
        Object materialId = data.get("materialId");
        return materialId != null ? materialId.toString() : null;
    }

    /**
     * Set the material ID associated with this saga.
     */
    public void setMaterialId(String materialId) {
        data.put("materialId", materialId);
    }

    /**
     * Get the quantity associated with this saga.
     */
    public Integer getQuantity() {
        Object quantity = data.get("quantity");
        if (quantity instanceof Integer i) {
            return i;
        }
        if (quantity instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    /**
     * Set the quantity associated with this saga.
     */
    public void setQuantity(Integer quantity) {
        data.put("quantity", quantity);
    }
}
