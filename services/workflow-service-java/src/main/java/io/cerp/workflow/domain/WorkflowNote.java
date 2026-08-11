package io.cerp.workflow.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
 * WorkflowNote – A log entry attached to a workflow or step.
 */
@Entity
@Table(name = "workflow_notes", schema = "workflow")
public class WorkflowNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @JsonIgnore
    @ManyToOne(optional = true)
    @JoinColumn(name = "step_id")
    private WorkflowStep step;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "note", nullable = false, columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 16)
    private NoteType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ──

    public enum NoteType {
        GENERAL, RISK, ISSUE, DECISION
    }

    // ── Constructors ──

    protected WorkflowNote() {
    }

    public WorkflowNote(Workflow workflow, WorkflowStep step, UUID authorId, String note, NoteType type) {
        this.workflow = workflow;
        this.step = step;
        this.authorId = authorId;
        this.note = note;
        this.type = type;
    }

    // ── Pre-persist hook ──

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public WorkflowStep getStep() {
        return step;
    }

    public void setStep(WorkflowStep step) {
        this.step = step;
    }

    /**
     * Exposes just the step id (not the full, {@code @JsonIgnore}'d step entity) so API
     * responses can tell which notes are step-scoped -- null for a workflow-level note.
     *
     * <p>
     * Deliberately NOT named {@code getStepId()}: Spring Data's query derivation for
     * {@code WorkflowNoteRepository.findByWorkflowIdAndStepIdOrderByCreatedAtDesc} resolves
     * "StepId" against this class's bean properties before falling back to nested traversal
     * (i.e. {@code step.id}) -- a same-named {@code stepId} property short-circuits that
     * fallback and Spring Data tries to resolve it as a direct (non-existent) JPA attribute
     * instead, which fails at startup. {@code @JsonProperty} keeps the JSON field named
     * "stepId" for API consumers without exposing a same-named Java bean property.
     */
    @JsonProperty("stepId")
    public UUID getLinkedStepId() {
        return step != null ? step.getId() : null;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public NoteType getType() {
        return type;
    }

    public void setType(NoteType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
