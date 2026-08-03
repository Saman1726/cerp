package io.cerp.notification.domain;

/**
 * Status of an individual notification instance.
 */
public enum NotificationStatus {
    /**
     * Notification has been created but not yet seen by the recipient.
     */
    PENDING,

    /**
     * Notification has been opened/read by the recipient.
     */
    READ,

    /**
     * Recipient has taken the required action.
     */
    ACTION_TAKEN,

    /**
     * Notification has expired without action.
     */
    EXPIRED,

    /**
     * Notification has been archived.
     */
    ARCHIVED
}