package io.cerp.notification.service;

import io.cerp.notification.domain.*;
import io.cerp.notification.dto.*;
import io.cerp.notification.event.NotificationJobCreated;
import io.cerp.notification.event.NotificationJobPublished;
import io.cerp.notification.repository.NotificationJobRepository;
import io.cerp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing notification jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationJobService {

    private final NotificationJobRepository jobRepository;
    private final NotificationRepository notificationRepository;
    private final EventPublisher eventPublisher;
    private final IdentityServiceClient identityServiceClient;

    /**
     * Create a new notification job in DRAFT state.
     */
    @Transactional
    public NotificationJobResponse createJob(String tenantId, NotificationJobCreateRequest request, UUID createdBy) {
        UUID jobId = UUID.randomUUID();
        String code = request.getCode() != null ? request.getCode() : "job-" + jobId.toString().substring(0, 8);

        NotificationJob job = NotificationJob.builder()
                .id(jobId)
                .tenantId(tenantId)
                .code(code)
                .title(request.getTitle())
                .description(request.getDescription())
                .actionType(request.getActionType() != null ? request.getActionType() : NotificationActionType.ACKNOWLEDGE)
                .status(NotificationJobStatus.DRAFT)
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
                .sourceService(request.getSourceService())
                .sourceEntityId(request.getSourceEntityId())
                .sourceEntityType(request.getSourceEntityType())
                .payload(request.getPayload())
                .recipientUserIds(request.getRecipientUserIds() != null ? new ArrayList<>(request.getRecipientUserIds()) : new ArrayList<>())
                .recipientRoles(request.getRecipientRoles() != null ? new ArrayList<>(request.getRecipientRoles()) : new ArrayList<>())
                .dueAt(request.getDueAt())
                .reminderIntervalMinutes(request.getReminderIntervalMinutes())
                .createdBy(createdBy)
                .build();

        job = jobRepository.save(job);

        // Publish domain event
        NotificationJobCreated domainEvent = NotificationJobCreated.builder()
                .jobId(jobId)
                .tenantId(tenantId)
                .code(code)
                .title(job.getTitle())
                .description(job.getDescription())
                .actionType(job.getActionType().name())
                .priority(job.getPriority().name())
                .createdBy(createdBy.toString())
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        eventPublisher.publishJobCreated(domainEvent);

        log.info("Notification job created: jobId={} code={}", jobId, code);
        return toJobResponse(job);
    }

    /**
     * Get a job by ID.
     */
    public NotificationJobResponse getJob(String tenantId, UUID jobId) {
        NotificationJob job = findJobByTenant(tenantId, jobId);
        return toJobResponse(job);
    }

    /**
     * List jobs with pagination.
     */
    public Page<NotificationJobResponse> listJobs(String tenantId, Pageable pageable) {
        return jobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toJobResponse);
    }

    /**
     * List jobs by status.
     */
    public Page<NotificationJobResponse> listJobsByStatus(String tenantId, NotificationJobStatus status, Pageable pageable) {
        return jobRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable)
                .map(this::toJobResponse);
    }

    /**
     * Publish a job - create notifications for all recipients.
     */
    @Transactional
    public NotificationJobResponse publishJob(String tenantId, UUID jobId, UUID actedBy) {
        NotificationJob job = findJobByTenant(tenantId, jobId);

        if (job.getStatus() != NotificationJobStatus.DRAFT && job.getStatus() != NotificationJobStatus.PENDING) {
            throw new RuntimeException("Job cannot be published in state: " + job.getStatus());
        }

        job.publish();

        // Create individual notifications for each recipient
        List<UUID> recipientIds = new ArrayList<>();
        if (job.getRecipientUserIds() != null && !job.getRecipientUserIds().isEmpty()) {
            for (String uid : job.getRecipientUserIds()) {
                try {
                    recipientIds.add(UUID.fromString(uid));
                } catch (Exception e) {
                    log.warn("Invalid UUID in recipientUserIds: {}", uid);
                }
            }
        }
        
        if (recipientIds.isEmpty() && job.getRecipientRoles() != null && !job.getRecipientRoles().isEmpty()) {
            recipientIds.addAll(identityServiceClient.resolveUsersByRoles(job.getRecipientRoles(), job.getTenantId()));
        }

        List<String> notificationIds = new ArrayList<>();
        for (UUID recipientId : recipientIds) {
            Notification notification = Notification.builder()
                    .id(UUID.randomUUID())
                    .tenantId(job.getTenantId())
                    .job(job)
                    .recipientId(recipientId)
                    .status(NotificationStatus.PENDING)
                    .priority(job.getPriority())
                    .expiresAt(job.getDueAt())
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            notificationIds.add(notification.getId().toString());
        }

        // Publish event
        NotificationJobPublished event = NotificationJobPublished.builder()
                .jobId(jobId)
                .tenantId(tenantId)
                .code(job.getCode())
                .title(job.getTitle())
                .notificationIds(notificationIds)
                .recipientIds(recipientIds)
                .publishedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        eventPublisher.publishNotificationJobPublished(event);

        log.info("Notification job published: jobId={} recipients={}", jobId, recipientIds.size());
        return toJobResponse(job);
    }

    /**
     * Cancel a job.
     */
    @Transactional
    public NotificationJobResponse cancelJob(String tenantId, UUID jobId) {
        NotificationJob job = findJobByTenant(tenantId, jobId);
        job.cancel();
        job = jobRepository.save(job);

        log.info("Notification job cancelled: jobId={}", jobId);
        return toJobResponse(job);
    }

    /**
     * Get notifications for a job.
     */
    public List<io.cerp.notification.dto.NotificationResponse> getJobNotifications(String tenantId, UUID jobId) {
        NotificationJob job = findJobByTenant(tenantId, jobId);
        return job.getNotifications().stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    private NotificationJob findJobByTenant(String tenantId, UUID jobId) {
        return jobRepository.findById(jobId)
                .filter(job -> job.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
    }

    private List<UUID> resolveRecipients(String tenantId, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return identityServiceClient.resolveUsersByRoles(roles, tenantId);
    }

    public NotificationJobRepository getJobRepository() {
        return jobRepository;
    }

    private NotificationJobResponse toJobResponse(NotificationJob job) {
        return NotificationJobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .code(job.getCode())
                .title(job.getTitle())
                .description(job.getDescription())
                .actionType(job.getActionType())
                .payload(job.getPayload())
                .sourceService(job.getSourceService())
                .sourceEntityId(job.getSourceEntityId())
                .sourceEntityType(job.getSourceEntityType())
                .priority(job.getPriority())
                .status(job.getStatus())
                .recipientUserIds(job.getRecipientUserIds())
                .recipientRoles(job.getRecipientRoles())
                .dueAt(job.getDueAt())
                .reminderIntervalMinutes(job.getReminderIntervalMinutes())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .publishedAt(job.getPublishedAt() != null ? job.getPublishedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .completedAt(job.getCompletedAt() != null ? job.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .cancelledAt(job.getCancelledAt() != null ? job.getCancelledAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private io.cerp.notification.dto.NotificationResponse toNotificationResponse(Notification notification) {
        NotificationJob job = notification.getJob();
        return io.cerp.notification.dto.NotificationResponse.builder()
                .id(notification.getId())
                .code(job.getCode())
                .title(job.getTitle())
                .description(job.getDescription())
                .actionType(job.getActionType() != null ? job.getActionType().name() : null)
                .status(notification.getStatus() != null ? notification.getStatus().name() : null)
                .priority(job.getPriority() != null ? job.getPriority().name() : null)
                .jobId(job.getId())
                .recipientId(notification.getRecipientId())
                .recipientRole(notification.getRecipientRole())
                .actionResult(notification.getActionResult())
                .sourceEntityId(job.getSourceEntityId() != null ? job.getSourceEntityId().toString() : null)
                .sourceEntityType(job.getSourceEntityType())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .openedAt(notification.getReadAt())
                .acknowledgedAt(notification.getAcknowledgedAt())
                .completedAt(notification.getActionTakenAt())
                .dueAt(notification.getExpiresAt())
                .build();
    }
}