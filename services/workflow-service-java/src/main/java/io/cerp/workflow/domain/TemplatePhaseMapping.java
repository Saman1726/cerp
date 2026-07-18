package io.cerp.workflow.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * TemplatePhaseMapping – Maps workflow templates to construction phases by building type group.
 *
 * <p>
 * This allows per-tenant, per-org configuration of which templates apply to which phases. Building type groups:
 * <ul>
 * <li>RESIDENTIAL_TYPE: RESIDENTIAL, COMMERCIAL, INDUSTRIAL, MIXED_USE</li>
 * <li>PUBLIC_TYPE: PUBLIC, PARKING, UTILITY, INFRASTRUCTURE, OTHER</li>
 * </ul>
 */
@Entity
@Table(name = "template_phase_mappings", uniqueConstraints = @UniqueConstraint(name = "uk_template_phase_mapping", columnNames = {
        "tenant_id", "phase_type", "building_type_group", "workflow_template_code" }), indexes = {
                @Index(name = "idx_template_phase_mapping_tenant", columnList = "tenant_id"),
                @Index(name = "idx_template_phase_mapping_phase", columnList = "phase_type"),
                @Index(name = "idx_template_phase_mapping_template", columnList = "workflow_template_code")
        })
public class TemplatePhaseMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "phase_type", nullable = false, length = 64)
    private String phaseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "building_type_group", nullable = false, length = 32)
    private BuildingTypeGroup buildingTypeGroup;

    @Column(name = "workflow_template_code", nullable = false, length = 64)
    private String workflowTemplateCode;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Enums ──

    public enum BuildingTypeGroup {
        /** RESIDENTIAL, COMMERCIAL, INDUSTRIAL, MIXED_USE */
        RESIDENTIAL_TYPE,
        /** PUBLIC, PARKING, UTILITY, INFRASTRUCTURE, OTHER */
        PUBLIC_TYPE
    }

    // ── Constructors ──

    public TemplatePhaseMapping() {
    }

    public TemplatePhaseMapping(String tenantId, String phaseType, BuildingTypeGroup buildingTypeGroup,
            String workflowTemplateCode, Integer priority) {
        this.tenantId = tenantId;
        this.phaseType = phaseType;
        this.buildingTypeGroup = buildingTypeGroup;
        this.workflowTemplateCode = workflowTemplateCode;
        this.priority = priority != null ? priority : 0;
        this.enabled = true;
    }

    // ── Business methods ──

    /** Toggle the enabled flag. */
    public void enable() {
        this.enabled = true;
    }

    /** Disable this mapping so it won't be used for generation. */
    public void disable() {
        this.enabled = false;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
