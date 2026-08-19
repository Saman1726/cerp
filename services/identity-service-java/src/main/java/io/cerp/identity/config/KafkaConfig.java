package io.cerp.identity.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.kafka")
    public KafkaProps kafkaProps() {
        return new KafkaProps();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
    public ProducerFactory<String, String> producerFactory(KafkaProps props) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }

    // =====================================================================
    // Consumer configuration for @KafkaListener (JSON deserialization)
    // =====================================================================
    @Bean
    @ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
    public ConsumerFactory<String, Object> identityConsumerFactory(KafkaProps props) {
        Map<String, Object> propsMap = new HashMap<>();
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, "identity-service");
        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        propsMap.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        propsMap.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        // Trusted packages: allow deserialization of both vendor and identity event types
        propsMap.put(JsonDeserializer.TRUSTED_PACKAGES,
            "io.cerp.vendor.dto,io.cerp.identity.events.VendorRegistrationConsumer");
        propsMap.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
            "io.cerp.identity.events.VendorRegistrationConsumer$VendorRegistrationEvent");
        propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Heartbeat and session timeout settings
        propsMap.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
        propsMap.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        propsMap.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return new DefaultKafkaConsumerFactory<>(propsMap);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> identityConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(identityConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0)));
        return factory;
    }

    public static class KafkaProps {
        /**
         * Comma separated bootstrap servers.
         */
        private String bootstrapServers = "localhost:9092";

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }
    }
}
