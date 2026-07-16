package io.cerp.catalog.events;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes catalog domain events to Kafka as simple envelopes: { type, timestamp, payload }
 */
@Component
public class CatalogEventsPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public CatalogEventsPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.topics.catalog}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(String type, Object payload) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", type);
        envelope.put("timestamp", OffsetDateTime.now().toString());
        envelope.put("payload", payload);
        // Use type as key for ordering by event type; could also use item id depending on partitioning strategy.
        kafkaTemplate.send(topic, type, envelope);
    }
}
