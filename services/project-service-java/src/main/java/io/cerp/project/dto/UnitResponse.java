package io.cerp.project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a unit.
 */
public record UnitResponse(
        UUID id,
        String label,
        BigDecimal usableArea,
        Integer bedrooms,
        Integer bathrooms,
        Integer balconies,
        Integer toilets,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
