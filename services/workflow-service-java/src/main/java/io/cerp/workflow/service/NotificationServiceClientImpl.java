package io.cerp.workflow.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Default implementation of {@link NotificationServiceClient} that communicates
 * with the notification-service-java microservice via REST.
 */
@Service
public class NotificationServiceClientImpl implements NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClientImpl.class);

    private final RestClient restClient;

    public NotificationServiceClientImpl(
            @Value("${cerp.notification-service.url:http://localhost:8082}") String notificationServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(notificationServiceUrl).build();
    }

    @Override
    public void sendNotification(String userId, String title, String message, String type, Map<String, Object> data) {
        sendNotification(userId, null, title, message, type, data);
    }

    @Override
    public void sendNotification(String userId, UUID tenantId, String title, String message, String type,
            Map<String, Object> data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("code", type != null ? type.toLowerCase().replace(" ", "_") : "notification");
            payload.put("title", title);
            payload.put("description", message);
            payload.put("actionType", "ACKNOWLEDGE");
            payload.put("recipientUserIds", List.of(userId));
            payload.put("sourceService", "workflow-service");
            if (data != null && !data.isEmpty()) {
                payload.put("payload", data);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> created = restClient.post()
                    .uri("/api/notifications/jobs")
                    .header("X-Tenant-Id", tenantId != null ? tenantId.toString() : "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (created != null && created.get("id") != null) {
                String jobId = created.get("id").toString();
                restClient.post()
                        .uri("/api/notifications/jobs/{jobId}/publish", jobId)
                        .header("X-Tenant-Id", tenantId != null ? tenantId.toString() : "")
                        .retrieve()
                        .toBodilessEntity();
                log.info("Notification job created and published: jobId={}, userId={}, title={}", jobId, userId, title);
            } else {
                log.warn("Notification job created but no id returned, skipping publish: userId={}, title={}", userId, title);
            }
        } catch (Exception e) {
            log.warn("Failed to send notification to userId={}, title={}: {}", userId, title, e.getMessage());
        }
    }

    @Override
    public void sendNotificationToRoles(UUID tenantId, String title, String message, String code,
                                         List<String> roles, Map<String, Object> data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("code", code != null ? code.toLowerCase().replace(" ", "_") : "notification");
            payload.put("title", title);
            payload.put("description", message);
            payload.put("actionType", "EXECUTE");
            payload.put("recipientRoles", roles);
            payload.put("sourceService", "workflow-service");
            if (data != null && !data.isEmpty()) {
                payload.put("payload", data);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> created = restClient.post()
                    .uri("/api/notifications/jobs")
                    .header("X-Tenant-Id", tenantId != null ? tenantId.toString() : "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (created != null && created.get("id") != null) {
                String jobId = created.get("id").toString();
                restClient.post()
                        .uri("/api/notifications/jobs/{jobId}/publish", jobId)
                        .header("X-Tenant-Id", tenantId != null ? tenantId.toString() : "")
                        .retrieve()
                        .toBodilessEntity();
                log.info("Role broadcast notification published: jobId={}, roles={}, title={}", jobId, roles, title);
            } else {
                log.warn("Role broadcast job created but no id returned: roles={}, title={}", roles, title);
            }
        } catch (Exception e) {
            log.warn("Failed to send role broadcast notification: roles={}, title={}: {}", roles, title, e.getMessage());
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            restClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.debug("Notification service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
