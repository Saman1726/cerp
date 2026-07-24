package io.cerp.workflow.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * WorkflowStep – A single step instance within a Workflow.
 */
@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_id")
    @JsonBackReference
    private Workflow workflow;

    @Column(name = "template_step_id")
    private UUID templateStepId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StepStatus status = StepStatus.NOT_STARTED;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "assignee_role", length = 64)
    private String assigneeRole;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "evidence")
    @JsonInclude(Include.NON_NULL)
    private String evidence;

    @Column(name = "notes", columnDefinition = "TEXT")
    @JsonInclude(Include.NON_NULL)
    private String notes;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    // ── Enums ──

    public enum StepStatus {
        NOT_STARTED, IN_PROGRESS, BLOCKED, COMPLETED, APPROVED, REJECTED
    }

    // ── Constructors ──

    protected WorkflowStep() {
    }

    public WorkflowStep(UUID templateStepId, Integer stepOrder, String code, String name) {
        this.templateStepId = templateStepId;
        this.stepOrder = stepOrder;
        this.code = code;
        this.name = name;
    }

    // ── Business methods ──

    public void start(UUID userId) {
        requireStatus(StepStatus.NOT_STARTED);
        this.status = StepStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        if (userId != null) {
            this.assigneeUserId = userId;
        }
    }

    public void complete(String evidence, String notes, UUID userId) {
        requireStatus(StepStatus.IN_PROGRESS);
        this.status = StepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.evidence = evidence;
        this.notes = notes;
        if (userId != null) {
            this.assigneeUserId = userId;
        }
    }

    public void approve(String notes, UUID userId) {
        requireStatus(StepStatus.COMPLETED);
        this.status = StepStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = userId;
        if (notes != null && this.notes != null) {
            this.notes = this.notes + "\n[Approved: " + notes + "]";
        } else if (notes != null) {
            this.notes = notes;
        }
    }

    public void reject(String notes, UUID userId) {
        requireStatus(StepStatus.COMPLETED);
        this.status = StepStatus.REJECTED;
        this.completedAt = LocalDateTime.now();
        this.notes = notes;
        if (userId != null) {
            this.approvedBy = userId;
        }
    }

    public void block(String reason) {
        requireStatus(StepStatus.IN_PROGRESS);
        this.status = StepStatus.BLOCKED;
        if (this.notes == null) {
            this.notes = reason;
        } else {
            this.notes = this.notes + "\n[Blocked: " + reason + "]";
        }
    }

    public void unblock() {
        requireStatus(StepStatus.BLOCKED);
        this.status = StepStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    private void requireStatus(StepStatus... allowed) {
        for (StepStatus s : allowed) {
            if (this.status == s) {
                return;
            }
        }
        String allowedStr = String.join(", ", java.util.Arrays.toString(allowed));
        throw new IllegalStateException(
            String.format("Step status %s does not allow this action. Allowed from: %s", this.status, allowedStr));
    }

    // ── Getters and Setters ──

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public UUID getTemplateStepId() {
        return templateStepId;
    }

    public void setTemplateStepId(UUID templateStepId) {
        this.templateStepId = templateStepId;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
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

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(UUID assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getAssigneeRole() {
        return assigneeRole;
    }

    public void setAssigneeRole(String assigneeRole) {
        this.assigneeRole = assigneeRole;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
}
