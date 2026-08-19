package io.cerp.vendor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "vendor_certifications", schema = "vendor")
public class VendorCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "issuing_body")
    private String issuingBody;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public VendorCertification() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public VendorCertification setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
        return this;
    }

    public String getName() {
        return name;
    }

    public VendorCertification setName(String name) {
        this.name = name;
        return this;
    }

    public String getIssuingBody() {
        return issuingBody;
    }

    public VendorCertification setIssuingBody(String issuingBody) {
        this.issuingBody = issuingBody;
        return this;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public VendorCertification setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
        return this;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public VendorCertification setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public VendorCertification setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
        return this;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public VendorCertification setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public VendorCertification setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public VendorCertification setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public VendorCertification setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    @PrePersist
    public void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VendorCertification that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}