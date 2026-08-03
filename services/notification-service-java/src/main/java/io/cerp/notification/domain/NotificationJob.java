package io.cerp.notification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a notification job that defines what action needs to be taken
 * and who needs to take it. One job creates multiple notification instances (one per recipient).
 */
@Entity
@Table(name = "notification_jobs", schema = "notification",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationJob {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private NotificationActionType actionType;

    @Column(name = "payload", columnDefinition = "JSONB")
    @Convert(converter = JsonNodeStringConverter.class)
    private Object payload;

    @Column(name = "source_service")
    private String sourceService;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Column(name = "source_entity_type")
    private String sourceEntityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationJobStatus status;

    @Column(name = "recipient_user_ids", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> recipientUserIds = new ArrayList<>();

    @Column(name = "recipient_roles", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> recipientRoles = new ArrayList<>();

    @Column(name = "due_at")
    private String dueAt;

    @Column(name = "reminder_interval_minutes")
    private Integer reminderIntervalMinutes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Transition job status to PUBLISHED and populate recipients.
     */
    public void publish() {
        if (this.status != NotificationJobStatus.DRAFT && this.status != NotificationJobStatus.PENDING) {
            throw new IllegalStateException("Only DRAFT or PENDING jobs can be published");
        }
        this.status = NotificationJobStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * Mark job as completed (all recipients have acted).
     */
    public void complete() {
        if (this.status == NotificationJobStatus.COMPLETED || this.status == NotificationJobStatus.CANCELLED) {
            return;
        }
        this.status = NotificationJobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Cancel the job.
     */
    public void cancel() {
        if (this.status == NotificationJobStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed job");
        }
        this.status = NotificationJobStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * Check if all notifications have been acted upon.
     */
    public boolean isPartiallyCompleted() {
        if (this.notifications.isEmpty()) {
            return false;
        }
        long actionTakenCount = this.notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.ACTION_TAKEN)
                .count();
        return actionTakenCount > 0 && actionTakenCount < this.notifications.size();
    }

    public static NotificationJob create(UUID id, String tenantId, String code, String title,
                                          String description, NotificationActionType actionType,
                                          Object payload, String sourceService, UUID sourceEntityId,
                                          String sourceEntityType, NotificationPriority priority,
                                          UUID createdBy) {
        NotificationJob job = new NotificationJob();
        job.setId(id);
        job.setTenantId(tenantId);
        job.setCode(code);
        job.setTitle(title);
        job.setDescription(description);
        job.setActionType(actionType);
        job.setPayload(payload);
        job.setSourceService(sourceService);
        job.setSourceEntityId(sourceEntityId);
        job.setSourceEntityType(sourceEntityType);
        job.setPriority(priority != null ? priority : NotificationPriority.MEDIUM);
        job.setStatus(NotificationJobStatus.DRAFT);
        job.setCreatedBy(createdBy);
        return job;
    }
}