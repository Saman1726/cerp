package io.cerp.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "price_table", schema = "catalog", indexes = {
        @Index(name = "idx_pt_material_service_id", columnList = "material_service_id"),
        @Index(name = "idx_pt_valid_from", columnList = "material_service_id, valid_from DESC"),
        @Index(name = "idx_pt_tenant_id", columnList = "tenant_id")
})
public class PriceTable {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "material_service_id", nullable = false)
    private MaterialService materialService;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    // Self-referencing relations for price chain traversal
    @ManyToOne
    @JoinColumn(name = "last_price_id")
    private PriceTable lastPrice;

    @ManyToOne
    @JoinColumn(name = "next_price_id")
    private PriceTable nextPrice;

    // Reverse relations for bidirectional navigation
    @OneToMany(mappedBy = "lastPrice")
    private List<PriceTable> nextPrices = new ArrayList<>();

    @OneToMany(mappedBy = "nextPrice")
    private List<PriceTable> lastPrices = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public PriceTable() {
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

    public MaterialService getMaterialService() {
        return materialService;
    }

    public void setMaterialService(MaterialService materialService) {
        this.materialService = materialService;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }

    public PriceTable getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(PriceTable lastPrice) {
        this.lastPrice = lastPrice;
    }

    public PriceTable getNextPrice() {
        return nextPrice;
    }

    public void setNextPrice(PriceTable nextPrice) {
        this.nextPrice = nextPrice;
    }

    public List<PriceTable> getNextPrices() {
        return nextPrices;
    }

    public void setNextPrices(List<PriceTable> nextPrices) {
        this.nextPrices = nextPrices != null ? new ArrayList<>(nextPrices) : new ArrayList<>();
    }

    public List<PriceTable> getLastPrices() {
        return lastPrices;
    }

    public void setLastPrices(List<PriceTable> lastPrices) {
        this.lastPrices = lastPrices != null ? new ArrayList<>(lastPrices) : new ArrayList<>();
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

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.validFrom == null) {
            this.validFrom = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceTable that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}