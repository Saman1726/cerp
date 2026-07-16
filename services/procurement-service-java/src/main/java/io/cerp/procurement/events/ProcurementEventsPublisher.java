package io.cerp.procurement.events;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes procurement domain events to Kafka as simple envelopes: { type, timestamp, payload }
 */
@Component
public class ProcurementEventsPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String requestTopic;

    public ProcurementEventsPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.topics.request}") String requestTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.requestTopic = requestTopic;
    }

    public void publishRequestEvent(String type, Object payload) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", type);
        envelope.put("timestamp", OffsetDateTime.now().toString());
        envelope.put("payload", payload);
        // Use type as key for ordering by event type; could also use request id depending on partitioning strategy.
        kafkaTemplate.send(requestTopic, type, envelope);
    }
}
