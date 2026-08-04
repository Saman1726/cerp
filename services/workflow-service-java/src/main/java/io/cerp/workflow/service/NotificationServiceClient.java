package io.cerp.workflow.service;

import java.util.Map;
import java.util.UUID;

/**
 * Client for sending notifications via the notification service.
 * This interface abstracts communication with the notification-service-java microservice.
 */
public interface NotificationServiceClient {

    /**
     * Send a notification to a user.
     *
     * @param userId   the recipient user ID
     * @param title    the notification title
     * @param message  the notification body
     * @param type     the notification type (e.g., "INFO", "WARNING", "ERROR", "SUCCESS")
     * @param data     additional contextual data
     */
    void sendNotification(String userId, String title, String message, String type, Map<String, Object> data);

    /**
     * Send a notification to a user with a specific tenant context.
     *
     * @param userId   the recipient user ID
     * @param tenantId the tenant ID
     * @param title    the notification title
     * @param message  the notification body
     * @param type     the notification type
     * @param data     additional contextual data
     */
    void sendNotification(String userId, UUID tenantId, String title, String message, String type, Map<String, Object> data);

    /**
     * Check if the notification service is healthy.
     *
     * @return true if the service is available, false otherwise
     */
    boolean isHealthy();
}