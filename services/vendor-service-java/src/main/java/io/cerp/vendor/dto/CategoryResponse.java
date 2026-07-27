package io.cerp.vendor.dto;

public record CategoryResponse(
        String id,
        String code,
        String name,
        String description,
        String icon,
        Integer displayOrder,
        String groupId,
        String groupName) {
}
