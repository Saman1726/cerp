package io.cerp.workflow.dto;

import io.cerp.workflow.domain.TemplatePhaseMapping.BuildingTypeGroup;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for template phase mappings.
 */
public class TemplatePhaseMappingResponse {

    private UUID id;
    private String tenantId;
    private String phaseType;
    private BuildingTypeGroup buildingTypeGroup;
    private String workflowTemplateCode;
    private Integer priority;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ──

    public TemplatePhaseMappingResponse() {
    }

    public TemplatePhaseMappingResponse(UUID id, String tenantId, String phaseType,
            BuildingTypeGroup buildingTypeGroup, String workflowTemplateCode,
            Integer priority, Boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.phaseType = phaseType;
        this.buildingTypeGroup = buildingTypeGroup;
        this.workflowTemplateCode = workflowTemplateCode;
        this.priority = priority;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ── Getters and Setters ──

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPhaseType() {
        return phaseType;
    }

    public void setPhaseType(String phaseType) {
        this.phaseType = phaseType;
    }

    public BuildingTypeGroup getBuildingTypeGroup() {
        return buildingTypeGroup;
    }

    public void setBuildingTypeGroup(BuildingTypeGroup buildingTypeGroup) {
        this.buildingTypeGroup = buildingTypeGroup;
    }

    public String getWorkflowTemplateCode() {
        return workflowTemplateCode;
    }

    public void setWorkflowTemplateCode(String workflowTemplateCode) {
        this.workflowTemplateCode = workflowTemplateCode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
