package io.cerp.catalog.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CatalogItemResponse {

    private UUID id;
    private String sku;
    private String name;
    private String description;
    private Map<String, Object> attributes;
    private List<String> categories;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private PriceView latestPrice;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
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

    public PriceView getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(PriceView latestPrice) {
        this.latestPrice = latestPrice;
    }

    public static class PriceView {
        private String currency;
        private BigDecimal amount;
        private OffsetDateTime validFrom;
        private OffsetDateTime validTo;

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
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
