package io.cerp.organization.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Organization entity representing a business organization profile.
 *
 * <p>
 * Each organization is scoped to a tenant (tenant_id) for row-level security. An organization is the primary business entity that contains properties, partners, and
 * users in the CERP system.
 *
 * <p>
 * Per Section 4 (Business Concept): Organizations are the root of the business hierarchy and own properties and contracts.
 */
@Entity
@Table(name = "organizations", indexes = {
        @Index(name = "idx_organizations_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_organizations_external_id", columnList = "external_id", unique = true),
        @Index(name = "idx_organizations_name", columnList = "name"),
        @Index(name = "idx_organizations_status", columnList = "status")
})
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant identifier. Used for RLS and tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * External system ID (for integration with external ERP systems). Nullable — only set if this organization was synced from an external system.
     */
    @Column(name = "external_id", length = 128)
    private String externalId;

    /**
     * Organization name (e.g., "Acme Construction GmbH").
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Legal form (e.g., "GmbH", "AG", "Inc.", "LLC").
     */
    @Column(name = "legal_form", length = 64)
    private String legalForm;

    /**
     * Tax/VAT identifier.
     */
    @Column(name = "tax_id", length = 64)
    private String taxId;

    /**
     * Commercial register number (Handelsregisternummer).
     */
    @Column(name = "commercial_register", length = 64)
    private String commercialRegister;

    /**
     * Primary address line 1.
     */
    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    /**
     * Primary address line 2.
     */
    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    /**
     * Postal code.
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * City.
     */
    @Column(name = "city", length = 128)
    private String city;

    /**
     * Country code (ISO 3166-1 alpha-2, e.g., "DE", "US").
     */
    @Column(name = "country_code", length = 4)
    private String countryCode;

    /**
     * Organization status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrganizationStatus status;

    /**
     * Contact email address.
     */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /**
     * Contact phone number.
     */
    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    /**
     * Website URL.
     */
    @Column(name = "website", length = 255)
    private String website;

    /**
     * ISO 4217 currency code (e.g., "EUR", "USD").
     */
    @Column(name = "currency_code", length = 8)
    private String currencyCode;

    /**
     * External system sync status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 32)
    private SyncStatus syncStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    // ── Constructors ──

    protected Organization() {
        // JPA requires
    }

    public Organization(String tenantId, String name, String currencyCode) {
        this.tenantId = tenantId;
        this.name = name;
        this.currencyCode = currencyCode != null ? currencyCode : "EUR";
        this.status = OrganizationStatus.ACTIVE;
        this.syncStatus = SyncStatus.LOCAL;
    }

    // ── Pre-persist/update hooks ──

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLegalForm() {
        return legalForm;
    }

    public void setLegalForm(String legalForm) {
        this.legalForm = legalForm;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getCommercialRegister() {
        return commercialRegister;
    }

    public void setCommercialRegister(String commercialRegister) {
        this.commercialRegister = commercialRegister;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(OrganizationStatus status) {
        this.status = status;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Organization lifecycle status.
     */
    public enum OrganizationStatus {
        ACTIVE, INACTIVE, SUSPENDED, UNDER_REVIEW
    }

    /**
     * Data source / sync status for external integrations.
     */
    public enum SyncStatus {
        /** Created and managed within this service */
        LOCAL,
        /** Synced from an external system (read-only) */
        REMOTE,
        /** Sync conflict detected */
        CONFLICT,
        /** Pending sync approval */
        PENDING_SYNC
    }
}
