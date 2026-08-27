package io.cerp.project.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Tracks IFC files uploaded to a construction site and their rendering status.
 */
@Entity
@Table(name = "construction_site_ifc_files", schema = "project", indexes = {
        @Index(name = "idx_csifc_site_id", columnList = "construction_site_id"),
        @Index(name = "idx_csifc_file_id", columnList = "ifc_file_id")
})
public class ConstructionSiteIfcFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_site_id", nullable = false)
    private ConstructionSite constructionSite;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_key", length = 1024)
    private String fileKey;

    @Column(name = "ifc_file_id", length = 128)
    private String ifcFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IfcFileStatus status;

    @Column(name = "preview_url", length = 2048)
    private String previewUrl;

    @Column(name = "metadata_url", length = 2048)
    private String metadataUrl;

    @Column(name = "export_url", length = 2048)
    private String exportUrl;

    @Column(name = "blender_render_url", length = 2048)
    private String blenderRenderUrl;

    @Column(name = "blender_render_view", length = 32)
    private String blenderRenderView;

    @Column(name = "last_presigned_at")
    private LocalDateTime lastPresignedAt;

    @Column(name = "preview_object_key", length = 2048)
    private String previewObjectKey;

    @Column(name = "render_object_key", length = 2048)
    private String renderObjectKey;

    @Column(name = "export_object_key", length = 2048)
    private String exportObjectKey;

    @Column(name = "metadata_object_key", length = 2048)
    private String metadataObjectKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    protected ConstructionSiteIfcFile() {}

    public ConstructionSiteIfcFile(ConstructionSite constructionSite,
            String objectKey, String fileName, Long fileSize,
            String fileKey, String ifcFileId, String createdBy) {
        this.constructionSite = constructionSite;
        this.objectKey = objectKey;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileKey = fileKey;
        this.ifcFileId = ifcFileId;
        this.status = IfcFileStatus.QUEUED;
        this.createdBy = createdBy;
    }

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ConstructionSite getConstructionSite() { return constructionSite; }
    public void setConstructionSite(ConstructionSite constructionSite) { this.constructionSite = constructionSite; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }

    public String getIfcFileId() { return ifcFileId; }
    public void setIfcFileId(String ifcFileId) { this.ifcFileId = ifcFileId; }

    public IfcFileStatus getStatus() { return status; }
    public void setStatus(IfcFileStatus status) { this.status = status; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getMetadataUrl() { return metadataUrl; }
    public void setMetadataUrl(String metadataUrl) { this.metadataUrl = metadataUrl; }

    public String getExportUrl() { return exportUrl; }
    public void setExportUrl(String exportUrl) { this.exportUrl = exportUrl; }

    public String getBlenderRenderUrl() { return blenderRenderUrl; }
    public void setBlenderRenderUrl(String blenderRenderUrl) { this.blenderRenderUrl = blenderRenderUrl; }

    public String getBlenderRenderView() { return blenderRenderView; }
    public void setBlenderRenderView(String blenderRenderView) { this.blenderRenderView = blenderRenderView; }

    public LocalDateTime getLastPresignedAt() { return lastPresignedAt; }
    public void setLastPresignedAt(LocalDateTime lastPresignedAt) { this.lastPresignedAt = lastPresignedAt; }

    public String getPreviewObjectKey() { return previewObjectKey; }
    public void setPreviewObjectKey(String previewObjectKey) { this.previewObjectKey = previewObjectKey; }

    public String getRenderObjectKey() { return renderObjectKey; }
    public void setRenderObjectKey(String renderObjectKey) { this.renderObjectKey = renderObjectKey; }

    public String getExportObjectKey() { return exportObjectKey; }
    public void setExportObjectKey(String exportObjectKey) { this.exportObjectKey = exportObjectKey; }

    public String getMetadataObjectKey() { return metadataObjectKey; }
    public void setMetadataObjectKey(String metadataObjectKey) { this.metadataObjectKey = metadataObjectKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public enum IfcFileStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED
    }
}