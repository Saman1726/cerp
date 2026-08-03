package io.cerp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for taking action on a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationActionRequest {
    
    private String result;
    
    private String comment;
}