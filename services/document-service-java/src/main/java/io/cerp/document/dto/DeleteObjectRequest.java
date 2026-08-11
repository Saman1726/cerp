package io.cerp.document.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteObjectRequest(
    @NotBlank String objectKey
) {}
