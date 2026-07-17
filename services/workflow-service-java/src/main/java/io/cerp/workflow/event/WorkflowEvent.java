package io.cerp.workflow.event;

import java.time.LocalDateTime;

/**
 * Base envelope for all Workflow domain events.
 *
 * <p>
 * Per Section 9 (Data and Event Design): Events are immutable facts with IDs, timestamps, tenant, correlation/causation IDs, and version.
 */
public abstract class WorkflowEvent {

    private String eventType;
    private String version;
    private LocalDateTime occurredAt;
    private String tenantId;
    private String aggregateType;
    private String aggregateId;
    private String aggregateVersion;
    private String correlationId;
    private String causationId;
    private String actor;

    protected WorkflowEvent() {
    }

    protected WorkflowEvent(String eventType, String templateId) {
        this.eventType = eventType;
        this.version = "1.0";
        this.occurredAt = LocalDateTime.now();
        this.tenantId = io.cerp.workflow.config.TenantContext.getCurrentTenantId();
        this.aggregateType = "WorkflowTemplate";
        this.aggregateId = templateId;
        this.aggregateVersion = "1";
        this.correlationId = java.util.UUID.randomUUID().toString();
        this.causationId = java.util.UUID.randomUUID().toString();
    }

    protected WorkflowEvent(String eventType, String tenantId, String aggregateType, String aggregateId) {
        this.eventType = eventType;
        this.version = "1.0";
        this.occurredAt = LocalDateTime.now();
        this.tenantId = tenantId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.aggregateVersion = "1";
        this.correlationId = java.util.UUID.randomUUID().toString();
        this.causationId = java.util.UUID.randomUUID().toString();
    }

    public String getEventType() {
        return eventType;
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateVersion() {
        return aggregateVersion;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}
