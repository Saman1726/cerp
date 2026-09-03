package io.cerp.project.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.project.dto.ConstructionSiteIfcFileCreateRequest;
import io.cerp.project.dto.ConstructionSiteIfcFileResponse;
import io.cerp.project.service.IfcFileService;

@RestController
@RequestMapping("/api/v1/construction-sites")
public class ConstructionSiteIpcFileController {

    private final IfcFileService ifcFileService;

    public ConstructionSiteIpcFileController(IfcFileService ifcFileService) {
        this.ifcFileService = ifcFileService;
    }

    @PostMapping("/{siteId}/ifc-files")
    public ResponseEntity<ConstructionSiteIfcFileResponse> uploadIfcFile(
            @PathVariable UUID siteId,
            @RequestBody ConstructionSiteIfcFileCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        ConstructionSiteIfcFileResponse response = ifcFileService.uploadIfcFile(siteId, request, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{siteId}/ifc-files")
    public ResponseEntity<List<ConstructionSiteIfcFileResponse>> listIfcFiles(
            @PathVariable UUID siteId) {
        List<ConstructionSiteIfcFileResponse> files = ifcFileService.listIfcFiles(siteId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{siteId}/ifc-files/{fileId}")
    public ResponseEntity<Void> deleteIfcFile(
            @PathVariable UUID siteId,
            @PathVariable UUID fileId) {
        ifcFileService.deleteIfcFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{siteId}/ifc-files/{fileId}/preview")
    public ResponseEntity<ConstructionSiteIfcFileResponse> triggerPreview(
            @PathVariable UUID siteId,
            @PathVariable UUID fileId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) throws Exception {
        ConstructionSiteIfcFileResponse response = ifcFileService.triggerPreview(fileId, authorization, tenantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{siteId}/ifc-files/{fileId}/blender-render")
    public ResponseEntity<ConstructionSiteIfcFileResponse> triggerBlenderRender(
            @PathVariable UUID siteId,
            @PathVariable UUID fileId,
            @RequestParam(defaultValue = "front") String view,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) throws Exception {
        ConstructionSiteIfcFileResponse response = ifcFileService.triggerBlenderRender(fileId, view, authorization, tenantId, siteId.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{siteId}/ifc-files/{fileId}/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata(
            @PathVariable UUID siteId,
            @PathVariable UUID fileId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) throws Exception {
        var ifcFile = ifcFileService.listIfcFiles(siteId).stream()
                .filter(f -> f.id().equals(fileId))
                .findFirst();
        if (ifcFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> metadata = ifcFileService.getMetadata(ifcFile.get().ifcFileId(), ifcFile.get().fileKey(), authorization, tenantId);
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/{siteId}/ifc-files/{fileId}/export")
    public ResponseEntity<Map<String, Object>> export3D(
            @PathVariable UUID siteId,
            @PathVariable UUID fileId,
            @RequestParam(defaultValue = "glb") String format,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) throws Exception {
        var ifcFile = ifcFileService.listIfcFiles(siteId).stream()
                .filter(f -> f.id().equals(fileId))
                .findFirst();
        if (ifcFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> result = ifcFileService.export3D(ifcFile.get().ifcFileId(), format, ifcFile.get().fileKey(), authorization, tenantId, siteId.toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{siteId}/ifc-files/{fileId}/generate-objects")
    public ResponseEntity<Object> generateObjects(
            @PathVariable UUID siteId,
            @PathVariable UUID fileId) throws Exception {
        var ifcFile = ifcFileService.listIfcFiles(siteId).stream()
                .filter(f -> f.id().equals(fileId))
                .findFirst();
        if (ifcFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var result = ifcFileService.generateObjects(siteId, ifcFile.get().ifcFileId());
        return ResponseEntity.ok(result);
    }

    /**
     * Generate a fresh presigned download URL for a stored resource.
     * Each call re-signs the same MinIO object — useful when the previous
     * URL has expired.  Supports types: preview, render, export, metadata.
     */
    @GetMapping("/{siteId}/ifc-files/{fileId}/{urlType}")
    public ResponseEntity<Map<String, String>> getFreshUrl(
            @PathVariable UUID siteId,
            @PathVariable UUID fileId,
            @PathVariable String urlType,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) throws Exception {
        // Verify the file belongs to this site
        var ifcFile = ifcFileService.listIfcFiles(siteId).stream()
                .filter(f -> f.id().equals(fileId))
                .findFirst();
        if (ifcFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String freshUrl = ifcFileService.generateFreshPresignedUrl(fileId, urlType, authorization, tenantId);
        return ResponseEntity.ok(Map.of("url", freshUrl));
    }
}
