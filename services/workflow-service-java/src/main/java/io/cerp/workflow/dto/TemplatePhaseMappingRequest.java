package io.cerp.workflow.dto;

import io.cerp.workflow.domain.TemplatePhaseMapping.BuildingTypeGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request DTO for creating or updating a template phase mapping.
 */
public class TemplatePhaseMappingRequest {

    @NotBlank(message = "Phase type is required")
    private String phaseType;

    @NotNull(message = "Building type group is required")
    private BuildingTypeGroup buildingTypeGroup;

    @NotBlank(message = "Workflow template code is required")
    private String workflowTemplateCode;

    @PositiveOrZero(message = "Priority must be zero or positive")
    private Integer priority = 0;

    private Boolean enabled = true;

    // ── Constructors ──

    public TemplatePhaseMappingRequest() {
    }

    public TemplatePhaseMappingRequest(String phaseType, BuildingTypeGroup buildingTypeGroup,
            String workflowTemplateCode, Integer priority, Boolean enabled) {
        this.phaseType = phaseType;
        this.buildingTypeGroup = buildingTypeGroup;
        this.workflowTemplateCode = workflowTemplateCode;
        this.priority = priority != null ? priority : 0;
        this.enabled = enabled != null ? enabled : true;
    }

    // ── Getters and Setters ──

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
}
