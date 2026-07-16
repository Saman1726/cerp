package io.cerp.procurement.dto;

import io.cerp.procurement.model.ProcurementRequest;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ProcurementRequestResponse {
    private UUID id;
    private UUID tenantId;
    private String title;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProcurementRequestResponse from(ProcurementRequest pr) {
        ProcurementRequestResponse r = new ProcurementRequestResponse();
        r.setId(pr.getId());
        r.setTenantId(pr.getTenantId());
        r.setTitle(pr.getTitle());
        r.setDescription(pr.getDescription());
        r.setStatus(pr.getStatus());
        r.setCreatedAt(pr.getCreatedAt());
        r.setUpdatedAt(pr.getUpdatedAt());
        return r;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
