package io.cerp.notification.controller;

import io.cerp.notification.config.TenantContext;
import io.cerp.notification.domain.NotificationJobStatus;
import io.cerp.notification.dto.NotificationJobCreateRequest;
import io.cerp.notification.dto.NotificationJobResponse;
import io.cerp.notification.dto.NotificationListResponse;
import io.cerp.notification.service.NotificationJobService;
import io.cerp.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for notification job management.
 */
@RestController
@RequestMapping("/api/notifications/jobs")
@RequiredArgsConstructor
@Tag(name = "Notification Jobs", description = "API for managing notification jobs")
public class NotificationJobController {

    private final NotificationJobService jobService;
    private final NotificationService notificationService;

    /**
     * Create a new notification job.
     */
    @PostMapping
    @Operation(summary = "Create a notification job", description = "Create a new notification job in DRAFT state")
    public ResponseEntity<NotificationJobResponse> createJob(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody @Valid NotificationJobCreateRequest request) {
        
        UUID createdBy = extractUserId();
        NotificationJobResponse response = jobService.createJob(tenantId, request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a notification job by ID.
     */
    @GetMapping("/{jobId}")
    @Operation(summary = "Get a notification job", description = "Get details of a specific notification job")
    public ResponseEntity<NotificationJobResponse> getJob(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID jobId) {
        
        NotificationJobResponse response = jobService.getJob(tenantId, jobId);
        return ResponseEntity.ok(response);
    }

    /**
     * List notification jobs with pagination.
     */
    @GetMapping
    @Operation(summary = "List notification jobs", description = "List all notification jobs with pagination")
    public ResponseEntity<List<NotificationJobResponse>> listJobs(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) NotificationJobStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        List<NotificationJobResponse> jobs;
        if (status != null) {
            jobs = jobService.listJobsByStatus(tenantId, status, pageable).getContent();
        } else {
            jobs = jobService.listJobs(tenantId, pageable).getContent();
        }
        return ResponseEntity.ok(jobs);
    }

    /**
     * Publish a job - create notifications for all recipients.
     */
    @PostMapping("/{jobId}/publish")
    @Operation(summary = "Publish a notification job", description = "Publish a DRAFT job to create notifications for recipients")
    public ResponseEntity<NotificationJobResponse> publishJob(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID jobId) {
        
        UUID actedBy = extractUserId();
        NotificationJobResponse response = jobService.publishJob(tenantId, jobId, actedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a notification job.
     */
    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a notification job", description = "Cancel a notification job")
    public ResponseEntity<NotificationJobResponse> cancelJob(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID jobId) {
        
        NotificationJobResponse response = jobService.cancelJob(tenantId, jobId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get notifications for a specific job.
     */
    @GetMapping("/{jobId}/notifications")
    @Operation(summary = "Get job notifications", description = "Get all notifications created by a job")
    public ResponseEntity<List<io.cerp.notification.dto.NotificationResponse>> getJobNotifications(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID jobId) {
        
        List<io.cerp.notification.dto.NotificationResponse> notifications = 
                jobService.getJobNotifications(tenantId, jobId);
        return ResponseEntity.ok(notifications);
    }

    private UUID extractUserId() {
        String userIdStr = TenantContext.getUserId();
        if (userIdStr != null && userIdStr.matches("[0-9a-fA-F-]{36}")) {
            return UUID.fromString(userIdStr);
        }
        return UUID.randomUUID();
    }
}