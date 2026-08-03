package io.cerp.notification.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published by other services to request notification job creation.
 * 
 * Usage example - a service publishes this to Kafka topic "notification-job-create-request":
 * 
 * {@code
 * kafkaTemplate.send("notification-job-create-request", requestId, new NotificationJobCreateRequest(
 *     UUID.randomUUID(),
 *     "purchase-order-review",
 *     "Review new purchase order #123",
 *     "Please review the new purchase order submitted by vendor.",
 *     "REVIEW",
 *     List.of("user1@example.com", "user2@example.com"),
 *     List.of("PROCUREMENT_MANAGER"),
 *     UUID.fromString("procurement"),
 *     "purchase-order",
 *     orderId,
 *     Map.of("orderId", orderId.toString(), "amount", "1500.00"),
 *     "HIGH",
 *     dueDate,
 *     60,
 *     "purchase-service"
 * ));
 * }
 */
public record NotificationJobCreateRequest(
    String requestId,
    String code,
    String title,
    String description,
    String actionType,
    List<String> recipientEmails,
    List<String> recipientRoles,
    UUID tenantId,
    String sourceEntityId,
    String sourceEntityType,
    java.util.Map<String, Object> payload,
    String priority,
    LocalDateTime dueAt,
    Integer reminderIntervalMinutes,
    String sourceService
) {
    public NotificationJobCreateRequest {
        if (recipientEmails == null && recipientRoles == null) {
            throw new IllegalArgumentException("At least one recipient (email or role) must be specified");
        }
    }
}