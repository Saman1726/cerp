package io.cerp.workflow.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an event in the transactional outbox.
 *
 * <p>
 * Events persisted here are later published to the message broker (Kafka) by the outbox processor, ensuring exactly-once delivery semantics through the transactional
 * outbox pattern.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 */
@Entity
@Table(name = "outbox_events", schema = "workflow")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String sagaType;

    @Column(nullable = false)
    private String sagaId;

    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "version")
    private int version;

    protected OutboxEvent() {
    }

    public OutboxEvent(String eventType, String sagaType, String sagaId, String tenantId,
            String eventData, String metadata) {
        this.eventType = eventType;
        this.sagaType = sagaType;
        this.sagaId = sagaId;
        this.tenantId = tenantId;
        this.eventData = eventData;
        this.metadata = metadata;
        this.status = OutboxStatus.PENDING;
        this.createdAt = Instant.now();
        this.retryCount = 0;
        this.version = 1;
    }

    public UUID getId() {
        return id;
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

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventData() {
        return eventData;
    }

    public String getMetadata() {
        return metadata;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Status of an outbox event.
     */
    public enum OutboxStatus {
        /** Event is pending publication */
        PENDING,
        /** Event has been published successfully */
        PUBLISHED,
        /** Event publishing failed after retries */
        FAILED
    }
}
