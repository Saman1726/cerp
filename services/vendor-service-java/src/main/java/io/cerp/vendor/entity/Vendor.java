package io.cerp.vendor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendors", schema = "supply")
public class Vendor {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "website")
    private String website;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "status", nullable = false)
    private String status = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_profile", columnDefinition = "jsonb")
    private Object taxProfile;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "categories", nullable = false, columnDefinition = "text[]")
    private List<String> categories;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Vendor() {
    }

    @PrePersist
    public void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = "pending";
        }
        if (this.categories == null) {
            this.categories = List.of();
        }
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Vendor setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Vendor setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public UUID getUserId() {
        return userId;
    }

    public Vendor setUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public String getLegalName() {
        return legalName;
    }

    public Vendor setLegalName(String legalName) {
        this.legalName = legalName;
        return this;
    }

    public String getContactName() {
        return contactName;
    }

    public Vendor setContactName(String contactName) {
        this.contactName = contactName;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public Vendor setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWebsite() {
        return website;
    }

    public Vendor setWebsite(String website) {
        this.website = website;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public Vendor setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getCity() {
        return city;
    }

    public Vendor setCity(String city) {
        this.city = city;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public Vendor setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Vendor setStatus(String status) {
        this.status = status;
        return this;
    }

    public Object getTaxProfile() {
        return taxProfile;
    }

    public Vendor setTaxProfile(Object taxProfile) {
        this.taxProfile = taxProfile;
        return this;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Vendor setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Vendor setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Vendor setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
