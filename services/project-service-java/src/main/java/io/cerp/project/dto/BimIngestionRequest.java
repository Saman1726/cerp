package io.cerp.project.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for BIM ingestion at the construction site level.
 * The presignedUrl is obtained from the document-service by the frontend
 * and passed directly (no additional auth needed for the presigned URL).
 */
public record BimIngestionRequest(
        @NotNull UUID constructionSiteId,
        @NotBlank String objectKey,
        @NotNull @Positive Long contentLength,
        String fileName) {
}