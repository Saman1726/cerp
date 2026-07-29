package io.cerp.workflow.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable event emitted by workflow state transitions or saga steps.
 *
 * <p>
 * Workflow events are published to the transactional outbox and subsequently to the message broker (Kafka). They form the backbone of event choreography between bounded
 * contexts.
 *
 * <p>
 * Event naming follows the convention: {Aggregate}{Action}PastTense (e.g., ProcurementRequestCreated, ApprovalComplete, DeliveryConfirmed)
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 */
public class WorkflowEvent {

    private final UUID eventId;
    private final String eventType;
    private final String sagaType;
    private final String sagaId;
    private final UUID aggregateId;
    private final String aggregateType;
    private final String tenantId;
    private final String userId;
    private final Map<String, Object> data;
    private final Map<String, String> metadata;
    private final Instant occurredAt;
    private final int version;

    protected WorkflowEvent(Builder builder) {
        this.eventId = builder.eventId != null ? builder.eventId : UUID.randomUUID();
        this.eventType = builder.eventType;
        this.sagaType = builder.sagaType;
        this.sagaId = builder.sagaId;
        this.aggregateId = builder.aggregateId;
        this.aggregateType = builder.aggregateType;
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
        this.data = builder.data != null ? Map.copyOf(builder.data) : Map.of();
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
        this.occurredAt = builder.occurredAt != null ? builder.occurredAt : Instant.now();
        this.version = builder.version;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSagaType() {
        return sagaType;
    }

    public String getSagaId() {
        return sagaId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Create a new builder for WorkflowEvent.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for WorkflowEvent.
     */
    public static class Builder {
        private UUID eventId;
        private String eventType;
        private String sagaType;
        private String sagaId;
        private UUID aggregateId;
        private String aggregateType;
        private String tenantId;
        private String userId;
        private Map<String, Object> data;
        private Map<String, String> metadata;
        private Instant occurredAt;
        private int version = 1;

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder sagaType(String sagaType) {
            this.sagaType = sagaType;
            return this;
        }

        public Builder sagaId(String sagaId) {
            this.sagaId = sagaId;
            return this;
        }

        public Builder aggregateId(UUID aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public WorkflowEvent build() {
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalStateException("eventType is required");
            }
            return new WorkflowEvent(this);
        }
    }
}
