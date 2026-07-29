package io.cerp.workflow.service;

import io.cerp.workflow.event.WorkflowEvent;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for publishing workflow events to the transactional outbox.
 * 
 * <p>
 * Events published through this interface are persisted to the outbox table and later published to the message broker (Kafka) by the outbox processor. This ensures
 * exactly-once event delivery semantics.
 * 
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 */
public interface EventPublisher {

    /**
     * Publish a workflow event.
     *
     * @param event
     *            the workflow event to publish
     */
    void publish(WorkflowEvent event);

    /**
     * Publish a workflow event with additional metadata.
     *
     * @param event
     *            the workflow event to publish
     * @param metadata
     *            additional metadata to include with the event
     */
    void publish(WorkflowEvent event, Map<String, Object> metadata);

    /**
     * Publish a workflow event for a specific aggregate.
     *
     * @param eventType
     *            the event type (e.g., "ProcurementRequestCreated")
     * @param sagaType
     *            the saga/workflow type
     * @param aggregateId
     *            the aggregate ID that caused the event
     * @param tenantId
     *            the tenant ID
     * @param data
     *            the event payload
     */
    void publishEvent(String eventType, String sagaType, UUID aggregateId,
            String tenantId, Map<String, Object> data);

    /**
     * Check if the event publisher is healthy.
     *
     * @return true if healthy
     */
    boolean isHealthy();
}
