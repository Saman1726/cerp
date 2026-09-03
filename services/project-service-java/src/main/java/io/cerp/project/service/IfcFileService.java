package io.cerp.project.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cerp.project.domain.ConstructionSite;
import io.cerp.project.domain.ConstructionSiteIfcFile;
import io.cerp.project.domain.ConstructionSiteIfcFile.IfcFileStatus;
import io.cerp.project.dto.ConstructionSiteIfcFileCreateRequest;
import io.cerp.project.dto.ConstructionSiteIfcFileResponse;
import io.cerp.project.repository.ConstructionSiteIfcFileRepository;
import io.cerp.project.repository.ConstructionSiteRepository;

@Service
public class IfcFileService {

    private static final Logger log = LoggerFactory.getLogger(IfcFileService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Shared HTTP client pinned to HTTP/1.1.
     *
     * <p>The JDK default ({@link HttpClient#newHttpClient()}) negotiates HTTP/2, which over
     * cleartext http:// issues an h2c upgrade. The Python services are served by uvicorn/h11,
     * which cannot parse the HTTP/2 preface and answers "Invalid HTTP request received." with
     * a plain-text 400. Pinning to HTTP/1.1 keeps those calls working.
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private final ConstructionSiteRepository constructionSiteRepository;
    private final ConstructionSiteIfcFileRepository ifcFileRepository;
    private final BimIngestionService bimIngestionService;
    private final String documentServiceUrl;
    private final String ifcRendererUrl;

    public IfcFileService(ConstructionSiteRepository constructionSiteRepository,
            ConstructionSiteIfcFileRepository ifcFileRepository,
            BimIngestionService bimIngestionService,
            @Value("${cerp.document-service-url:http://document-service-java:8092}") String documentServiceUrl,
            @Value("${cerp.ifc-renderer-url:http://ifc-renderer-python:8093}") String ifcRendererUrl) {
        this.constructionSiteRepository = constructionSiteRepository;
        this.ifcFileRepository = ifcFileRepository;
        this.bimIngestionService = bimIngestionService;
        this.documentServiceUrl = documentServiceUrl;
        this.ifcRendererUrl = ifcRendererUrl;
    }

    @Transactional
    public ConstructionSiteIfcFileResponse uploadIfcFile(UUID siteId,
            ConstructionSiteIfcFileCreateRequest request, String createdBy) throws Exception {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Construction site not found: " + siteId));
        ConstructionSiteIfcFile ifcFile = new ConstructionSiteIfcFile(
                site, request.objectKey(), request.fileName(),
                request.contentLength(), request.fileKey(), request.ifcFileId(), createdBy);
        ConstructionSiteIfcFile saved = ifcFileRepository.save(ifcFile);
        // Register with the IFC renderer to get a file_id (if not already set)
        if (saved.getIfcFileId() == null || saved.getIfcFileId().isBlank()) {
            try {
                String fileId = registerWithIfcRenderer(saved.getObjectKey(), String.valueOf(siteId));
                if (fileId != null) {
                    saved.setIfcFileId(fileId);
                    saved = ifcFileRepository.save(saved);
                }
            } catch (Exception e) {
                log.warn("Failed to register IFC file with renderer: {}", e.getMessage(), e);
            }
        }
        return toResponse(saved);
    }

    /**
     * Register an already-uploaded IFC file with the IFC renderer service and return the generated file_id.
     */
    private String registerWithIfcRenderer(String fileKey, String projectId) throws Exception {
        String url = ifcRendererUrl + "/api/v1/ifc/register?file_key=" + java.net.URLEncoder.encode(fileKey, java.nio.charset.StandardCharsets.UTF_8)
                + "&project_id=" + java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(java.time.Duration.ofSeconds(10))
                .build();
        HttpResponse<String> response = HTTP_CLIENT
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode json = objectMapper.readTree(response.body());
            return json.get("file_id").asText();
        }
        log.warn("IFC renderer registration failed (HTTP {}) for file_key={}, project_id={}: {}",
                response.statusCode(), fileKey, projectId, response.body());
        return null;
    }

    public List<ConstructionSiteIfcFileResponse> listIfcFiles(UUID siteId) {
        return ifcFileRepository.findByConstructionSiteId(siteId).stream()
                .map(this::toResponse).toList();
    }
    private String effectiveTenantId(String tenantId) {
        return (tenantId == null || tenantId.isBlank()) ? "00000000-0000-0000-0000-000000000001" : tenantId;
    }

    @Transactional
    public ConstructionSiteIfcFileResponse triggerPreview(UUID fileId, String authorization, String tenantId) throws Exception {
        ConstructionSiteIfcFile ifcFile = ifcFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("IFC file not found: " + fileId));
        // Lazily register with the IFC renderer if file_id is missing (pre-existing records)
        if (ifcFile.getIfcFileId() == null || ifcFile.getIfcFileId().isBlank()) {
            String registeredId = registerWithIfcRenderer(
                    ifcFile.getObjectKey(),
                    String.valueOf(ifcFile.getConstructionSite().getId()));
            if (registeredId != null) {
                ifcFile.setIfcFileId(registeredId);
                ifcFileRepository.save(ifcFile);
            }
        }
        if (ifcFile.getIfcFileId() == null || ifcFile.getIfcFileId().isBlank()) {
            throw new IllegalStateException(
                    "IFC file is not registered with the renderer (no file_id) for file: " + fileId);
        }
        ifcFile.setStatus(IfcFileStatus.PROCESSING);
        ifcFile = ifcFileRepository.save(ifcFile);
        try {
            String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFile.getIfcFileId() + "/preview?file_key=" + ifcFile.getObjectKey();
            HttpRequest.Builder previewBuilder = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("X-Tenant-Id", effectiveTenantId(tenantId))
                    .timeout(java.time.Duration.ofSeconds(120));
            if (authorization != null && !authorization.isBlank()) {
                previewBuilder.header("Authorization", authorization);
            }
            HttpResponse<byte[]> response = HTTP_CLIENT
                    .send(previewBuilder.GET().build(),
                            HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                // ── Presign upload ──
                String presignUrl = documentServiceUrl + "/api/v1/media/presign";
                String jsonBody = objectMapper.writeValueAsString(Map.of(
                        "projectId", ifcFile.getConstructionSite().getId().toString(),
                        "kind", "IFC_FILE",
                        "fileName", "render.png",
                        "contentType", "image/png"));
                HttpRequest.Builder presignBuilder = HttpRequest.newBuilder().uri(URI.create(presignUrl))
                        .header("Content-Type", "application/json")
                        .header("X-Tenant-Id", effectiveTenantId(tenantId))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(java.time.Duration.ofSeconds(30));
                if (authorization != null && !authorization.isBlank()) {
                    presignBuilder.header("Authorization", authorization);
                }
                HttpResponse<String> presignResponse = HTTP_CLIENT
                        .send(presignBuilder.build(), HttpResponse.BodyHandlers.ofString());
                JsonNode presignJson = objectMapper.readTree(presignResponse.body());
                String uploadUrl = presignJson.get("serverUploadUrl").asText();
                String actualObjectKey = presignJson.get("objectKey").asText();

                // ── Upload ──
                uploadToPresignedUrl(uploadUrl, response.body());

                // ── Store object key for later URL regeneration ──
                ifcFile.setPreviewObjectKey(actualObjectKey);
                String downloadUrl = getPresignedDownloadUrl(actualObjectKey, authorization, tenantId);

                ifcFile.setPreviewUrl(downloadUrl);
                ifcFile.setLastPresignedAt(LocalDateTime.now());
                ifcFile.setStatus(IfcFileStatus.COMPLETED);
                ifcFile = ifcFileRepository.save(ifcFile);
            } else {
                ifcFile.setStatus(IfcFileStatus.FAILED);
                ifcFileRepository.save(ifcFile);
                throw new RuntimeException("Preview failed: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Preview generation failed for file {}: {}", fileId, e.getMessage());
            ifcFile.setStatus(IfcFileStatus.FAILED);
            ifcFileRepository.save(ifcFile);
            throw e;
        }
        return toResponse(ifcFile);
    }
    @Transactional
    public ConstructionSiteIfcFileResponse triggerBlenderRender(UUID fileId, String view, String authorization, String tenantId, String projectId) throws Exception {
        ConstructionSiteIfcFile ifcFile = ifcFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("IFC file not found: " + fileId));
        ifcFile.setStatus(IfcFileStatus.PROCESSING);
        ifcFile.setBlenderRenderView(view);
        ifcFile = ifcFileRepository.save(ifcFile);
        try {
            String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFile.getIfcFileId() + "/blender-render?file_key=" + ifcFile.getObjectKey();
            HttpRequest.Builder blenderBuilder = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-Tenant-Id", effectiveTenantId(tenantId))
                    .header("X-Project-Id", projectId != null ? projectId : "")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .timeout(java.time.Duration.ofSeconds(600));
            if (authorization != null && !authorization.isBlank()) {
                blenderBuilder.header("Authorization", authorization);
            }
            HttpResponse<String> response = HTTP_CLIENT
                    .send(blenderBuilder.build(),
                            HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                String contentType = response.headers()
                        .firstValue("Content-Type")
                        .orElse("");
                if (!contentType.contains("application/json")
                        || responseBody == null || responseBody.isBlank()) {
                    log.error("Blender render service returned non-JSON response. "
                            + "Content-Type: {}, body (first 500 chars): {}",
                            contentType,
                            responseBody != null && responseBody.length() > 500
                                    ? responseBody.substring(0, 500)
                                    : responseBody);
                    ifcFile.setStatus(IfcFileStatus.FAILED);
                    ifcFileRepository.save(ifcFile);
                    throw new RuntimeException(
                            "Blender render service returned unexpected response format (expected JSON, got: "
                                    + contentType + ")");
                }
                JsonNode json = objectMapper.readTree(responseBody);
                String renderUrl = json.path("output_url").asText();
                if (renderUrl != null && !renderUrl.isBlank()) {
                    ifcFile.setBlenderRenderUrl(renderUrl);
                    ifcFile.setLastPresignedAt(LocalDateTime.now());
                }
                ifcFile.setStatus(IfcFileStatus.COMPLETED);
                ifcFile = ifcFileRepository.save(ifcFile);
            } else {
                log.error("Blender render service returned status {}: body={}",
                        response.statusCode(), response.body());
                ifcFile.setStatus(IfcFileStatus.FAILED);
                ifcFileRepository.save(ifcFile);
                throw new RuntimeException("Blender render failed: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Blender render failed for file {}: {}", fileId, e.getMessage());
            ifcFile.setStatus(IfcFileStatus.FAILED);
            ifcFileRepository.save(ifcFile);
            throw e;
        }
        return toResponse(ifcFile);
    }
    public Map<String, Object> getMetadata(String ifcFileId, String fileKey, String authorization) throws Exception {
        return getMetadata(ifcFileId, fileKey, authorization, null);
    }

    public Map<String, Object> getMetadata(String ifcFileId, String fileKey, String authorization, String tenantId) throws Exception {
        String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFileId + "/metadata?file_key=" + fileKey;
        HttpRequest.Builder metaBuilder = HttpRequest.newBuilder().uri(URI.create(url))
                .header("X-Tenant-Id", effectiveTenantId(tenantId))
                .timeout(java.time.Duration.ofSeconds(120));
        if (authorization != null && !authorization.isBlank()) {
            metaBuilder.header("Authorization", authorization);
        }
        HttpResponse<String> response = HTTP_CLIENT
                .send(metaBuilder.GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Metadata extraction failed: " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), Map.class);
    }

    public Map<String, Object> export3D(String ifcFileId, String format, String fileKey, String authorization) throws Exception {
        return export3D(ifcFileId, format, fileKey, authorization, null);
    }

    public Map<String, Object> export3D(String ifcFileId, String format, String fileKey, String authorization, String tenantId) throws Exception {
        return export3D(ifcFileId, format, fileKey, authorization, tenantId, null);
    }

    public Map<String, Object> export3D(String ifcFileId, String format, String fileKey, String authorization, String tenantId, String projectId) throws Exception {
        String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFileId + "/export?file_key=" + fileKey;
        String requestBody = objectMapper.writeValueAsString(Map.of("fileId", ifcFileId, "format", format));
        HttpRequest.Builder exportBuilder = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", effectiveTenantId(tenantId))
                .header("X-Project-Id", projectId != null ? projectId : "")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(java.time.Duration.ofSeconds(300));
        if (authorization != null && !authorization.isBlank()) {
            exportBuilder.header("Authorization", authorization);
        }
        HttpResponse<String> response = HTTP_CLIENT
                .send(exportBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("3D export failed: " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), Map.class);
    }

    @Transactional
    public BimIngestionResult generateObjects(UUID siteId, String fileId) throws Exception {
        ConstructionSiteIfcFile ifcFile = ifcFileRepository.findByConstructionSiteIdAndIfcFileId(siteId, fileId)
                .or(() -> ifcFileRepository.findById(UUID.fromString(fileId)))
                .orElseThrow(() -> new IllegalArgumentException("IFC file not found: " + fileId));
        log.info("Generating objects from IFC file {} for site {}", fileId, siteId);
        BimIngestionResult result = bimIngestionService.parseAndIngest(siteId, ifcFile.getObjectKey());
        ifcFile.setStatus(IfcFileStatus.COMPLETED);
        ifcFileRepository.save(ifcFile);
        return result;
    }

    @Transactional
    public void deleteIfcFile(UUID fileId) {
        ConstructionSiteIfcFile ifcFile = ifcFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("IFC file not found: " + fileId));
        ifcFileRepository.delete(ifcFile);
    }
    private ConstructionSiteIfcFileResponse toResponse(ConstructionSiteIfcFile ifcFile) {
        return new ConstructionSiteIfcFileResponse(
                ifcFile.getId(), ifcFile.getFileName(), ifcFile.getFileSize(),
                ifcFile.getFileKey(), ifcFile.getIfcFileId(),
                ifcFile.getStatus().name(), ifcFile.getPreviewUrl(),
                ifcFile.getMetadataUrl(), ifcFile.getExportUrl(),
                ifcFile.getBlenderRenderUrl(), ifcFile.getBlenderRenderView(),
                ifcFile.getLastPresignedAt(),
                ifcFile.getCreatedAt());
    }

    private String getPresignUploadUrl(String objectKey, String contentType, String authorization, String tenantId) throws Exception {
        return getPresignUploadUrl(objectKey, contentType, authorization, tenantId, "00000000-0000-0000-0000-000000000001");
    }

    private String getPresignUploadUrl(String objectKey, String contentType, String authorization, String tenantId, String projectId) throws Exception {
        String url = documentServiceUrl + "/api/v1/media/presign";
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "kind", "IFC_FILE",
                "fileName", objectKey.substring(objectKey.lastIndexOf('/') + 1),
                "contentType", contentType));
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", effectiveTenantId(tenantId))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(java.time.Duration.ofSeconds(30));
        if (authorization != null && !authorization.isBlank()) {
            builder.header("Authorization", authorization);
        }
        HttpResponse<String> response = HTTP_CLIENT
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Presign upload failed: " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.get("serverUploadUrl").asText();
    }

    /**
     * Calls the document-service presign endpoint, uploads data to the returned URL,
     * and returns the presigned download URL for the object that was just uploaded.
     *
     * <p>Unlike {@link #getPresignUploadUrl}, this method captures the actual
     * {@code objectKey} from the presign response so the subsequent download URL
     * matches exactly where the file was stored.</p>
     */
    private String uploadToMinioAndGetDownloadUrl(byte[] data, String contentType,
            String authorization, String tenantId, String projectId) throws Exception {
        String presignUrl = documentServiceUrl + "/api/v1/media/presign";
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "kind", "IFC_FILE",
                "fileName", "render.png",
                "contentType", contentType));

        HttpRequest.Builder presignBuilder = HttpRequest.newBuilder().uri(URI.create(presignUrl))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", effectiveTenantId(tenantId))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(java.time.Duration.ofSeconds(30));
        if (authorization != null && !authorization.isBlank()) {
            presignBuilder.header("Authorization", authorization);
        }

        HttpResponse<String> presignResponse = HTTP_CLIENT
                .send(presignBuilder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode presignJson = objectMapper.readTree(presignResponse.body());
        String uploadUrl = presignJson.get("serverUploadUrl").asText();
        String actualObjectKey = presignJson.get("objectKey").asText();

        uploadToPresignedUrl(uploadUrl, data);

        return getPresignedDownloadUrl(actualObjectKey, authorization, tenantId);
    }

    /**
     * Generate a fresh presigned download URL for a stored object key.
     * Called from the UI each time a user clicks "View Preview" / "3D Render" etc.
     */
    public String generateFreshPresignedUrl(UUID fileId, String urlType,
            String authorization, String tenantId) throws Exception {
        Optional<ConstructionSiteIfcFile> opt = ifcFileRepository.findById(fileId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("IFC file not found: " + fileId);
        }
        ConstructionSiteIfcFile ifcFile = opt.get();
        String objectKey = switch (urlType) {
            case "preview" -> ifcFile.getPreviewObjectKey();
            case "render" -> ifcFile.getRenderObjectKey();
            case "export" -> ifcFile.getExportObjectKey();
            case "metadata" -> ifcFile.getMetadataObjectKey();
            default -> throw new IllegalArgumentException("Unknown URL type: " + urlType);
        };
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalStateException("No object key stored for resource: " + urlType + " (file: " + fileId + ")");
        }
        return getPresignedDownloadUrl(objectKey, authorization, tenantId);
    }

    private void uploadToPresignedUrl(String url, byte[] data) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "image/png")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .timeout(java.time.Duration.ofMinutes(5)).build();
        HttpResponse<Void> response = HTTP_CLIENT
                .send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Upload failed: " + response.statusCode());
        }
    }

    private String getPresignedDownloadUrl(String objectKey, String authorization, String tenantId) throws Exception {
        String url = documentServiceUrl + "/api/v1/media/presign-download";
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", effectiveTenantId(tenantId))
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(Map.of("objectKey", objectKey))))
                .timeout(java.time.Duration.ofSeconds(30));
        if (authorization != null && !authorization.isBlank()) {
            builder.header("Authorization", authorization);
        }
        HttpResponse<String> response = HTTP_CLIENT
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Presign download failed: " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.get("url").asText();
    }
}
