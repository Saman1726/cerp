package io.cerp.document.api;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/site-captures")
    public ResponseEntity<SiteCaptureSubmissionResponse> submit(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody SiteCaptureSubmissionRequest request) {
        return ResponseEntity.ok(documentService.submit(tenantId, request));
    }
}
