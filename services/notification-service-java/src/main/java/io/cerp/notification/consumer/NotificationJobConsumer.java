package io.cerp.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cerp.notification.config.KafkaTopicConfig;
import io.cerp.notification.domain.Notification;
import io.cerp.notification.domain.NotificationActionType;
import io.cerp.notification.domain.NotificationJob;
import io.cerp.notification.domain.NotificationPriority;
import io.cerp.notification.domain.NotificationJobStatus;
import io.cerp.notification.domain.NotificationStatus;
import io.cerp.notification.event.NotificationJobCreateRequest;
import io.cerp.notification.event.NotificationJobCreated;
import io.cerp.notification.repository.NotificationJobRepository;
import io.cerp.notification.repository.NotificationRepository;
import io.cerp.notification.service.EventPublisher;
import io.cerp.notification.service.IdentityServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kafka consumer that listens for notification job creation requests from other services.
 * 
 * Other services publish to topic "notification-job-create-request":
 * 
 * {@code kafkaTemplate.send("notification-job-create-request", requestId, new NotificationJobCreateRequest(...));}
 * 
 * The consumer will:
 * 1. Resolve recipients via IdentityService (by email or role)
 * 2. Create a NotificationJob record
 * 3. Create individual Notification records for each resolved recipient
 * 4. Publish a NotificationJobCreated event to "notification-job-created" topic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationJobConsumer {

    private final NotificationJobRepository jobRepository;
    private final NotificationRepository notificationRepository;
    private final IdentityServiceClient identityServiceClient;
    private final EventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
        topics = "${notification.kafka.inbound.topic:notification-job-create-request}",
        groupId = "${notification.kafka.inbound.group-id:notification-service}",
        properties = {
            "spring.json.value.default.type=io.cerp.notification.event.NotificationJobCreateRequest"
        }
    )
    @Transactional
    public void handleJobCreateRequest(NotificationJobCreateRequest request) {
        log.info("Received notification job create request: requestId={}, code={}, title={}",
                request.requestId(), request.code(), request.title());

        try {
            // Resolve recipients by email and role
            List<UUID> resolvedUserIds = resolveRecipients(request);
            
            // Create job and notifications
            createJobWithNotifications(request, resolvedUserIds);
            
        } catch (Exception e) {
            log.error("Failed to process notification job create request: requestId={}",
                    request.requestId(), e);
        }
    }

    private List<UUID> resolveRecipients(NotificationJobCreateRequest request) {
        Set<UUID> userIds = new java.util.HashSet<>();

        // Resolve by email
        if (request.recipientEmails() != null && !request.recipientEmails().isEmpty()) {
            for (String email : request.recipientEmails()) {
                try {
                    UUID userId = identityServiceClient.resolveUserByEmail(email);
                    if (userId != null) {
                        userIds.add(userId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve user by email: {}", email);
                }
            }
        }

        // Resolve by role - get all users with the specified roles
        if (request.recipientRoles() != null && !request.recipientRoles().isEmpty()) {
            try {
                List<UUID> roleUserIds = identityServiceClient.resolveUsersByRoles(
                        request.recipientRoles(), 
                        request.tenantId() != null ? request.tenantId().toString() : null);
                if (roleUserIds != null) {
                    userIds.addAll(roleUserIds);
                }
            } catch (Exception e) {
                log.warn("Failed to resolve users by roles: {}", request.recipientRoles());
            }
        }

        return new ArrayList<>(userIds);
    }

    private void createJobWithNotifications(NotificationJobCreateRequest request, List<UUID> recipientIds) {
        // Create the job
        UUID jobId = UUID.randomUUID();
        String jobCode = request.code() != null ? request.code() : 
                         "job-" + jobId.toString().substring(0, 8);

        // Parse dueAt as String
        String dueAtStr = null;
        if (request.dueAt() != null) {
            dueAtStr = request.dueAt().toString();
        }

        // Create job using builder
        NotificationJob job = NotificationJob.builder()
                .id(jobId)
                .tenantId(request.tenantId() != null ? request.tenantId().toString() : "default")
                .code(jobCode)
                .title(request.title())
                .description(request.description())
                .actionType(parseActionType(request.actionType()))
                .payload(request.payload())
                .sourceService(request.sourceService())
                .sourceEntityId(parseSourceEntityId(request.sourceEntityId()))
                .sourceEntityType(request.sourceEntityType())
                .priority(parsePriority(request.priority()))
                .status(recipientIds.isEmpty() ? NotificationJobStatus.DRAFT : NotificationJobStatus.PUBLISHED)
                .recipientUserIds(new ArrayList<>())
                .recipientRoles(request.recipientRoles() != null ? new ArrayList<>(request.recipientRoles()) : new ArrayList<>())
                .dueAt(dueAtStr)
                .reminderIntervalMinutes(request.reminderIntervalMinutes())
                .build();

        job = jobRepository.save(job);
        log.info("Created notification job: jobId={}, code={}, status={}", jobId, jobCode, job.getStatus());

        // Create individual notifications for each recipient
        String expiresAtStr = null;
        if (request.dueAt() != null) {
            LocalDateTime expiresAt = request.dueAt().minusHours(1);
            expiresAtStr = expiresAt.toString();
        }

        List<Notification> notifications = new ArrayList<>();
        for (UUID recipientId : recipientIds) {
            Notification notification = Notification.builder()
                    .id(UUID.randomUUID())
                    .job(job)
                    .tenantId(request.tenantId() != null ? request.tenantId().toString() : "default")
                    .recipientId(recipientId)
                    .status(NotificationStatus.PENDING)
                    .expiresAt(expiresAtStr)
                    .build();
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("Created {} notifications for job: {}", notifications.size(), jobId);
        }

        // Publish outbound event
        eventPublisher.publishJobCreated(NotificationJobCreated.builder()
            .jobId(job.getId())
            .code(job.getCode())
            .tenantId(job.getTenantId())
            .title(job.getTitle())
            .description(job.getDescription())
            .actionType(job.getActionType().name())
            .priority(job.getPriority() != null ? job.getPriority().name() : "MEDIUM")
            .createdBy(job.getCreatedBy() != null ? job.getCreatedBy().toString() : null)
            .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
            .build());

        log.info("Published notification job created event for job: {}", jobId);
    }

    private NotificationActionType parseActionType(String actionType) {
        if (actionType == null) {
            return NotificationActionType.ACKNOWLEDGE;
        }
        try {
            return NotificationActionType.valueOf(actionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown action type: {}, defaulting to ACKNOWLEDGE", actionType);
            return NotificationActionType.ACKNOWLEDGE;
        }
    }

    private NotificationPriority parsePriority(String priority) {
        if (priority == null) {
            return NotificationPriority.MEDIUM;
        }
        try {
            return NotificationPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown priority: {}, defaulting to MEDIUM", priority);
            return NotificationPriority.MEDIUM;
        }
    }

    private UUID parseSourceEntityId(String entityId) {
        if (entityId == null) {
            return null;
        }
        try {
            return UUID.fromString(entityId);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse source entity ID as UUID: {}", entityId);
            return null;
        }
    }
}