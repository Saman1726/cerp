package io.cerp.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Domain event published when a notification job is created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJobCreated {

    private UUID jobId;
    private String tenantId;
    private String code;
    private String title;
    private String description;
    private String actionType;
    private String priority;
    private String createdBy;
    private String createdAt;
}