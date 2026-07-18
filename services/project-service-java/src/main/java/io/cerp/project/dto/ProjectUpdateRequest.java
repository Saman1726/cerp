package io.cerp.project.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating a project. All fields are optional.
 */
public record ProjectUpdateRequest(

        @Size(max = 64, message = "Project code must be 64 characters or less") String code,

        @Size(max = 255, message = "Project name must be 255 characters or less") String name,

        @Size(max = 255, message = "Description must be 255 characters or less") String description,

        String constructionSiteId,

        UUID propertyId,

        @Size(max = 255, message = "Address line 1 must be 255 characters or less") String addressLine1,

        @Size(max = 255, message = "Address line 2 must be 255 characters or less") String addressLine2,

        @Size(max = 20, message = "Postal code must be 20 characters or less") String postalCode,

        @Size(max = 128, message = "City must be 128 characters or less") String city,

        @Size(max = 4, message = "Country code must be 4 characters or less") String countryCode,

        String projectType,

        String status,

        LocalDate startDate,

        LocalDate endDate,

        LocalDate actualStartDate,

        LocalDate actualEndDate,

        BigDecimal progress,

        BigDecimal budget,

        @Size(max = 8, message = "Currency code must be 8 characters or less") String currencyCode,

        @Size(max = 255, message = "Client name must be 255 characters or less") String clientName,

        @Size(max = 255, message = "Client email must be 255 characters or less") String clientEmail,

        @Size(max = 32, message = "Client phone must be 32 characters or less") String clientPhone

) {
}
