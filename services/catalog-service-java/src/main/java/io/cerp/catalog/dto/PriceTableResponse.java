package io.cerp.catalog.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class PriceTableResponse {

    private UUID id;
    private UUID materialServiceId;
    private String materialServiceName;
    private String materialServiceCode;
    private String materialServiceType;
    private String currency;
    private BigDecimal unitPrice;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Price chain navigation
    private LatestPriceView lastPrice;
    private LatestPriceView nextPrice;
    private List<LatestPriceView> previousPrices;
    private List<LatestPriceView> followingPrices;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMaterialServiceId() {
        return materialServiceId;
    }

    public void setMaterialServiceId(UUID materialServiceId) {
        this.materialServiceId = materialServiceId;
    }

    public String getMaterialServiceName() {
        return materialServiceName;
    }

    public void setMaterialServiceName(String materialServiceName) {
        this.materialServiceName = materialServiceName;
    }

    public String getMaterialServiceCode() {
        return materialServiceCode;
    }

    public void setMaterialServiceCode(String materialServiceCode) {
        this.materialServiceCode = materialServiceCode;
    }

    public String getMaterialServiceType() {
        return materialServiceType;
    }

    public void setMaterialServiceType(String materialServiceType) {
        this.materialServiceType = materialServiceType;
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

    public LatestPriceView getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(LatestPriceView lastPrice) {
        this.lastPrice = lastPrice;
    }

    public LatestPriceView getNextPrice() {
        return nextPrice;
    }

    public void setNextPrice(LatestPriceView nextPrice) {
        this.nextPrice = nextPrice;
    }

    public List<LatestPriceView> getPreviousPrices() {
        return previousPrices;
    }

    public void setPreviousPrices(List<LatestPriceView> previousPrices) {
        this.previousPrices = previousPrices;
    }

    public List<LatestPriceView> getFollowingPrices() {
        return followingPrices;
    }

    public void setFollowingPrices(List<LatestPriceView> followingPrices) {
        this.followingPrices = followingPrices;
    }

    public static class LatestPriceView {
        private UUID id;
        private String currency;
        private BigDecimal unitPrice;
        private OffsetDateTime validFrom;
        private OffsetDateTime validTo;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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
    }
}