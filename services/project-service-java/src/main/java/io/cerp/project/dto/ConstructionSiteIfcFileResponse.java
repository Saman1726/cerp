package io.cerp.project.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConstructionSiteIfcFileResponse(
        UUID id,
        String fileName,
        Long fileSize,
        String fileKey,
        String ifcFileId,
        String status,
        String previewUrl,
        String metadataUrl,
        String exportUrl,
        String blenderRenderUrl,
        String blenderRenderView,
        LocalDateTime lastPresignedAt,
        LocalDateTime createdAt
) {}