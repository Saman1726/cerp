package io.cerp.catalog.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class MaterialServiceResponse {

    private UUID id;
    private String type;
    private String code;
    private String name;
    private String description;
    private String unit;
    private String documentUrl;
    private List<UUID> categoryGroupIds;
    private List<UUID> categoryIds;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private LatestPriceView latestPrice;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public List<UUID> getCategoryGroupIds() {
        return categoryGroupIds;
    }

    public void setCategoryGroupIds(List<UUID> categoryGroupIds) {
        this.categoryGroupIds = categoryGroupIds;
    }

    public List<UUID> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<UUID> categoryIds) {
        this.categoryIds = categoryIds;
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

    public LatestPriceView getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(LatestPriceView latestPrice) {
        this.latestPrice = latestPrice;
    }

    public static class LatestPriceView {
        private String currency;
        private BigDecimal unitPrice;
        private OffsetDateTime validFrom;
        private OffsetDateTime validTo;

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
    }
}