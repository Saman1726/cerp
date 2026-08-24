package io.cerp.project.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
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

@Entity
@Table(name = "construction_materials", schema = "project", indexes = {
        @Index(name = "idx_cm_site_id", columnList = "construction_site_id"),
        @Index(name = "idx_cm_physical_obj", columnList = "physical_object_type, physical_object_id"),
        @Index(name = "idx_cm_service_code", columnList = "material_service_code")
})
public class ConstructionMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_site_id", nullable = false)
    private ConstructionSite constructionSite;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical_object_type", length = 20)
    private PhysicalObjectType physicalObjectType;

    @Column(name = "physical_object_id")
    private UUID physicalObjectId;
@Column(name = "material_service_code", length = 100)
    private String materialServiceCode;

    @Column(name = "material_service_name", length = 255)
    private String materialServiceName;

    @Column(name = "ifc_material_name", length = 500)
    private String ifcMaterialName;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ConstructionMaterial() {
    }

    public ConstructionMaterial(ConstructionSite constructionSite,
            PhysicalObjectType physicalObjectType, UUID physicalObjectId,
            String materialServiceCode, String materialServiceName,
            String ifcMaterialName, BigDecimal quantity, String unit) {
        this.constructionSite = constructionSite;
        this.physicalObjectType = physicalObjectType;
        this.physicalObjectId = physicalObjectId;
        this.materialServiceCode = materialServiceCode;
        this.materialServiceName = materialServiceName;
        this.ifcMaterialName = ifcMaterialName;
        this.quantity = quantity;
        this.unit = unit;
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConstructionSite getConstructionSite() {
        return constructionSite;
    }

    public void setConstructionSite(ConstructionSite constructionSite) {
        this.constructionSite = constructionSite;
    }

    public PhysicalObjectType getPhysicalObjectType() {
        return physicalObjectType;
    }

    public void setPhysicalObjectType(PhysicalObjectType physicalObjectType) {
        this.physicalObjectType = physicalObjectType;
    }

    public UUID getPhysicalObjectId() {
        return physicalObjectId;
    }

    public void setPhysicalObjectId(UUID physicalObjectId) {
        this.physicalObjectId = physicalObjectId;
    }

    public String getMaterialServiceCode() {
        return materialServiceCode;
    }

    public void setMaterialServiceCode(String materialServiceCode) {
        this.materialServiceCode = materialServiceCode;
    }

    public String getMaterialServiceName() {
        return materialServiceName;
    }

    public void setMaterialServiceName(String materialServiceName) {
        this.materialServiceName = materialServiceName;
    }

    public String getIfcMaterialName() {
        return ifcMaterialName;
    }

    public void setIfcMaterialName(String ifcMaterialName) {
        this.ifcMaterialName = ifcMaterialName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String effectiveDedupCode() {
        if (materialServiceCode != null && !materialServiceCode.isBlank()) {
            return materialServiceCode;
        }
        return ifcMaterialName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstructionMaterial that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public enum PhysicalObjectType {
        BUILDING, FLOOR, UNIT
    }
}