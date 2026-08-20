package io.cerp.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CreatePriceTableRequest {

    @NotNull
    private UUID materialServiceId;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private BigDecimal unitPrice;

    private OffsetDateTime validFrom;

    private OffsetDateTime validTo;

    public UUID getMaterialServiceId() {
        return materialServiceId;
    }

    public void setMaterialServiceId(UUID materialServiceId) {
        this.materialServiceId = materialServiceId;
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