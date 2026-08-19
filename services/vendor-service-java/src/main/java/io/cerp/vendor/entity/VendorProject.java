package io.cerp.vendor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "vendor_projects", schema = "vendor")
public class VendorProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "project_type")
    private String projectType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "estimated_value", precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public VendorProject() {
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

    public VendorProject setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
        return this;
    }

    public String getName() {
        return name;
    }

    public VendorProject setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public VendorProject setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getProjectType() {
        return projectType;
    }

    public VendorProject setProjectType(String projectType) {
        this.projectType = projectType;
        return this;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public VendorProject setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public VendorProject setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public VendorProject setEstimatedValue(BigDecimal estimatedValue) {
        this.estimatedValue = estimatedValue;
        return this;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public VendorProject setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public VendorProject setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public VendorProject setUpdatedAt(OffsetDateTime updatedAt) {
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
        if (!(o instanceof VendorProject that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}