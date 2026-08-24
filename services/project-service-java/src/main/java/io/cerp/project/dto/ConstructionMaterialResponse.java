package io.cerp.project.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for a construction material.
 */
public record ConstructionMaterialResponse(
        UUID id,
        String physicalObjectType,
        UUID physicalObjectId,
        String materialServiceCode,
        String materialServiceName,
        String ifcMaterialName,
        BigDecimal quantity,
        String unit) {
}