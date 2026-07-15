package io.cerp.core.partyregistry;

import jakarta.validation.constraints.NotBlank;

// Request DTO matching services/partner-system/src/auth/dto/partner-login.dto.ts
public record PartnerLoginRequest(
        @NotBlank String partnerCode,
        @NotBlank String username,
        @NotBlank String password) {
}
