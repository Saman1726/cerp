package io.cerp.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating an organization.
 */
public record OrganizationCreateRequest(

        @NotBlank(message = "Organization name is required") @Size(max = 255, message = "Name must be 255 characters or less") String name,

        @Size(max = 64, message = "Legal form must be 64 characters or less") String legalForm,

        @Size(max = 64, message = "Tax ID must be 64 characters or less") String taxId,

        @Size(max = 64, message = "Commercial register must be 64 characters or less") String commercialRegister,

        @Size(max = 255, message = "Address line 1 must be 255 characters or less") String addressLine1,

        @Size(max = 255, message = "Address line 2 must be 255 characters or less") String addressLine2,

        @Size(max = 20, message = "Postal code must be 20 characters or less") String postalCode,

        @Size(max = 128, message = "City must be 128 characters or less") String city,

        @Size(max = 4, message = "Country code must be 4 characters or less") String countryCode,

        @Email(message = "Contact email must be valid") @Size(max = 255, message = "Contact email must be 255 characters or less") String contactEmail,

        @Size(max = 32, message = "Contact phone must be 32 characters or less") String contactPhone,

        @Size(max = 255, message = "Website must be 255 characters or less") String website,

        @Size(max = 8, message = "Currency code must be 8 characters or less") String currencyCode

) {
}
