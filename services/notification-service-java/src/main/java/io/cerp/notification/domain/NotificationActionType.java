package io.cerp.notification.domain;

/**
 * Types of actions that can be required on a notification.
 */
public enum NotificationActionType {
    /**
     * User needs to review the information and take no formal action.
     */
    REVIEW,

    /**
     * User needs to explicitly approve the request.
     */
    APPROVE,

    /**
     * User needs to explicitly reject the request.
     */
    REJECT,

    /**
     * User needs to acknowledge receipt of the notification.
     */
    ACKNOWLEDGE,

    /**
     * User needs to execute a specific action or workflow step.
     */
    EXECUTE,

    /**
     * User needs to provide input or feedback.
     */
    INPUT
}