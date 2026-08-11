package io.cerp.document.dto;

public record PresignResponse(
    String uploadUrl,
    String objectKey,
    String expiresAt
) {}
