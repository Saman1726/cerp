package io.cerp.project.dto;

import java.math.BigDecimal;

/**
 * Request DTO for creating a floor.
 */
public record FloorCreateRequest(
        String type,
        BigDecimal usableArea,
        BigDecimal totalArea) {
}
