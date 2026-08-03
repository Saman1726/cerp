package io.cerp.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a user opens a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOpened {
    
    private String eventId;
    
    private UUID notificationId;
    
    private UUID userId;
    
    private String tenantId;
    
    private String sourceEntityId;
    
    private String sourceEntityType;
    
    private LocalDateTime occurredAt;
}