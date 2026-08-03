package io.cerp.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Kafka topic names used by the notification service.
 */
@Configuration
public class KafkaTopicConfig {

    // Inbound topic - other services publish to this to create notification jobs
    public static final String INBOUND_JOB_CREATE_TOPIC = "notification-job-create-request";

    // Outbound topics - notification service publishes to these
    public static final String NOTIFICATION_JOB_CREATED_TOPIC = "notification-job-created";
    public static final String NOTIFICATION_JOB_PUBLISHED_TOPIC = "notification-job-published";
    public static final String NOTIFICATION_OPENED_TOPIC = "notification-opened";
    public static final String NOTIFICATION_ACTION_TAKEN_TOPIC = "notification-action-taken";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
}