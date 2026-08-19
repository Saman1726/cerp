package io.cerp.vendor.dto;

import java.util.UUID;

public record CategoryGroupResponse(
        UUID id,
        String name,
        String description,
        Integer displayOrder,
        Boolean active,
        Long activeCategoryCount) {
}