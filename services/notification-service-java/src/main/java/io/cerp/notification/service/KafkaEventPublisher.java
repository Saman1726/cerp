package io.cerp.notification.service;

import io.cerp.notification.config.KafkaTopicConfig;
import io.cerp.notification.event.NotificationActionTaken;
import io.cerp.notification.event.NotificationJobCreated;
import io.cerp.notification.event.NotificationJobPublished;
import io.cerp.notification.event.NotificationOpened;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka-based implementation of the event publisher.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishJobCreated(NotificationJobCreated event) {
        log.info("Publishing notification job created event: jobId={}", event.getJobId());
        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATION_JOB_CREATED_TOPIC,
                event.getJobId().toString(), event);
    }

    @Override
    public void publishNotificationJobPublished(NotificationJobPublished event) {
        log.info("Publishing notification job published event: jobId={}", event.getJobId());
        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATION_JOB_PUBLISHED_TOPIC,
                event.getJobId().toString(), event);
    }

    @Override
    public void publishNotificationOpened(NotificationOpened event) {
        log.info("Publishing notification opened event: notificationId={}", event.getNotificationId());
        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATION_OPENED_TOPIC,
                event.getNotificationId().toString(), event);
    }

    @Override
    public void publishNotificationActionTaken(NotificationActionTaken event) {
        log.info("Publishing notification action taken event: notificationId={}", event.getNotificationId());
        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATION_ACTION_TAKEN_TOPIC,
                event.getNotificationId().toString(), event);
    }
}