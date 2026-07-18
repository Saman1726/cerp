package io.cerp.project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a building.
 */
public record BuildingResponse(
        UUID id,
        String name,
        BigDecimal usableArea,
        BigDecimal totalArea,
        List<FloorResponse> floors,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
