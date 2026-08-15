package io.cerp.workflow.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA entity for saga state persistence.
 * 
 * <p>The {@code id} field stores the saga identifier (sagaId) and serves as the primary key.
 * This ensures JPA performs UPDATE instead of INSERT when saving existing saga state.
 */
@Entity
@Table(name = "saga_events", schema = "workflow")
public class SagaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "saga_type", nullable = false)
    private String sagaType;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "next_step")
    private String nextStep;

    @Column(name = "steps_json", columnDefinition = "TEXT")
    private String stepsJson;

    @Column(name = "last_error_json", columnDefinition = "TEXT")
    private String lastErrorJson;

    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;

    /**
     * Legacy user ID field — kept for backward compatibility.
     * New code should use {@link #createdBy} and {@link #assignedTo}.
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * User ID of the saga creator. Used to show sagas in "Created by Me" view.
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * User ID of the person assigned to the saga. Used to show sagas in "Assigned to Me" view.
     */
    @Column(name = "assigned_to")
    private String assignedTo;

    /**
     * Party IDs (vendor IDs, org IDs) that have access to this saga.
     * Used for vendor/procurement shared access. Stored as JSON array.
     */
    @Column(name = "party_ids", columnDefinition = "TEXT")
    private String partyIds;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SagaEntity() {
    }

    public SagaEntity(String id, String sagaType, String tenantId, String status,
            String currentStep, String nextStep, String stepsJson,
            String lastErrorJson, String dataJson, String userId, String createdBy,
            String assignedTo, String partyIds, String projectId,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sagaType = sagaType;
        this.tenantId = tenantId;
        this.status = status;
        this.currentStep = currentStep;
        this.nextStep = nextStep;
        this.stepsJson = stepsJson;
        this.lastErrorJson = lastErrorJson;
        this.dataJson = dataJson;
        this.userId = userId;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
        this.partyIds = partyIds;
        this.projectId = projectId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSagaType() {
        return sagaType;
    }

    public void setSagaType(String sagaType) {
        this.sagaType = sagaType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    public String getStepsJson() {
        return stepsJson;
    }

    public void setStepsJson(String stepsJson) {
        this.stepsJson = stepsJson;
    }

    public String getLastErrorJson() {
        return lastErrorJson;
    }

    public void setLastErrorJson(String lastErrorJson) {
        this.lastErrorJson = lastErrorJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getPartyIds() {
        return partyIds;
    }

    public void setPartyIds(String partyIds) {
        this.partyIds = partyIds;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}