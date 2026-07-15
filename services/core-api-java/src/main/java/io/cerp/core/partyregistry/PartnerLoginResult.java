package io.cerp.core.partyregistry;

// Mirrors packages/core-api/src/party-registry/party-registry.types.ts PartnerLoginResult
public record PartnerLoginResult(
        String partnerId,
        String partnerCode,
        String partnerName,
        String username,
        String role) {
}
