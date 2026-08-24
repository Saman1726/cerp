package io.cerp.project.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConstructionSiteIfcFileCreateRequest(
        @NotBlank String objectKey,
        @NotBlank String fileName,
        Long contentLength,
        String fileKey,
        String ifcFileId
) {}