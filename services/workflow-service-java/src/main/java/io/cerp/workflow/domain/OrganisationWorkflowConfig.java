package io.cerp.workflow.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * OrganisationWorkflowConfig - Per-organization settings for workflow generation.
 *
 * <p>
 * Controls auto-generation behavior and allows custom template selection/overrides per organization within a tenant.
 */
@Entity
@Table(name = "organisation_workflow_config", schema = "workflow", uniqueConstraints = @UniqueConstraint(name = "uk_organisation_workflow_config", columnNames = {
        "tenant_id", "org_id" }), indexes = {
                @Index(name = "idx_owc_tenant", columnList = "tenant_id"),
                @Index(name = "idx_owc_org", columnList = "org_id")
        })
public class OrganisationWorkflowConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "auto_generate_on_subproject_creation", nullable = false)
    private Boolean autoGenerateOnSubprojectCreation = false;

    /** JSONB array of template codes to include. Empty means use all from mappings. */
    @Column(name = "selected_template_codes", columnDefinition = "jsonb")
    private String selectedTemplateCodesJson;

    /** JSONB object mapping phase types to building type groups to lists of template codes. */
    @Column(name = "custom_phase_mappings", columnDefinition = "jsonb")
    private String customPhaseMappingsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private transient ObjectMapper objectMapper;

    // ── Constructors ──

    public OrganisationWorkflowConfig() {
    }

    public OrganisationWorkflowConfig(String tenantId, UUID orgId) {
        this.tenantId = tenantId;
        this.orgId = orgId;
        this.autoGenerateOnSubprojectCreation = false;
        this.selectedTemplateCodesJson = "[]";
        this.customPhaseMappingsJson = "{}";
    }

    // ── Setter injection for ObjectMapper (Spring will provide it) ──

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // ── Business methods ──

    /** Enable auto-generation of workflows when subprojects are created. */
    public void enableAutoGeneration() {
        this.autoGenerateOnSubprojectCreation = true;
    }

    /** Disable auto-generation of workflows. */
    public void disableAutoGeneration() {
        this.autoGenerateOnSubprojectCreation = false;
    }

    /**
     * Set the list of selected template codes. Empty list means use all from mappings.
     */
    public void setSelectedTemplateCodes(List<String> templateCodes) {
        ObjectMapper mapper = getObjectMapper();
        if (mapper != null && templateCodes != null) {
            try {
                this.selectedTemplateCodesJson = mapper.writeValueAsString(templateCodes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize selected template codes", e);
            }
        }
    }

    /**
     * Get the list of selected template codes.
     */
    public List<String> getSelectedTemplateCodes() {
        ObjectMapper mapper = getObjectMapper();
        if (mapper != null && selectedTemplateCodesJson != null) {
            try {
                return mapper.readValue(selectedTemplateCodesJson, new TypeReference<List<String>>() {
                });
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    /**
     * Set custom phase mappings. Key is phase type, value is map of building type group to template codes.
     */
    public void setCustomPhaseMappings(Map<String, Map<String, List<String>>> customMappings) {
        ObjectMapper mapper = getObjectMapper();
        if (mapper != null && customMappings != null) {
            try {
                this.customPhaseMappingsJson = mapper.writeValueAsString(customMappings);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize custom phase mappings", e);
            }
        }
    }

    /**
     * Get custom phase mappings. Returns map of phase type -> building type group -> template codes.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, List<String>>> getCustomPhaseMappings() {
        ObjectMapper mapper = getObjectMapper();
        if (mapper != null && customPhaseMappingsJson != null) {
            try {
                TypeReference<Map<String, Map<String, List<String>>>> typeRef = new TypeReference<Map<String, Map<String, List<String>>>>() {
                };
                return mapper.readValue(customPhaseMappingsJson, typeRef);
            } catch (Exception e) {
                return Map.of();
            }
        }
        return Map.of();
    }

    /**
     * Add a single custom template mapping for a phase and building type group.
     */
    public void addCustomMapping(String phaseType, String buildingTypeGroup, String templateCode) {
        Map<String, Map<String, List<String>>> mappings = getCustomPhaseMappings();
        mappings.computeIfAbsent(phaseType, k -> Map.of())
            .computeIfAbsent(buildingTypeGroup, k -> new ArrayList<>())
            .add(templateCode);
        setCustomPhaseMappings(mappings);
    }

    // ── Pre-persist/update hooks ──

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public String getSelectedTemplateCodesJson() {
        return selectedTemplateCodesJson;
    }

    public void setSelectedTemplateCodesJson(String selectedTemplateCodesJson) {
        this.selectedTemplateCodesJson = selectedTemplateCodesJson;
    }

    public String getCustomPhaseMappingsJson() {
        return customPhaseMappingsJson;
    }

    public void setCustomPhaseMappingsJson(String customPhaseMappingsJson) {
        this.customPhaseMappingsJson = customPhaseMappingsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
