package io.cerp.workflow.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
 * StepTemplate – A single step definition within a WorkflowTemplate.
 */
@Entity
@Table(name = "workflow_template_steps")
public class WorkflowTemplateStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id")
    private WorkflowTemplate template;

    @Column(name = "step_order", nullable = false)
    private Integer order;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 16)
    private StepType type;

    @ElementCollection
    @Column(name = "assignee_roles", length = 64)
    private List<String> assigneeRoles = new ArrayList<>();

    @Column(name = "requires_evidence", nullable = false)
    private Boolean requiresEvidence = false;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    @Column(name = "due_offset_days")
    private Integer dueOffsetDays;

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "checklists")
    @JsonInclude(Include.NON_NULL)
    private String checklists;

    @Column(name = "gating_condition")
    @JsonInclude(Include.NON_NULL)
    private String gatingCondition;

    // ── Enums ──

    public enum StepType {
        /** An actionable task to be completed */
        TASK,
        /** An inspection requiring evidence and possibly approval */
        INSPECTION,
        /** Requires formal approval by a designated role */
        APPROVAL,
        /** A key milestone marking completion of a significant phase */
        MILESTONE
    }

    // ── Constructors ──

    protected WorkflowTemplateStep() {
    }

    public WorkflowTemplateStep(Integer order, String code, String name, StepType type) {
        this.order = order;
        this.code = code;
        this.name = name;
        this.type = type;
    }

    // ── Getters and Setters ──

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public WorkflowTemplate getTemplate() {
        return template;
    }

    public void setTemplate(WorkflowTemplate template) {
        this.template = template;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
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

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public List<String> getAssigneeRoles() {
        return assigneeRoles;
    }

    public void setAssigneeRoles(List<String> assigneeRoles) {
        this.assigneeRoles = assigneeRoles;
    }

    public Boolean getRequiresEvidence() {
        return requiresEvidence;
    }

    public void setRequiresEvidence(Boolean requiresEvidence) {
        this.requiresEvidence = requiresEvidence;
    }

    public Boolean getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public Integer getDueOffsetDays() {
        return dueOffsetDays;
    }

    public void setDueOffsetDays(Integer dueOffsetDays) {
        this.dueOffsetDays = dueOffsetDays;
    }

    public Integer getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(Integer slaHours) {
        this.slaHours = slaHours;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getChecklists() {
        return checklists;
    }

    public void setChecklists(String checklists) {
        this.checklists = checklists;
    }

    public String getGatingCondition() {
        return gatingCondition;
    }

    public void setGatingCondition(String gatingCondition) {
        this.gatingCondition = gatingCondition;
    }
}
