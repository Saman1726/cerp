package io.cerp.partner.partners;

/**
 * Mirrors packages/core-api/src/party-registry/party-registry.types.ts Public record separated into its own file to satisfy Java's one-public-type-per-file rule.
 */
public record PartyRegistryEntry(
        String id,
        String code,
        String name,
        String type,
        String contactEmail,
        String status) {
}
