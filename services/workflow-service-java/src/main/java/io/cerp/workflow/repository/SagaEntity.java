package io.cerp.workflow.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for saga state persistence.
 */
@Entity
@Table(name = "saga_events")
public class SagaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SagaEntity() {
    }

    public SagaEntity(UUID id, String sagaType, String tenantId, String status,
            String currentStep, String nextStep, String stepsJson,
            String lastErrorJson, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sagaType = sagaType;
        this.tenantId = tenantId;
        this.status = status;
        this.currentStep = currentStep;
        this.nextStep = nextStep;
        this.stepsJson = stepsJson;
        this.lastErrorJson = lastErrorJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
}
