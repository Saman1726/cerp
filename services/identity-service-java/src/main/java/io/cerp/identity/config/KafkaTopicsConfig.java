package io.cerp.identity.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares identity.* topics for local/dev environments. Spring Kafka's KafkaAdmin will auto-create these when beans are present.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
public class KafkaTopicsConfig {

    // Phase 1 identity topics
    public static final String TENANT_TOPIC = "identity.tenant.v1";
    public static final String USER_TOPIC = "identity.user.v1";
    public static final String MEMBERSHIP_TOPIC = "identity.membership.v1";

    // Use modest partition count for local dev; RF=1 because single-broker in compose
    private static final int PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;

    @Bean
    public NewTopic identityTenantTopic() {
        return new NewTopic(TENANT_TOPIC, PARTITIONS, REPLICATION_FACTOR);
    }

    @Bean
    public NewTopic identityUserTopic() {
        return new NewTopic(USER_TOPIC, PARTITIONS, REPLICATION_FACTOR);
    }

    @Bean
    public NewTopic identityMembershipTopic() {
        return new NewTopic(MEMBERSHIP_TOPIC, PARTITIONS, REPLICATION_FACTOR);
    }
}
