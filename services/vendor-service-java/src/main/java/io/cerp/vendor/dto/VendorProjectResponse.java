package io.cerp.vendor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VendorProjectResponse(
        String id,
        String vendorId,
        String name,
        String description,
        String projectType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal estimatedValue,
        String imageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}