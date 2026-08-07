package io.cerp.workflow.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Workflow – Runtime instance for a specific SubProject (building).
 *
 * <p>
 * Instantiated from a WorkflowTemplate. Each workflow is scoped to a tenant/project/subproject triad.
 */
@Entity
@Table(name = "workflows", schema = "workflow", indexes = {
        @Index(name = "idx_workflow_tenant_project_subproject", columnList = "tenant_id, project_id, subproject_id"),
        @Index(name = "idx_workflow_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_workflow_template", columnList = "template_id")
})
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "subproject_id", nullable = false)
    private UUID subprojectId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_version", nullable = false)
    private Integer templateVersion;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WorkflowStatus status = WorkflowStatus.PLANNED;

    @Column(name = "next_step_id")
    private UUID nextStepId;

    @Column(name = "who_has_done")
    private UUID whoHasDone;

    @Column(name = "who_has_approved")
    private UUID whoHasApproved;

    /**
     * Scope level for this workflow instance. - WHOLE: workflow applies to the entire building/SubProject - FLOOR: workflow applies to specific floors within the
     * SubProject - PART: workflow applies to specific building parts within the SubProject
     */
    @Column(name = "scope_level", nullable = false, length = 16)
    private String scopeLevel = "WHOLE";

    /**
     * Floor identifiers for FLOOR scope level workflows. Examples: "F1", "F2", "GROUND", "FIRST", etc.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "workflow_floor_ids",
        joinColumns = @JoinColumn(name = "workflow_id"),
        schema = "workflow"
    )
    @Column(name = "floor_id", length = 64)
    private List<String> floorIds = new ArrayList<>();

    /**
     * Building part identifiers for PART scope level workflows. Examples: "facade", "basement", "roof", "elevator_shaft", "hvac_room", etc.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "workflow_part_ids",
        joinColumns = @JoinColumn(name = "workflow_id"),
        schema = "workflow"
    )
    @Column(name = "part_id", length = 64)
    private List<String> partIds = new ArrayList<>();

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @JsonManagedReference
    @OneToMany(mappedBy = "workflow", orphanRemoval = true)
    private List<WorkflowStep> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Enums ──

    public enum WorkflowStatus {
        PLANNED, IN_PROGRESS, BLOCKED, ON_HOLD, COMPLETED, APPROVED, REJECTED, CANCELED
    }

    // ── Constructors ──

    protected Workflow() {
    }

    public Workflow(String tenantId, UUID projectId, UUID subprojectId, UUID templateId,
            Integer templateVersion, String code, String name) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.subprojectId = subprojectId;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.code = code;
        this.name = name;
        this.creationDate = LocalDate.now();
    }

    // ── Business methods ──

    /**
     * Side effects applied by each status transition.
     *
     * <p>
     * Status is set exclusively by {@code WorkflowStateStore}, driven by
     * {@code StateMachineEngine.executeTransition(...)} in {@code WorkflowService} -- see
     * {@code WorkflowStateMachines} for the guarded transition graph this class used to enforce
     * itself. These methods only apply the side effects (timestamps) that accompany each
     * transition; several have none.
     */
    public void start() {
        this.startDate = LocalDate.now();
    }

    public void block() {
    }

    public void unblock() {
    }

    public void hold() {
    }

    public void unhold() {
    }

    public void complete() {
        this.endDate = LocalDate.now();
    }

    public void approve() {
    }

    public void reject() {
    }

    public void cancel() {
        this.endDate = LocalDate.now();
    }

    /**
     * Recalculate progress based on completed step weights.
     *
     * <p>
     * progressPercent = (sum of completed step weights / total step weights) * 100
     */
    public void recalculateProgress() {
        if (steps.isEmpty()) {
            this.progressPercent = BigDecimal.ZERO;
            return;
        }

        BigDecimal totalWeight = steps.stream()
            .map(WorkflowStep::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal completedWeight = steps.stream()
            .filter(s -> s.getStatus() == WorkflowStep.StepStatus.COMPLETED
                    || s.getStatus() == WorkflowStep.StepStatus.APPROVED)
            .map(WorkflowStep::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            this.progressPercent = BigDecimal.ZERO;
        } else {
            this.progressPercent = completedWeight.divide(totalWeight, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    public void setNextStep(UUID stepId) {
        this.nextStepId = stepId;
    }

    public void setWhoHasDone(UUID userId) {
        this.whoHasDone = userId;
    }

    public void setWhoHasApproved(UUID userId) {
        this.whoHasApproved = userId;
    }

    // ── Step management ──

    public void addStep(WorkflowStep step) {
        steps.add(step);
        step.setWorkflow(this);
    }

    /**
     * Add a floor ID to this workflow's floor list. Used when scopeLevel = FLOOR.
     */
    public void addFloorId(String floorId) {
        if (!this.floorIds.contains(floorId)) {
            this.floorIds.add(floorId);
        }
    }

    /**
     * Remove a floor ID from this workflow's floor list.
     */
    public void removeFloorId(String floorId) {
        this.floorIds.remove(floorId);
    }

    /**
     * Add a building part ID to this workflow's part list. Used when scopeLevel = PART.
     */
    public void addPartId(String partId) {
        if (!this.partIds.contains(partId)) {
            this.partIds.add(partId);
        }
    }

    /**
     * Remove a building part ID from this workflow's part list.
     */
    public void removePartId(String partId) {
        this.partIds.remove(partId);
    }

    /**
     * Set the scope level and update applicable fields. - WHOLE: clears floorIds and partIds - FLOOR: requires floorIds to be set - PART: requires partIds to be set
     */
    public void setScopeLevel(String scopeLevel) {
        this.scopeLevel = scopeLevel;
        if ("WHOLE".equals(scopeLevel)) {
            this.floorIds.clear();
            this.partIds.clear();
        }
    }

    public WorkflowStep findNextPendingStep() {
        return steps.stream()
            .filter(s -> s.getStatus() == WorkflowStep.StepStatus.NOT_STARTED)
            .min(java.util.Comparator.comparingInt(WorkflowStep::getStepOrder))
            .orElse(null);
    }

    public List<WorkflowStep> getSteps() {
        return steps;
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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getSubprojectId() {
        return subprojectId;
    }

    public void setSubprojectId(UUID subprojectId) {
        this.subprojectId = subprojectId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public Integer getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(Integer templateVersion) {
        this.templateVersion = templateVersion;
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

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    /**
     * Bypasses the guarded transition graph -- used internally by {@code WorkflowStateStore} to
     * write back the state {@code StateMachineEngine} already validated. Do not call directly to
     * change status; use {@code WorkflowService}'s transition methods instead.
     */
    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public UUID getNextStepId() {
        return nextStepId;
    }

    public UUID getWhoHasDone() {
        return whoHasDone;
    }

    public UUID getWhoHasApproved() {
        return whoHasApproved;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getScopeLevel() {
        return scopeLevel;
    }

    public List<String> getFloorIds() {
        return floorIds;
    }

    public void setFloorIds(List<String> floorIds) {
        this.floorIds = floorIds != null ? new ArrayList<>(floorIds) : new ArrayList<>();
    }

    public List<String> getPartIds() {
        return partIds;
    }

    public void setPartIds(List<String> partIds) {
        this.partIds = partIds != null ? new ArrayList<>(partIds) : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
