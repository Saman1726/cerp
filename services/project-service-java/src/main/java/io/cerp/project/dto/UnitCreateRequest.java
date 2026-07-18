package io.cerp.project.dto;

import java.math.BigDecimal;

/**
 * Request DTO for creating a unit.
 */
public record UnitCreateRequest(
        String label,
        BigDecimal usableArea,
        Integer bedrooms,
        Integer bathrooms,
        Integer balconies,
        Integer toilets) {
}
