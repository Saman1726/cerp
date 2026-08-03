package io.cerp.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a user takes action on a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationActionTaken {
    
    private String eventId;
    
    private UUID notificationId;
    
    private UUID userId;
    
    private String tenantId;
    
    private String actionType;
    
    private String result;
    
    private String comment;
    
    private String sourceEntityId;
    
    private String sourceEntityType;
    
    private Map<String, Object> actionData;
    
    private LocalDateTime occurredAt;
}