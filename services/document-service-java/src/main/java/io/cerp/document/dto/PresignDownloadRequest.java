package io.cerp.document.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignDownloadRequest(
    @NotBlank String objectKey
) {}
