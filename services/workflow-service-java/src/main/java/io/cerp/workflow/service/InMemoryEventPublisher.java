package io.cerp.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.outbox.OutboxEvent;
import io.cerp.workflow.outbox.OutboxRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-memory implementation of the EventPublisher that uses the transactional outbox pattern.
 *
 * <p>
 * Events are persisted to the outbox table within the same transaction as the business operation. A separate outbox processor (running as a scheduled task) reads pending
 * events from the outbox and publishes them to the message broker.
 *
 * <p>
 * This ensures that events are only published if the business transaction commits, solving the distributed transaction problem.
 *
 * @see EventPublisher
 * @see OutboxRepository
 */
@Service
public class InMemoryEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // In-memory event store for in-process access (not persisted separately)
    private final Map<UUID, WorkflowEvent> eventStore = new ConcurrentHashMap<>();

    public InMemoryEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(WorkflowEvent event) {
        persistToOutbox(event);
    }

    @Override
    @Transactional
    public void publish(WorkflowEvent event, Map<String, Object> metadata) {
        persistToOutbox(event, metadata);
    }

    @Override
    @Transactional
    public void publishEvent(String eventType, String sagaType, UUID aggregateId,
            String tenantId, Map<String, Object> data) {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType(eventType)
            .sagaType(sagaType)
            .sagaId(aggregateId != null ? aggregateId.toString() : null)
            .aggregateId(aggregateId)
            .aggregateType(sagaType)
            .tenantId(tenantId)
            .data(data)
            .build();
        persistToOutbox(event);
    }

    @Override
    public boolean isHealthy() {
        try {
            outboxRepository.countPendingEvents();
            return true;
        } catch (Exception e) {
            log.error("EventPublisher health check failed: {}", e.getMessage());
            return false;
        }
    }

    // --- Private methods ---

    private void persistToOutbox(WorkflowEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event.getData());
            String metadata = objectMapper.writeValueAsString(event.getMetadata());

            OutboxEvent outboxEvent = new OutboxEvent(
                event.getEventType(),
                event.getSagaType(),
                event.getSagaId(),
                event.getTenantId(),
                eventData,
                metadata);
            outboxEvent.setAggregateId(event.getAggregateId());
            outboxEvent.setAggregateType(event.getAggregateType());
            outboxEvent.setUserId(event.getUserId());

            outboxRepository.save(outboxEvent);
            eventStore.put(event.getEventId(), event);

            log.info("Outbox event persisted: type={}, sagaType={}, sagaId={}, aggregateType={}, aggregateId={}",
                event.getEventType(), event.getSagaType(), event.getSagaId(),
                event.getAggregateType(), event.getAggregateId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private void persistToOutbox(WorkflowEvent event, Map<String, Object> metadata) {
        try {
            String eventData = objectMapper.writeValueAsString(event.getData());
            String metadataJson = objectMapper.writeValueAsString(metadata);

            OutboxEvent outboxEvent = new OutboxEvent(
                event.getEventType(),
                event.getSagaType(),
                event.getSagaId(),
                event.getTenantId(),
                eventData,
                metadataJson);
            outboxEvent.setAggregateId(event.getAggregateId());
            outboxEvent.setAggregateType(event.getAggregateType());
            outboxEvent.setUserId(event.getUserId());

            outboxRepository.save(outboxEvent);
            eventStore.put(event.getEventId(), event);

            log.info("Outbox event persisted with metadata: type={}, sagaType={}, sagaId={}",
                event.getEventType(), event.getSagaType(), event.getSagaId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event with metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event with metadata", e);
        }
    }

    /**
     * Get an event from the in-memory store (for testing).
     */
    public WorkflowEvent getEvent(UUID eventId) {
        return eventStore.get(eventId);
    }

    /**
     * Get all events from the in-memory store (for testing).
     */
    public java.util.List<WorkflowEvent> getAllEvents() {
        return java.util.List.copyOf(eventStore.values());
    }

    /**
     * Clear the in-memory event store (for testing).
     */
    public void clearStore() {
        eventStore.clear();
    }
}
