package io.cerp.vendor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateVendorProjectRequest(
        String name,
        String description,
        String projectType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal estimatedValue,
        String imageUrl) {
}