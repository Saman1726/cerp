package io.cerp.document.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import io.cerp.document.config.StorageProperties;
import io.cerp.document.domain.SiteCapture;
import io.cerp.document.dto.PresignRequest;
import io.cerp.document.dto.PresignResponse;
import io.cerp.document.dto.SiteCaptureSubmissionRequest;
import io.cerp.document.dto.SiteCaptureSubmissionResponse;
import io.cerp.document.repository.SiteCaptureRepository;

@Service
public class DocumentService {

    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;
    private final SiteCaptureRepository repository;

    public DocumentService(S3Presigner s3Presigner, StorageProperties storageProperties, SiteCaptureRepository repository) {
        this.s3Presigner = s3Presigner;
        this.storageProperties = storageProperties;
        this.repository = repository;
    }

    public PresignResponse presign(UUID tenantId, PresignRequest request) {
        String objectKey = "tenants/%s/projects/%s/%s%s".formatted(
            tenantId, request.projectId(), UUID.randomUUID(), extensionFor(request.contentType(), request.fileName()));

        Duration ttl = Duration.ofMinutes(storageProperties.presignTtlMinutes());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(objectKey)
            .contentType(request.contentType())
            .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .putObjectRequest(putObjectRequest)
            .build());

        return new PresignResponse(presigned.url().toString(), objectKey, Instant.now().plus(ttl).toString());
    }

    public SiteCaptureSubmissionResponse submit(UUID tenantId, SiteCaptureSubmissionRequest request) {
        UUID id = UUID.fromString(request.clientId());

        // clientId is the idempotency key: a retried submission (e.g. after the
        // client got the response but a flaky connection dropped before it could
        // process it) finds the already-stored row instead of erroring or
        // duplicating it.
        return repository.findById(id)
            .map(existing -> new SiteCaptureSubmissionResponse(existing.getId().toString(), existing.getStatus()))
            .orElseGet(() -> {
                SiteCapture capture = new SiteCapture(
                    id,
                    tenantId,
                    UUID.fromString(request.projectId()),
                    request.subProjectId() != null ? UUID.fromString(request.subProjectId()) : null,
                    request.kind(),
                    request.notes(),
                    Instant.parse(request.capturedAt()),
                    request.objectKey(),
                    request.originalFileName(),
                    request.contentType(),
                    request.byteSize());
                repository.save(capture);
                return new SiteCaptureSubmissionResponse(capture.getId().toString(), capture.getStatus());
            });
    }

    private String extensionFor(String contentType, String fileName) {
        if (contentType != null) {
            int slash = contentType.indexOf('/');
            if (slash >= 0) {
                return "." + contentType.substring(slash + 1);
            }
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}
