package io.cerp.workflow.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * SagaAttachmentEntity – A photo or document attached as evidence to a saga as a whole (see
 * {@code SagaNoteEntity} for why there is no step-level scoping here).
 *
 * <p>
 * Stores only a reference to the uploaded blob ({@code objectKey}); the bytes go straight from
 * the browser to object storage via document-service's presign endpoint and are never touched by
 * this service.
 */
@Entity
@Table(name = "saga_attachments", schema = "workflow")
public class SagaAttachmentEntity {

    // Client-generated, not @GeneratedValue -- doubles as the idempotency key so a retried
    // "record attachment" call (e.g. from the offline upload queue after a dropped connection)
    // finds the already-stored row instead of duplicating it.
    @Id
    private UUID id;

    @Column(name = "saga_id", nullable = false)
    private String sagaId;

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

    protected SagaAttachmentEntity() {
    }

    public SagaAttachmentEntity(UUID id, String sagaId, UUID uploadedBy, AttachmentKind kind,
            String objectKey, String originalFileName, String contentType, Long byteSize) {
        this.id = id;
        this.sagaId = sagaId;
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

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
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
