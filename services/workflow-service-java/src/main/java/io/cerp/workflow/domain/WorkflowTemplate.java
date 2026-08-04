package io.cerp.workflow.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.UniqueConstraint;

/**
 * WorkflowTemplate – Canonical definition for a construction phase workflow.
 *
 * <p>
 * Used to instantiate Workflow instances per SubProject. One template is reusable across Projects within a Tenant.
 */
@Entity
@Table(name = "workflow_templates", schema = "workflow", uniqueConstraints = @UniqueConstraint(name = "uk_template_tenant_code_version", columnNames = { "tenant_id", "code",
        "version" }), indexes = {
                @Index(name = "idx_template_tenant_code", columnList = "tenant_id, code"),
                @Index(name = "idx_template_status", columnList = "status"),
                @Index(name = "idx_template_phase_type", columnList = "phase_type")
        })
public class WorkflowTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "phase_type", nullable = false, length = 64)
    private String phaseType;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "parameters_schema")
    private String parametersSchema;

    @Column(name = "default_sla")
    private String defaultSla;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(
        name = "workflow_template_required_documents",
        joinColumns = @JoinColumn(name = "workflow_template_id"),
        schema = "workflow"
    )
    @Column(name = "required_document", length = 64)
    private List<String> requiredDocuments = new ArrayList<>();

    /**
     * Applicable scope levels for this template. - WHOLE: template applies to whole buildings/SubProjects - FLOOR: template applies to specific floors within a
     * SubProject - PART: template applies to specific building parts within a SubProject
     */
    @ElementCollection(fetch = jakarta.persistence.FetchType.LAZY)
    @BatchSize(size = 20)
    @CollectionTable(
        name = "workflow_template_applicable_scope_levels",
        joinColumns = @JoinColumn(name = "workflow_template_id"),
        schema = "workflow"
    )
    @Column(name = "applicable_scope_level", length = 16)
    private List<String> applicableScopeLevels = new ArrayList<>();

    /**
     * Default scope level for this template when instantiating workflows. - WHOLE: workflow applies to the entire building - FLOOR: workflow applies to specific floors
     * (floorIds must be provided at instantiation) - PART: workflow applies to specific building parts (partIds must be provided at instantiation)
     */
    @Column(name = "default_scope_level", length = 16)
    private String defaultScopeLevel = "WHOLE";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TemplateStatus status = TemplateStatus.DRAFT;

    /**
     * Order position of this workflow template in the construction sequence (1-23 per Section 23 of architecture docs). Used to display templates in the correct
     * construction phase order.
     */
    @Column(name = "order_num")
    private Integer orderNum;

    @JsonManagedReference("template-steps")
    @OneToMany(mappedBy = "template", orphanRemoval = true)
    private List<WorkflowTemplateStep> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Enums ──

    public enum TemplateStatus {
        DRAFT, ACTIVE, DEPRECATED
    }

    // ── Constructors ──

    public WorkflowTemplate() {
    }

    public WorkflowTemplate(String tenantId, String code, String name, String phaseType) {
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.phaseType = phaseType;
        this.status = TemplateStatus.DRAFT;
        this.version = 1;
    }

    // ── Business methods ──

    public void activate() {
        if (this.status != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT templates can be activated");
        }
        this.status = TemplateStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status != TemplateStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE templates can be deprecated");
        }
        this.status = TemplateStatus.DEPRECATED;
    }

    /** Alias for deactivate to match the domain language. */
    public void deprecate() {
        deactivate();
    }

    /** Update mutable template fields (only DRAFT templates can be updated). */
    public void update(String name, String description, String phaseType, List<String> requiredDocuments) {
        update(name, description, phaseType, requiredDocuments, null, null);
    }

    /**
     * Update mutable template fields with scope level support. Only DRAFT templates can be updated.
     */
    public void update(String name, String description, String phaseType, List<String> requiredDocuments,
            List<String> applicableScopeLevels, String defaultScopeLevel) {
        update(name, description, phaseType, requiredDocuments, applicableScopeLevels, defaultScopeLevel, null);
    }

    /**
     * Update mutable template fields with scope level and order support. Only DRAFT templates can be updated.
     */
    public void update(String name, String description, String phaseType, List<String> requiredDocuments,
            List<String> applicableScopeLevels, String defaultScopeLevel, Integer orderNum) {
        if (this.status != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT templates can be updated");
        }
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (phaseType != null) {
            this.phaseType = phaseType;
        }
        if (requiredDocuments != null) {
            this.requiredDocuments = requiredDocuments;
        }
        if (applicableScopeLevels != null) {
            this.applicableScopeLevels = applicableScopeLevels;
        }
        if (defaultScopeLevel != null) {
            this.defaultScopeLevel = defaultScopeLevel;
        }
        if (orderNum != null) {
            this.orderNum = orderNum;
        }
    }

    public void cloneTo(String newTenantId, String newCode) {
        this.version++;
        this.tenantId = newTenantId;
        this.code = newCode;
        this.status = TemplateStatus.DRAFT;
    }

    public void addStep(WorkflowTemplateStep step) {
        steps.add(step);
        step.setTemplate(this);
    }

    public void removeStep(UUID stepId) {
        steps.removeIf(s -> s.getId().equals(stepId));
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhaseType() {
        return phaseType;
    }

    public void setPhaseType(String phaseType) {
        this.phaseType = phaseType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getParametersSchema() {
        return parametersSchema;
    }

    public void setParametersSchema(String parametersSchema) {
        this.parametersSchema = parametersSchema;
    }

    public String getDefaultSla() {
        return defaultSla;
    }

    public void setDefaultSla(String defaultSla) {
        this.defaultSla = defaultSla;
    }

    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(List<String> requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
    }

    public List<WorkflowTemplateStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowTemplateStep> steps) {
        this.steps = steps;
    }

    public List<String> getApplicableScopeLevels() {
        return applicableScopeLevels;
    }

    public void setApplicableScopeLevels(List<String> applicableScopeLevels) {
        this.applicableScopeLevels = applicableScopeLevels;
    }

    public String getDefaultScopeLevel() {
        return defaultScopeLevel;
    }

    public void setDefaultScopeLevel(String defaultScopeLevel) {
        this.defaultScopeLevel = defaultScopeLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }
}
