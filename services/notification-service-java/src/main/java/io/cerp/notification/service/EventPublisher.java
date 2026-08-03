package io.cerp.notification.service;

import io.cerp.notification.event.NotificationJobCreated;
import io.cerp.notification.event.NotificationJobPublished;
import io.cerp.notification.event.NotificationOpened;
import io.cerp.notification.event.NotificationActionTaken;

/**
 * Interface for publishing notification events.
 */
public interface EventPublisher {

    void publishJobCreated(NotificationJobCreated event);

    void publishNotificationJobPublished(NotificationJobPublished event);

    void publishNotificationOpened(NotificationOpened event);

    void publishNotificationActionTaken(NotificationActionTaken event);
}