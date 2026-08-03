package io.cerp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for acknowledging a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAcknowledgeRequest {
    
    private String read;
}