package io.cerp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for user notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {
    
    private List<NotificationResponse> notifications;
    
    private int page;
    
    private int size;
    
    private long total;
    
    private int totalPages;
    
    private boolean last;
}