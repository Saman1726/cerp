package io.cerp.workflow.config;

import org.springframework.context.annotation.Configuration;

/**
 * Kafka topic configuration for Workflow Service.
 *
 * <p>
 * Topics follow the naming convention: <context>.<aggregate>.v1
 */
@Configuration
public class KafkaTopicConfig {

    // ── Topic names ──

    /** Workflow template lifecycle events */
    public static final String TOPIC_TEMPLATE_CREATED = "collaboration.workflow.template.v1";
    public static final String TOPIC_TEMPLATE_UPDATED = "collaboration.workflow.template.updated.v1";
    public static final String TOPIC_TEMPLATE_ACTIVATED = "collaboration.workflow.template.activated.v1";
    public static final String TOPIC_TEMPLATE_DEPRECATED = "collaboration.workflow.template.deprecated.v1";
    public static final String TOPIC_TEMPLATE_CLONED = "collaboration.workflow.template.cloned.v1";

    /** Workflow instance lifecycle events */
    public static final String TOPIC_WORKFLOW_INSTANTIATED = "collaboration.workflow.instantiated.v1";
    public static final String TOPIC_WORKFLOW_STARTED = "collaboration.workflow.started.v1";
    public static final String TOPIC_WORKFLOW_BLOCKED = "collaboration.workflow.blocked.v1";
    public static final String TOPIC_WORKFLOW_UNBLOCKED = "collaboration.workflow.unblocked.v1";
    public static final String TOPIC_WORKFLOW_CANCELED = "collaboration.workflow.canceled.v1";
    public static final String TOPIC_WORKFLOW_COMPLETED = "collaboration.workflow.completed.v1";
    public static final String TOPIC_WORKFLOW_APPROVED = "collaboration.workflow.approved.v1";
    public static final String TOPIC_WORKFLOW_REJECTED = "collaboration.workflow.rejected.v1";
    public static final String TOPIC_WORKFLOW_ONHOLD = "collaboration.workflow.onhold.v1";
    public static final String TOPIC_WORKFLOW_UNHOLD = "collaboration.workflow.unhold.v1";

    /** Workflow step events */
    public static final String TOPIC_STEP_STARTED = "collaboration.workflow.step.started.v1";
    public static final String TOPIC_STEP_COMPLETED = "collaboration.workflow.step.completed.v1";
    public static final String TOPIC_STEP_APPROVED = "collaboration.workflow.step.approved.v1";
    public static final String TOPIC_STEP_REJECTED = "collaboration.workflow.step.rejected.v1";
    public static final String TOPIC_STEP_BLOCKED = "collaboration.workflow.step.blocked.v1";

    /** Workflow metadata and progress events */
    public static final String TOPIC_PROGRESS_UPDATED = "collaboration.workflow.progress.updated.v1";
    public static final String TOPIC_STATUS_CHANGED = "collaboration.workflow.status.changed.v1";
    public static final String TOPIC_NOTE_ADDED = "collaboration.workflow.note.added.v1";
    public static final String TOPIC_SLA_BREACHED = "collaboration.workflow.sla.breached.v1";

    /** Workflow template step schema */
    public static final String TOPIC_STEP_TEMPLATE_ADDED = "collaboration.workflow.step.template.added.v1";
    public static final String TOPIC_STEP_TEMPLATE_UPDATED = "collaboration.workflow.step.template.updated.v1";
    public static final String TOPIC_STEP_TEMPLATE_REMOVED = "collaboration.workflow.step.template.removed.v1";
}
