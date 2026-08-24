package io.cerp.document.api;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.document.dto.DeleteObjectRequest;
import io.cerp.document.dto.PresignDownloadRequest;
import io.cerp.document.dto.PresignDownloadResponse;
import io.cerp.document.dto.PresignRequest;
import io.cerp.document.dto.PresignResponse;
import io.cerp.document.dto.SiteCaptureSubmissionRequest;
import io.cerp.document.dto.SiteCaptureSubmissionResponse;
import io.cerp.document.service.DocumentService;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/media/presign")
    public ResponseEntity<PresignResponse> presign(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PresignRequest request) {
        return ResponseEntity.ok(documentService.presign(tenantId, request));
    }

    @PostMapping("/media/presign-download")
    public ResponseEntity<PresignDownloadResponse> presignDownload(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PresignDownloadRequest request) {
        return ResponseEntity.ok(documentService.presignDownload(request));
    }

    @PostMapping("/site-captures")
    public ResponseEntity<SiteCaptureSubmissionResponse> submit(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody SiteCaptureSubmissionRequest request) {
        return ResponseEntity.ok(documentService.submit(tenantId, request));
    }

    // POST, not DELETE + path variable: objectKey contains slashes, which don't survive as a
    // single path segment - same reasoning as presign-download taking it in the body.
    @PostMapping("/media/delete")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody DeleteObjectRequest request) {
        documentService.deleteObject(request);
        return ResponseEntity.noContent().build();
    }

    /** Server-to-server download: serves the file content directly using the internal S3Client,
     * bypassing the public MinIO endpoint that external services (like project-service) cannot reach. */
    @PostMapping("/media/download/internal")
    public ResponseEntity<byte[]> downloadForServerSide(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody io.cerp.document.dto.DownloadForServerSideRequest request) {
        try {
            byte[] content = documentService.downloadForServerSide(request.objectKey());
            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM).body(content);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage().getBytes());
        }
    }
}
