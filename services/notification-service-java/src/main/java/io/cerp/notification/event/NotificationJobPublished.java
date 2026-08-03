package io.cerp.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Event published when a notification job is published (notifications created for recipients).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJobPublished {

    private UUID jobId;
    private String tenantId;
    private String code;
    private String title;
    private List<String> notificationIds;
    private List<UUID> recipientIds;
    private String publishedAt;
}