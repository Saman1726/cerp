package io.cerp.document.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import io.cerp.document.config.StorageProperties;
import io.cerp.document.domain.SiteCapture;
import io.cerp.document.dto.DeleteObjectRequest;
import io.cerp.document.dto.PresignDownloadRequest;
import io.cerp.document.dto.PresignDownloadResponse;
import io.cerp.document.dto.PresignRequest;
import io.cerp.document.dto.PresignResponse;
import io.cerp.document.dto.SiteCaptureSubmissionRequest;
import io.cerp.document.dto.SiteCaptureSubmissionResponse;
import io.cerp.document.repository.SiteCaptureRepository;

@Service
public class DocumentService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;
    private final SiteCaptureRepository repository;

    public DocumentService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties storageProperties,
            SiteCaptureRepository repository) {
        this.s3Client = s3Client;
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

        // Return two URLs: the original with the internal endpoint for server-side uploads,
        // and the public-endpoint version for browser uploads.
        String serverUploadUrl = presigned.url().toString();
        String uploadUrl = replaceHost(serverUploadUrl, storageProperties.publicEndpoint());
        return new PresignResponse(uploadUrl, serverUploadUrl, objectKey, Instant.now().plus(ttl).toString());
    }

    // Symmetric to presign() above, but for reading an already-uploaded object back: presign()
    // only ever mints short-lived PUT URLs, so there was previously no way to get a viewable/
    // downloadable URL for an object after it was stored (needed e.g. to display a workflow
    // step's attached photos).
    public PresignDownloadResponse presignDownload(PresignDownloadRequest request) {
        Duration ttl = Duration.ofMinutes(storageProperties.presignTtlMinutes());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(request.objectKey())
            .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .getObjectRequest(getObjectRequest)
            .build());

        String downloadUrl = replaceHost(presigned.url().toString(), storageProperties.publicEndpoint());
        return new PresignDownloadResponse(downloadUrl, Instant.now().plus(ttl).toString());
    }

    // Uses the internal (Docker-network) S3Client directly, not a presigned URL - this is a
    // server-to-server delete, never something a browser needs to do itself. Best-effort from
    // the caller's perspective: workflow-service deletes its metadata row first and treats this
    // as cleanup, not a transactional dependency (a delete failing here just leaves an orphaned
    // object, which never affects correctness the way a dangling metadata row would).
    public void deleteObject(DeleteObjectRequest request) {
        s3Client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(request.objectKey())
            .build());
    }

    public byte[] downloadForServerSide(String objectKey) {
        var response = s3Client.getObject(
                software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(storageProperties.bucket())
                    .key(objectKey)
                    .build());
        try {
            return response.readAllBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read IFC file", e);
        } finally {
            try { response.close(); } catch (java.io.IOException ignored) {}
        }
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

    /**
     * The s3Presigner signs against the Docker-internal endpoint (e.g. minio:9000)
     * so the container can always reach MinIO. This rewrites the host portion of
     * the resulting presigned URL so browsers on the LAN can reach MinIO directly.
     */
    private String replaceHost(String presignedUrl, String publicEndpoint) {
        try {
            URI original = new URI(presignedUrl);
            URI publicUri = new URI(publicEndpoint);
            // Rebuild with public host:port, keeping path + query params intact
            return new URI(
                publicUri.getScheme(),
                null, // no userInfo
                publicUri.getHost(),
                publicUri.getPort() >= 0 ? publicUri.getPort() : -1,
                original.getPath(),
                original.getQuery(),
                original.getFragment()
            ).toString();
        } catch (URISyntaxException e) {
            // Should never happen with well-formed URLs, but return original as fallback
            return presignedUrl;
        }
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
