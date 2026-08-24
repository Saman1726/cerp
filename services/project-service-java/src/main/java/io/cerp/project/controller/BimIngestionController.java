package io.cerp.project.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.project.dto.BimIngestionRequest;
import io.cerp.project.service.BimIngestionResult;
import io.cerp.project.service.BimIngestionService;

/**
 * REST controller for BIM (Building Information Modeling) ingestion.
 * Accepts presigned IFC URLs and ingests building/floor/unit hierarchy.
 */
@RestController
@RequestMapping("/api/v1/construction-sites")
public class BimIngestionController {

    private static final Logger log = LoggerFactory.getLogger(BimIngestionController.class);

    private final BimIngestionService bimIngestionService;

    public BimIngestionController(BimIngestionService bimIngestionService) {
        this.bimIngestionService = bimIngestionService;
    }

    /**
     * Ingest an IFC file from a presigned URL into the specified construction site.
     */
    @PostMapping("/{siteId}/bim/ingest")
    public ResponseEntity<BimIngestionResult> ingestBim(
            @Valid @RequestBody BimIngestionRequest request) throws Exception {
        log.info("BIM ingestion request: siteId={}, objectKey={}", request.constructionSiteId(), request.objectKey());
        BimIngestionResult result = bimIngestionService.parseAndIngest(
                request.constructionSiteId(), request.objectKey());
        return ResponseEntity.ok(result);
    }
}