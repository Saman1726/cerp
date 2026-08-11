package io.cerp.document.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_captures")
public class SiteCapture {

    // The client-generated id doubles as the primary key, not a separate
    // server-assigned one - that's what makes a resubmission of the same
    // capture (e.g. after a dropped connection) idempotent, see
    // DocumentService.submit().
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "sub_project_id")
    private UUID subProjectId;

    @Column(nullable = false)
    private String kind;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SiteCapture() {
        // JPA
    }

    public SiteCapture(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID subProjectId,
            String kind,
            String notes,
            Instant capturedAt,
            String objectKey,
            String originalFileName,
            String contentType,
            long byteSize) {
        this.id = id;
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.subProjectId = subProjectId;
        this.kind = kind;
        this.notes = notes;
        this.capturedAt = capturedAt;
        this.objectKey = objectKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.status = "RECEIVED";
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }
}
