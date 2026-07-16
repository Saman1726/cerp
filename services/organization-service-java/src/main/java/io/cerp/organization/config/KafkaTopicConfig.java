package io.cerp.organization.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration for Organization Service events.
 *
 * <p>
 * Topics created per Section 10 (Event Sourcing / CQRS): - organization.created - organization.updated - organization.deleted
 */
@Configuration
public class KafkaTopicConfig {

    public static final String ORGANIZATION_TOPIC_PREFIX = "organization.";

    @Bean
    public NewTopic organizationCreated() {
        return TopicBuilder.name(ORGANIZATION_TOPIC_PREFIX + "created")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic organizationUpdated() {
        return TopicBuilder.name(ORGANIZATION_TOPIC_PREFIX + "updated")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic organizationDeleted() {
        return TopicBuilder.name(ORGANIZATION_TOPIC_PREFIX + "deleted")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
