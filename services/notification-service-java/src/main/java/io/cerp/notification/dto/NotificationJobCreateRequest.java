package io.cerp.notification.dto;

import io.cerp.notification.domain.NotificationActionType;
import io.cerp.notification.domain.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new notification job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJobCreateRequest {

    @NotBlank(message = "Job code is required")
    private String code;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Action type is required")
    private NotificationActionType actionType;

    private Object payload;

    private String sourceService;

    private UUID sourceEntityId;

    private String sourceEntityType;

    private NotificationPriority priority;

    @NotNull(message = "At least one recipient is required")
    private List<String> recipientUserIds;

    private List<String> recipientRoles;

    private String dueAt;

    private Integer reminderIntervalMinutes;
}