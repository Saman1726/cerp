package io.cerp.vendor.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VendorCertificationResponse(
        String id,
        String vendorId,
        String name,
        String issuingBody,
        LocalDate issueDate,
        LocalDate expiryDate,
        String certificateNumber,
        String documentUrl,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}