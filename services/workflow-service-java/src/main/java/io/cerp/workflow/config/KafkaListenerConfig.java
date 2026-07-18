package io.cerp.workflow.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka listener configuration for the workflow service.
 *
 * <p>
 * Configures the Kafka listener container factory with settings that prevent blocking of the Spring Boot application startup. The key setting is
 * {@code autoStartup=false}, which allows the application to start even if Kafka is temporarily unavailable.
 */
@Configuration
public class KafkaListenerConfig {

    /**
     * Configure the Kafka consumer factory with proper settings.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            System.getenv("KAFKA_BOOTSTRAP_SERVERS") != null
                    ? System.getenv("KAFKA_BOOTSTRAP_SERVERS")
                    : "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "workflow-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Configure the Kafka listener container factory with non-blocking settings.
     *
     * <p>
     * The key configuration here:
     * <ul>
     * <li>setAutoStartup(false) - allows app to start even if Kafka is unavailable</li>
     * </ul>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Allow the application to start even if Kafka is not available
        factory.setAutoStartup(false);

        return factory;
    }
}
