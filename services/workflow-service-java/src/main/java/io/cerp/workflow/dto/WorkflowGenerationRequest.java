package io.cerp.workflow.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for generating workflows for a subproject.
 */
public class WorkflowGenerationRequest {

    @NotNull(message = "Subproject ID is required")
    private UUID subprojectId;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotNull(message = "Organization ID is required")
    private UUID orgId;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    /**
     * Building type of the subproject (e.g., RESIDENTIAL, COMMERCIAL, PUBLIC).
     */
    private String buildingType;

    /**
     * constructionPhase of the subproject (e.g., PRE-CONSTRUCTION, FOUNDATION, SITE_PREP).
     */
    private String constructionPhase;

    /**
     * Optional list of phase types to generate. If null, all matching templates are used.
     */
    private List<String> phaseTypes;

    /**
     * Optional parameters to pass to workflow instances during creation.
     */
    private Map<String, Object> workflowParameters;

    /**
     * User ID of the person who created the subproject (for workflow createdBy audit).
     */
    private String createdBy;

    // ── Constructors ──

    public WorkflowGenerationRequest() {
    }

    public WorkflowGenerationRequest(UUID subprojectId, String tenantId, UUID orgId,
            List<String> phaseTypes, Map<String, Object> workflowParameters, String constructionPhase) {
        this.subprojectId = subprojectId;
        this.tenantId = tenantId;
        this.orgId = orgId;
        this.phaseTypes = phaseTypes;
        this.workflowParameters = workflowParameters;
        this.constructionPhase = constructionPhase;
    }

    // ── Getters and Setters ──

    public UUID getSubprojectId() {
        return subprojectId;
    }

    public void setSubprojectId(UUID subprojectId) {
        this.subprojectId = subprojectId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public List<String> getPhaseTypes() {
        return phaseTypes;
    }

    public void setPhaseTypes(List<String> phaseTypes) {
        this.phaseTypes = phaseTypes;
    }

    public Map<String, Object> getWorkflowParameters() {
        return workflowParameters;
    }

    public void setWorkflowParameters(Map<String, Object> workflowParameters) {
        this.workflowParameters = workflowParameters;
    }

    public String getConstructionPhase() {
        return constructionPhase;
    }

    public void setConstructionPhase(String constructionPhase) {
        this.constructionPhase = constructionPhase;
    }

    /**
     * User ID of the person who created the subproject (for workflow createdBy audit).
     */
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
