package io.cerp.notification.controller;

import io.cerp.notification.config.TenantContext;
import io.cerp.notification.domain.NotificationActionType;
import io.cerp.notification.domain.NotificationStatus;
import io.cerp.notification.dto.NotificationAcknowledgeRequest;
import io.cerp.notification.dto.NotificationActionRequest;
import io.cerp.notification.dto.NotificationListResponse;
import io.cerp.notification.dto.NotificationResponse;
import io.cerp.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user notification operations.
 */
@RestController
@RequestMapping("/api/notifications/me")
@RequiredArgsConstructor
@Tag(name = "User Notifications", description = "API for managing user notifications")
public class UserNotificationController {

    private final NotificationService notificationService;

    /**
     * Get current user's notifications.
     */
    @GetMapping
    @Operation(summary = "Get my notifications", description = "Get notifications for the current user with pagination")
    public ResponseEntity<NotificationListResponse> getMyNotifications(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = extractUserId();
        NotificationListResponse response = notificationService.getMyNotifications(tenantId, userId, status, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get unread notification count.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get the count of unread notifications for the current user")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId) {
        
        UUID userId = extractUserId();
        long count = notificationService.getUnreadCount(tenantId, userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Acknowledge a notification.
     */
    @PostMapping("/{notificationId}/ack")
    @Operation(summary = "Acknowledge a notification", description = "Mark a notification as read/acknowledged")
    public ResponseEntity<NotificationResponse> acknowledgeNotification(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID notificationId,
            @RequestBody(required = false) NotificationAcknowledgeRequest request) {
        
        UUID userId = extractUserId();
        NotificationResponse response = notificationService.acknowledgeNotification(tenantId, userId, notificationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Take action on a notification.
     */
    @PostMapping("/{notificationId}/action")
    @Operation(summary = "Take action on a notification", description = "Execute the required action on a notification")
    public ResponseEntity<NotificationResponse> takeAction(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID notificationId,
            @RequestBody @Valid NotificationActionRequest request) {
        
        UUID userId = extractUserId();
        NotificationResponse response = notificationService.takeAction(
                tenantId, userId, notificationId, null, request.getResult());
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific notification.
     */
    @GetMapping("/{notificationId}")
    @Operation(summary = "Get a notification", description = "Get details of a specific notification")
    public ResponseEntity<NotificationResponse> getNotification(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID notificationId) {
        
        UUID userId = extractUserId();
        NotificationResponse response = notificationService.getNotification(tenantId, userId, notificationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is healthy");
    }

    private UUID extractUserId() {
        String userIdStr = TenantContext.getTenantId();
        if (userIdStr != null && userIdStr.matches("[0-9a-fA-F-]{36}")) {
            return UUID.fromString(userIdStr);
        }
        return UUID.randomUUID();
    }
}