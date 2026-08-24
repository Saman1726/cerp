package io.cerp.document.dto;

public record PresignResponse(
    /** Presigned S3 PUT URL with the public endpoint (for browser uploads). */
    String uploadUrl,
    /** Presigned S3 PUT URL with the internal endpoint (for server-side uploads). */
    String serverUploadUrl,
    /** Storage object key the backend will associate with the uploaded file. */
    String objectKey,
    /** ISO timestamp the presigned URL stops being valid. */
    String expiresAt
) {}
