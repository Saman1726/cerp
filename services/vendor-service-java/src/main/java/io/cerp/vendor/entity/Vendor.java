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
@Table(name = "vendors", schema = "vendor")
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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "vendor_types", nullable = false, columnDefinition = "text[] default '{}'")
    private List<String> vendorTypes = List.of();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "category_group_ids", nullable = false, columnDefinition = "uuid[] default '{}'")
    private List<UUID> categoryGroupIds = List.of();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "capability_ids", nullable = false, columnDefinition = "uuid[] default '{}'")
    private List<UUID> capabilityIds = List.of();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "project_types", nullable = false, columnDefinition = "text[] default '{}'")
    private List<String> projectTypes = List.of();

    @Column(name = "years_in_business")
    private Integer yearsInBusiness;

    @Column(name = "description")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "registration_number")
    private String registrationNumber;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "regions", nullable = false, columnDefinition = "text[] default '{}'")
    private List<String> regions = List.of();

    @Column(name = "status", nullable = false)
    private String status = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_profile", columnDefinition = "jsonb")
    private Object taxProfile;

    /**
     * @deprecated Use {@link #categoryGroupIds} and {@link #capabilityIds} instead.
     *             This field is retained for backward compatibility.
     */
    @Deprecated(forRemoval = true)
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
        if (this.vendorTypes == null) {
            this.vendorTypes = List.of();
        }
        if (this.categoryGroupIds == null) {
            this.categoryGroupIds = List.of();
        }
        if (this.capabilityIds == null) {
            this.capabilityIds = List.of();
        }
        if (this.projectTypes == null) {
            this.projectTypes = List.of();
        }
        if (this.regions == null) {
            this.regions = List.of();
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

    public List<String> getVendorTypes() {
        return vendorTypes;
    }

    public Vendor setVendorTypes(List<String> vendorTypes) {
        this.vendorTypes = vendorTypes != null ? vendorTypes : List.of();
        return this;
    }

    public List<UUID> getCategoryGroupIds() {
        return categoryGroupIds;
    }

    public Vendor setCategoryGroupIds(List<UUID> categoryGroupIds) {
        this.categoryGroupIds = categoryGroupIds != null ? categoryGroupIds : List.of();
        return this;
    }

    public List<UUID> getCapabilityIds() {
        return capabilityIds;
    }

    public Vendor setCapabilityIds(List<UUID> capabilityIds) {
        this.capabilityIds = capabilityIds != null ? capabilityIds : List.of();
        return this;
    }

    public List<String> getProjectTypes() {
        return projectTypes;
    }

    public Vendor setProjectTypes(List<String> projectTypes) {
        this.projectTypes = projectTypes != null ? projectTypes : List.of();
        return this;
    }

    public Integer getYearsInBusiness() {
        return yearsInBusiness;
    }

    public Vendor setYearsInBusiness(Integer yearsInBusiness) {
        this.yearsInBusiness = yearsInBusiness;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Vendor setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public Vendor setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public Vendor setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
        return this;
    }

    public List<String> getRegions() {
        return regions;
    }

    public Vendor setRegions(List<String> regions) {
        this.regions = regions != null ? regions : List.of();
        return this;
    }

    /**
     * @deprecated Use {@link #getCategoryGroupIds()} or {@link #getCapabilityIds()} instead.
     */
    @Deprecated(forRemoval = true)
    public List<String> getCategories() {
        return categories;
    }

    /**
     * @deprecated Use {@link #setCategoryGroupIds(List)} and {@link #setCapabilityIds(List)} instead.
     */
    @Deprecated(forRemoval = true)
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
