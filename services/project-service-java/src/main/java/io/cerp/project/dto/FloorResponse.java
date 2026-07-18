package io.cerp.project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a floor.
 */
public record FloorResponse(
        UUID id,
        String type,
        BigDecimal usableArea,
        BigDecimal totalArea,
        List<UnitResponse> units,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
