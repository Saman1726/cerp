package io.cerp.notification.dto;

import io.cerp.notification.domain.NotificationActionType;
import io.cerp.notification.domain.NotificationJobStatus;
import io.cerp.notification.domain.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for notification job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJobResponse {

    private UUID id;
    private String tenantId;
    private String code;
    private String title;
    private String description;
    private NotificationActionType actionType;
    private Object payload;
    private String sourceService;
    private UUID sourceEntityId;
    private String sourceEntityType;
    private NotificationPriority priority;
    private NotificationJobStatus status;
    private List<String> recipientUserIds;
    private List<String> recipientRoles;
    private String dueAt;
    private Integer reminderIntervalMinutes;
    private UUID createdBy;
    private String createdAt;
    private String publishedAt;
    private String completedAt;
    private String cancelledAt;
}