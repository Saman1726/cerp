package io.cerp.workflow.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Default implementation of {@link NotificationServiceClient} that communicates
 * with the notification-service-java microservice via REST.
 */
@Service
public class NotificationServiceClientImpl implements NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public NotificationServiceClientImpl(
            @Value("${cerp.notification-service.url:http://localhost:8082}") String notificationServiceUrl) {
        this.notificationServiceUrl = notificationServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendNotification(String userId, String title, String message, String type, Map<String, Object> data) {
        sendNotification(userId, null, title, message, type, data);
    }

    @Override
    public void sendNotification(String userId, UUID tenantId, String title, String message, String type,
            Map<String, Object> data) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", userId,
                    "tenantId", tenantId != null ? tenantId.toString() : null,
                    "title", title,
                    "message", message,
                    "type", type,
                    "data", data != null ? data : Map.of());

            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications",
                    payload,
                    String.class);

            log.info("Notification sent to userId={}, title={}", userId, title);
        } catch (Exception e) {
            log.warn("Failed to send notification to userId={}, title={}: {}", userId, title, e.getMessage());
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            restTemplate.getForObject(notificationServiceUrl + "/actuator/health", String.class);
            return true;
        } catch (Exception e) {
            log.debug("Notification service health check failed: {}", e.getMessage());
            return false;
        }
    }
}