package io.cerp.project.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration for Project Service events.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String PROJECT_TOPIC_PREFIX = "project.";

    @Bean
    public NewTopic projectCreated() {
        return TopicBuilder.name(PROJECT_TOPIC_PREFIX + "created")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic projectUpdated() {
        return TopicBuilder.name(PROJECT_TOPIC_PREFIX + "updated")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic projectDeleted() {
        return TopicBuilder.name(PROJECT_TOPIC_PREFIX + "deleted")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
