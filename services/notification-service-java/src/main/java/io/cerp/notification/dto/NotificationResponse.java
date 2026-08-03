package io.cerp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response object for a single notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private UUID id;
    
    private String code;
    
    private String title;
    
    private String description;
    
    private String actionType;
    
    private String status;
    
    private String priority;
    
    private UUID jobId;
    
    private UUID recipientId;
    
    private String recipientEmail;
    
    private Map<String, Object> payload;
    
    private String sourceEntityId;
    
    private String sourceEntityType;

    private String recipientRole;

    private Object actionResult;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime openedAt;
    
    private LocalDateTime acknowledgedAt;
    
    private LocalDateTime completedAt;
    
    private String dueAt;
}