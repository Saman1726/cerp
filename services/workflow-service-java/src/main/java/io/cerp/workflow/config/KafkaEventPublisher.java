package io.cerp.workflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import io.cerp.workflow.event.WorkflowEvent;

/**
 * Publishes domain events to Kafka topics.
 *
 * <p>
 * Topic naming convention: {@code collaboration.workflow.v1}.
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private static final String TOPIC = "collaboration.workflow.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish a single domain event.
     *
     * @param event
     *            the domain event to publish
     */
    public void publish(WorkflowEvent event) {
        String key = event.getAggregateId();
        kafkaTemplate.send(TOPIC, key, event).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published event {} to topic {} at partition {} offset {}",
                    event.getEventType(), TOPIC,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event {} to topic {}: {}",
                    event.getEventType(), TOPIC, ex.getMessage(), ex);
            }
        });
    }

    /**
     * Publish multiple events (batch).
     *
     * @param events
     *            list of domain events
     */
    public void publishAll(java.util.List<WorkflowEvent> events) {
        for (WorkflowEvent event : events) {
            publish(event);
        }
    }
}
