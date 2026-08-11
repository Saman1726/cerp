package io.cerp.document.dto;

public record PresignDownloadResponse(
    String url,
    String expiresAt
) {}
