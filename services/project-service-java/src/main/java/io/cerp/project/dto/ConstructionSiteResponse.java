package io.cerp.project.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a construction site.
 */
public record ConstructionSiteResponse(
        UUID id,
        String name,
        String address,
        List<BuildingResponse> buildings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy) {
}
