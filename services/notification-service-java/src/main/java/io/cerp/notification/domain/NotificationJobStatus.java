package io.cerp.notification.domain;

/**
 * Status of a notification job.
 */
public enum NotificationJobStatus {
    /**
     * Job is created but not yet published to recipients.
     */
    DRAFT,

    /**
     * Job is pending publication (e.g., awaiting approval).
     */
    PENDING,

    /**
     * Job is published and notifications have been created for recipients.
     */
    PUBLISHED,

    /**
     * Some recipients have taken action, but not all.
     */
    PARTIALLY_COMPLETED,

    /**
     * All recipients have taken action.
     */
    COMPLETED,

    /**
     * Job has expired without all actions being taken.
     */
    EXPIRED,

    /**
     * Job has been cancelled.
     */
    CANCELLED
}