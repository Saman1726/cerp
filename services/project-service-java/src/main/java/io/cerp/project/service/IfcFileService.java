package io.cerp.project.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        return toResponse(saved);
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
        ifcFile.setStatus(IfcFileStatus.PROCESSING);
        ifcFile = ifcFileRepository.save(ifcFile);
        try {
            String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFile.getIfcFileId() + "/preview?file_key=" + ifcFile.getObjectKey();
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder().uri(URI.create(url)).GET()
                            .timeout(java.time.Duration.ofSeconds(120)).build(),
                            HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                String previewKey = "renders/" + ifcFile.getId() + "/preview.png";
                String presignUrl = getPresignUploadUrl(previewKey, "image/png", authorization, tenantId);
                uploadToPresignedUrl(presignUrl, response.body());
                String downloadUrl = getPresignedDownloadUrl(previewKey, authorization, tenantId);
                ifcFile.setPreviewUrl(downloadUrl);
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
    public ConstructionSiteIfcFileResponse triggerBlenderRender(UUID fileId, String view, String authorization, String tenantId) throws Exception {
        ConstructionSiteIfcFile ifcFile = ifcFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("IFC file not found: " + fileId));
        ifcFile.setStatus(IfcFileStatus.PROCESSING);
        ifcFile.setBlenderRenderView(view);
        ifcFile = ifcFileRepository.save(ifcFile);
        try {
            String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFile.getIfcFileId() + "/blender-render?file_key=" + ifcFile.getObjectKey();
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder().uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{}"))
                            .timeout(java.time.Duration.ofSeconds(600)).build(),
                            HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                String renderKey = "renders/" + ifcFile.getId() + "/" + view + ".png";
                String presignUrl = getPresignUploadUrl(renderKey, "image/png", authorization, tenantId);
                uploadToPresignedUrl(presignUrl, response.body());
                String downloadUrl = getPresignedDownloadUrl(renderKey, authorization, tenantId);
                ifcFile.setBlenderRenderUrl(downloadUrl);
                ifcFile.setStatus(IfcFileStatus.COMPLETED);
                ifcFile = ifcFileRepository.save(ifcFile);
            } else {
                ifcFile.setStatus(IfcFileStatus.FAILED);
                ifcFileRepository.save(ifcFile);
                throw new RuntimeException("Blender render failed: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Blender render failed for file {}: {}", fileId, e.getMessage());
            ifcFile.setStatus(IfcFileStatus.FAILED);
            ifcFileRepository.save(ifcFile);
            throw e;
        }
        return toResponse(ifcFile);
    }
    public Map<String, Object> getMetadata(String ifcFileId, String fileKey) throws Exception {
        String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFileId + "/metadata?file_key=" + fileKey;
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder().uri(URI.create(url)).GET()
                        .timeout(java.time.Duration.ofSeconds(120)).build(),
                        HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Metadata extraction failed: " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), Map.class);
    }

    public Map<String, Object> export3D(String ifcFileId, String format, String fileKey) throws Exception {
        String url = ifcRendererUrl + "/api/v1/ifc/" + ifcFileId + "/export?file_key=" + fileKey;
        String requestBody = objectMapper.writeValueAsString(Map.of("fileId", ifcFileId, "format", format));
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder().uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(java.time.Duration.ofSeconds(300)).build(),
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
                ifcFile.getCreatedAt());
    }

    private String getPresignUploadUrl(String objectKey, String contentType, String authorization, String tenantId) throws Exception {
        String url = documentServiceUrl + "/api/v1/media/presign";
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "projectId", "00000000-0000-0000-0000-000000000001",
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
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Presign upload failed: " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.get("serverUploadUrl").asText();
    }

    private void uploadToPresignedUrl(String url, byte[] data) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "image/png")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .timeout(java.time.Duration.ofMinutes(5)).build();
        HttpResponse<Void> response = HttpClient.newHttpClient()
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
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Presign download failed: " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.get("url").asText();
    }
}
