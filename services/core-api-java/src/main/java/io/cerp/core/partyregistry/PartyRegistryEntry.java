package io.cerp.core.partyregistry;

// Mirrors packages/core-api/src/party-registry/party-registry.types.ts PartyRegistryEntry
public record PartyRegistryEntry(
        String id,
        String code,
        String name,
        String type,
        String contactEmail,
        String status) {
}
