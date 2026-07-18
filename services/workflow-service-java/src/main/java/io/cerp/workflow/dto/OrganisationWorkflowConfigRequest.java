package io.cerp.workflow.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating organisation workflow configuration.
 */
public class OrganisationWorkflowConfigRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotNull(message = "Organization ID is required")
    private String orgId;

    private Boolean autoGenerateOnSubprojectCreation = false;

    private List<String> selectedTemplateCodes;

    /** Map of phase type -> building type group -> template codes */
    private Map<String, Map<String, List<String>>> customPhaseMappings;

    // ── Constructors ──

    public OrganisationWorkflowConfigRequest() {
    }

    public OrganisationWorkflowConfigRequest(String tenantId, String orgId, Boolean autoGenerateOnSubprojectCreation,
            List<String> selectedTemplateCodes, Map<String, Map<String, List<String>>> customPhaseMappings) {
        this.tenantId = tenantId;
        this.orgId = orgId;
        this.autoGenerateOnSubprojectCreation = autoGenerateOnSubprojectCreation;
        this.selectedTemplateCodes = selectedTemplateCodes;
        this.customPhaseMappings = customPhaseMappings;
    }

    // ── Getters and Setters ──

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
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
}
