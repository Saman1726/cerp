package io.cerp.procurement.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial update for a ProcurementRequest. Any non-null field will be applied.
 */
public class UpdateProcurementRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    // Allowed statuses: draft | submitted | approved | ordered | closed
    @Size(max = 32)
    private String status;

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
}
