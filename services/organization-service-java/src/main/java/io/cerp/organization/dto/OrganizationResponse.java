package io.cerp.organization.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.cerp.organization.domain.Organization;

/**
 * Response DTO for organization data.
 */
public record OrganizationResponse(
        UUID id,
        String tenantId,
        String externalId,
        String name,
        String legalForm,
        String taxId,
        String commercialRegister,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String city,
        String countryCode,
        String status,
        String contactEmail,
        String contactPhone,
        String website,
        String currencyCode,
        String syncStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    /**
     * Convert entity to response DTO.
     */
    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
            org.getId(),
            org.getTenantId(),
            org.getExternalId(),
            org.getName(),
            org.getLegalForm(),
            org.getTaxId(),
            org.getCommercialRegister(),
            org.getAddressLine1(),
            org.getAddressLine2(),
            org.getPostalCode(),
            org.getCity(),
            org.getCountryCode(),
            org.getStatus() != null ? org.getStatus().name() : null,
            org.getContactEmail(),
            org.getContactPhone(),
            org.getWebsite(),
            org.getCurrencyCode(),
            org.getSyncStatus() != null ? org.getSyncStatus().name() : null,
            org.getCreatedAt(),
            org.getUpdatedAt());
    }
}
