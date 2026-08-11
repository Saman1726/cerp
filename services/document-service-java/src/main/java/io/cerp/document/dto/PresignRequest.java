package io.cerp.document.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignRequest(
    @NotBlank String projectId,
    String subProjectId,
    @NotBlank String kind,
    @NotBlank String fileName,
    @NotBlank String contentType
) {}
