package io.cerp.workflow.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * SagaNoteEntity – A note attached to a saga as a whole.
 *
 * <p>
 * Sagas have no individually-addressable step records (a saga's "steps" are a lossily
 * (de)serialized JSON array with no id column - see {@code SagaStep} and
 * {@code DefaultSagaOrchestrator}'s (de)serializeSteps), so unlike {@code WorkflowNote} there is
 * no optional step-level scoping here - every note is saga-level, the direct analog of a
 * {@code WorkflowNote} with a null step.
 *
 * <p>
 * {@code sagaId} is a plain scalar column (not a {@code @ManyToOne}) matching
 * {@code SagaEntity}'s own flat, relationship-free style - nothing here needs to navigate an
 * object graph, every access starts from a known sagaId path parameter.
 */
@Entity
@Table(name = "saga_notes", schema = "workflow")
public class SagaNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "saga_id", nullable = false)
    private String sagaId;

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

    protected SagaNoteEntity() {
    }

    public SagaNoteEntity(String sagaId, UUID authorId, String note, NoteType type) {
        this.sagaId = sagaId;
        this.authorId = authorId;
        this.note = note;
        this.type = type;
    }

    // ── Pre-persist hook ──

    @PrePersist
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

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
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
