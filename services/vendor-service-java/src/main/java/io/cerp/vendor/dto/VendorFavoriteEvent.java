package io.cerp.vendor.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event payload for vendor favorite create/remove events published to Kafka.
 */
public record VendorFavoriteEvent(
        UUID userId,
        UUID vendorId,
        String action, // "CREATE" or "REMOVE"
        OffsetDateTime timestamp) {

    public VendorFavoriteEvent(UUID userId, UUID vendorId, String action) {
        this(userId, vendorId, action, OffsetDateTime.now());
    }
}
