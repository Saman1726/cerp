package io.cerp.workflow.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for organisation workflow configurations.
 */
public class OrganisationWorkflowConfigResponse {

    private UUID id;
    private String tenantId;
    private UUID orgId;
    private Boolean autoGenerateOnSubprojectCreation;
    private List<String> selectedTemplateCodes;
    private Map<String, Map<String, List<String>>> customPhaseMappings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ──

    public OrganisationWorkflowConfigResponse() {
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

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public Boolean getAutoGenerateOnSubprojectCreation() {
        return autoGenerateOnSubprojectCreation;
    }

    public void setAutoGenerateOnSubprojectCreation(Boolean autoGenerateOnSubprojectCreation) {
        this.autoGenerateOnSubprojectCreation = autoGenerateOnSubprojectCreation;
    }

    public List<String> getSelectedTemplateCodes() {
        return selectedTemplateCodes;
    }

    public void setSelectedTemplateCodes(List<String> selectedTemplateCodes) {
        this.selectedTemplateCodes = selectedTemplateCodes;
    }

    public Map<String, Map<String, List<String>>> getCustomPhaseMappings() {
        return customPhaseMappings;
    }

    public void setCustomPhaseMappings(Map<String, Map<String, List<String>>> customPhaseMappings) {
        this.customPhaseMappings = customPhaseMappings;
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
