package io.cerp.workflow.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * WorkflowStepAttachment – A photo or document attached as evidence to a WorkflowStep.
 *
 * <p>
 * Stores only a reference to the uploaded blob ({@code objectKey}); the bytes go straight from
 * the browser to object storage via document-service's presign endpoint and are never touched by
 * this service.
 */
@Entity
@Table(name = "workflow_step_attachments", schema = "workflow")
public class WorkflowStepAttachment {

    // Client-generated, not @GeneratedValue -- doubles as the idempotency key so a retried
    // "record attachment" call (e.g. from the offline upload queue after a dropped connection)
    // finds the already-stored row instead of duplicating it.
    @Id
    private UUID id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "step_id")
    private WorkflowStep step;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private AttachmentKind kind;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private Long byteSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ──

    public enum AttachmentKind {
        PHOTO, DOCUMENT
    }

    // ── Constructors ──

    protected WorkflowStepAttachment() {
    }

    public WorkflowStepAttachment(UUID id, Workflow workflow, WorkflowStep step, UUID uploadedBy,
            AttachmentKind kind, String objectKey, String originalFileName, String contentType,
            Long byteSize) {
        this.id = id;
        this.workflow = workflow;
        this.step = step;
        this.uploadedBy = uploadedBy;
        this.kind = kind;
        this.objectKey = objectKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.byteSize = byteSize;
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

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public AttachmentKind getKind() {
        return kind;
    }

    public void setKind(AttachmentKind kind) {
        this.kind = kind;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getByteSize() {
        return byteSize;
    }

    public void setByteSize(Long byteSize) {
        this.byteSize = byteSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
