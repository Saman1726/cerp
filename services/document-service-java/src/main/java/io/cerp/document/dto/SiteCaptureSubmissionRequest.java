package io.cerp.document.dto;

import jakarta.validation.constraints.NotBlank;

public record SiteCaptureSubmissionRequest(
    @NotBlank String clientId,
    @NotBlank String projectId,
    String subProjectId,
    @NotBlank String kind,
    String notes,
    @NotBlank String capturedAt,
    @NotBlank String objectKey,
    @NotBlank String originalFileName,
    @NotBlank String contentType,
    long byteSize
) {}
