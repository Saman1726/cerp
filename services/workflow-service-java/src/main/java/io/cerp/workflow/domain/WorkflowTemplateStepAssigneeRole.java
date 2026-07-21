package io.cerp.workflow.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Assignee role for a workflow template step.
 * 
 * <p>
 * Normalized from workflow_template_steps.assignee_roles TEXT[] into a separate table for better queryability and role management.
 * </p>
 */
@Entity
@Table(name = "workflow_template_step_assignee_roles")
public class WorkflowTemplateStepAssigneeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_step_id", nullable = false)
    private WorkflowTemplateStep templateStep;

    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    @Column(name = "role_order", nullable = false)
    private Integer roleOrder;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    // ── Constructors ──

    protected WorkflowTemplateStepAssigneeRole() {
    }

    public WorkflowTemplateStepAssigneeRole(WorkflowTemplateStep templateStep, String roleName, Integer roleOrder,
            Boolean isPrimary) {
        this.templateStep = templateStep;
        this.roleName = roleName;
        this.roleOrder = roleOrder;
        this.isPrimary = isPrimary;
    }

    // ── Getters and Setters ──

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public WorkflowTemplateStep getTemplateStep() {
        return templateStep;
    }

    public void setTemplateStep(WorkflowTemplateStep templateStep) {
        this.templateStep = templateStep;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getRoleOrder() {
        return roleOrder;
    }

    public void setRoleOrder(Integer roleOrder) {
        this.roleOrder = roleOrder;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
