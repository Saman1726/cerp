package io.cerp.vendor.dto;

import io.cerp.vendor.model.Vendor;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VendorResponse {
    private UUID id;
    private UUID tenantId;
    private String legalName;
    private String status;
    private Map<String, Object> taxProfile;
    private List<String> categories = new ArrayList<>();
    private String city;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static VendorResponse from(Vendor v) {
        VendorResponse r = new VendorResponse();
        r.setId(v.getId());
        r.setTenantId(v.getTenantId());
        r.setLegalName(v.getLegalName());
        r.setStatus(v.getStatus());
        r.setTaxProfile(v.getTaxProfile());
        r.setCategories(v.getCategories());
        r.setCity(v.getCity());
        r.setCreatedAt(v.getCreatedAt());
        r.setUpdatedAt(v.getUpdatedAt());
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

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getTaxProfile() {
        return taxProfile;
    }

    public void setTaxProfile(Map<String, Object> taxProfile) {
        this.taxProfile = taxProfile;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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
