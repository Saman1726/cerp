package io.cerp.vendor.dto;

import java.time.LocalDate;

public record CreateVendorCertificationRequest(
        String name,
        String issuingBody,
        LocalDate issueDate,
        LocalDate expiryDate,
        String certificateNumber,
        String documentUrl) {
}