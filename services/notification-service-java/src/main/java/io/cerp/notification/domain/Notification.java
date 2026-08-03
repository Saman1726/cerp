package io.cerp.notification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a notification delivered to a specific user.
 * One NotificationJob creates multiple Notification instances (one per recipient).
 */
@Entity
@Table(name = "notifications", schema = "notification",
       indexes = {
           @Index(name = "idx_notifications_job_id", columnList = "job_id"),
           @Index(name = "idx_notifications_recipient", columnList = "recipient_id"),
           @Index(name = "idx_notifications_status", columnList = "status"),
           @Index(name = "idx_notifications_expires", columnList = "expires_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private NotificationJob job;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "recipient_role")
    private String recipientRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private NotificationPriority priority;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "action_taken_at")
    private LocalDateTime actionTakenAt;

    @Column(name = "action_taken_by")
    private UUID actionTakenBy;

    @Column(name = "action_result", columnDefinition = "JSONB")
    @Convert(converter = JsonNodeStringConverter.class)
    private Object actionResult;

    @Column(name = "expires_at")
    private String expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark notification as acknowledged/read.
     */
    public void acknowledge() {
        if (this.status == NotificationStatus.ACTION_TAKEN) {
            throw new IllegalStateException("Cannot acknowledge a notification that already has action taken");
        }
        if (this.status == NotificationStatus.EXPIRED) {
            throw new IllegalStateException("Cannot acknowledge an expired notification");
        }
        if (this.acknowledgedAt == null) {
            this.acknowledgedAt = LocalDateTime.now();
        }
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
        this.status = NotificationStatus.READ;
    }

    /**
     * Take action on this notification.
     */
    public void takeAction(UUID userId, Object result) {
        if (this.status == NotificationStatus.ACTION_TAKEN) {
            throw new IllegalStateException("Action already taken on this notification");
        }
        if (this.status == NotificationStatus.EXPIRED) {
            throw new IllegalStateException("Cannot act on an expired notification");
        }
        this.status = NotificationStatus.ACTION_TAKEN;
        this.actionTakenAt = LocalDateTime.now();
        this.actionTakenBy = userId;
        this.actionResult = result;
    }

    /**
     * Mark notification as expired.
     */
    public void expire() {
        if (this.status == NotificationStatus.ACTION_TAKEN || this.status == NotificationStatus.ARCHIVED) {
            return;
        }
        this.status = NotificationStatus.EXPIRED;
    }

    /**
     * Archive the notification.
     */
    public void archive() {
        this.status = NotificationStatus.ARCHIVED;
    }

    /**
     * Check if this notification has expired.
     */
    public boolean isExpired() {
        if (this.expiresAt == null) {
            return false;
        }
        try {
            LocalDateTime expiry = LocalDateTime.parse(this.expiresAt);
            return LocalDateTime.now().isAfter(expiry);
        } catch (Exception e) {
            return false;
        }
    }

    public static Notification create(UUID id, NotificationJob job, UUID recipientId, String recipientRole) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setJob(job);
        notification.setTenantId(job.getTenantId());
        notification.setRecipientId(recipientId);
        notification.setRecipientRole(recipientRole);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setPriority(job.getPriority());
        notification.setExpiresAt(job.getDueAt());
        return notification;
    }
}