package io.cerp.notification.service;

import io.cerp.notification.domain.Notification;
import io.cerp.notification.domain.NotificationStatus;
import io.cerp.notification.domain.NotificationJobStatus;
import io.cerp.notification.domain.NotificationActionType;
import io.cerp.notification.dto.*;
import io.cerp.notification.event.NotificationOpened;
import io.cerp.notification.event.NotificationActionTaken;
import io.cerp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventPublisher eventPublisher;

    /**
     * Get notifications for the current user.
     */
    public NotificationListResponse getMyNotifications(String tenantId, UUID userId,
                                                        NotificationStatus status,
                                                        int page, int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<Notification> notifications;

        if (status != null) {
            notifications = notificationRepository.findByRecipientIdAndStatus(tenantId, userId, status, pageable);
        } else {
            notifications = notificationRepository.findByRecipientId(tenantId, userId, pageable);
        }

        List<NotificationResponse> response = notifications.getContent().stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());

        return NotificationListResponse.builder()
                .notifications(response)
                .total((int) notifications.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(notifications.getTotalPages())
                .last(notifications.isLast())
                .build();
    }

    /**
     * Get unread notification count for a user.
     */
    public long getUnreadCount(String tenantId, UUID userId) {
        return notificationRepository.countPendingNotifications(tenantId, userId);
    }

    /**
     * Acknowledge (mark as read) a notification.
     */
    @Transactional
    public NotificationResponse acknowledgeNotification(String tenantId, UUID userId, UUID notificationId) {
        Notification notification = findNotificationByTenantAndRecipient(tenantId, userId, notificationId);

        notification.acknowledge();
        notification = notificationRepository.save(notification);

        // Publish event
        NotificationOpened event = NotificationOpened.builder()
                .notificationId(notificationId)
                .userId(userId)
                .tenantId(tenantId)
                .sourceEntityId(notification.getJob().getSourceEntityId() != null ? notification.getJob().getSourceEntityId().toString() : null)
                .sourceEntityType(notification.getJob().getSourceEntityType())
                .occurredAt(LocalDateTime.now())
                .build();

        eventPublisher.publishNotificationOpened(event);

        log.info("Notification acknowledged: notificationId={}", notificationId);
        return toNotificationResponse(notification);
    }

    /**
     * Take action on a notification.
     */
    @Transactional
    public NotificationResponse takeAction(String tenantId, UUID userId, UUID notificationId,
                                            NotificationActionType actionType,
                                            Object result) {
        Notification notification = findNotificationByTenantAndRecipient(tenantId, userId, notificationId);

        notification.takeAction(userId, result);
        notification = notificationRepository.save(notification);

        // Update job status
        io.cerp.notification.domain.NotificationJob job = notification.getJob();
        boolean allDone = job.getNotifications().stream()
                .allMatch(n -> n.getStatus() == NotificationStatus.ACTION_TAKEN
                        || n.getStatus() == NotificationStatus.ARCHIVED);

        if (allDone) {
            job.complete();
        } else if (job.getNotifications().stream().anyMatch(
                n -> n.getStatus() == NotificationStatus.ACTION_TAKEN)) {
            job.setStatus(NotificationJobStatus.PARTIALLY_COMPLETED);
        }

        // Publish event
        NotificationActionTaken event = NotificationActionTaken.builder()
                .notificationId(notificationId)
                .userId(userId)
                .tenantId(tenantId)
                .actionType(actionType != null ? actionType.name() : notification.getJob().getActionType().name())
                .result(notification.getActionResult() != null ? String.valueOf(notification.getActionResult()) : null)
                .sourceEntityId(notification.getJob().getSourceEntityId() != null ? notification.getJob().getSourceEntityId().toString() : null)
                .sourceEntityType(notification.getJob().getSourceEntityType())
                .occurredAt(LocalDateTime.now())
                .build();

        eventPublisher.publishNotificationActionTaken(event);

        log.info("Action taken on notification: notificationId={} result={}", notificationId, result);
        return toNotificationResponse(notification);
    }

    /**
     * Get a specific notification by ID.
     */
    public NotificationResponse getNotification(String tenantId, UUID userId, UUID notificationId) {
        Notification notification = findNotificationByTenantAndRecipient(tenantId, userId, notificationId);
        return toNotificationResponse(notification);
    }

    private Notification findNotificationByTenantAndRecipient(String tenantId, UUID userId, UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getTenantId().equals(tenantId) && n.getRecipientId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        io.cerp.notification.domain.NotificationJob job = notification.getJob();
        return NotificationResponse.builder()
                .id(notification.getId())
                .code(job.getCode())
                .title(job.getTitle())
                .description(job.getDescription())
                .actionType(job.getActionType() != null ? job.getActionType().name() : null)
                .status(notification.getStatus() != null ? notification.getStatus().name() : null)
                .priority(job.getPriority() != null ? job.getPriority().name() : null)
                .jobId(job.getId())
                .recipientId(notification.getRecipientId())
                .recipientRole(notification.getRecipientRole())
                .actionResult(notification.getActionResult())
                .sourceEntityId(job.getSourceEntityId() != null ? job.getSourceEntityId().toString() : null)
                .sourceEntityType(job.getSourceEntityType())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .openedAt(notification.getReadAt())
                .acknowledgedAt(notification.getAcknowledgedAt())
                .completedAt(notification.getActionTakenAt())
                .dueAt(notification.getExpiresAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> convertToMap(Object result) {
        if (result instanceof java.util.Map) {
            return (java.util.Map<String, Object>) result;
        }
        return java.util.Map.of("value", String.valueOf(result));
    }
}