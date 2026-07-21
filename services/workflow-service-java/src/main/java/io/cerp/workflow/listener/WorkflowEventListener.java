package io.cerp.workflow.listener;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cerp.workflow.service.SubProjectWorkflowGenerator;

/**
 * Kafka event listener for subproject-related events.
 *
 * <p>
 * This listener processes subproject creation events and triggers automatic workflow generation based on the organisation's configuration.
 */
@Component
public class WorkflowEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventListener.class);

    /** Topic for subproject creation events published by project-service. */
    public static final String TOPIC_SUBPROJECT_CREATED = "project.subproject.created";

    private final SubProjectWorkflowGenerator subProjectWorkflowGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowEventListener(SubProjectWorkflowGenerator subProjectWorkflowGenerator) {
        this.subProjectWorkflowGenerator = subProjectWorkflowGenerator;
    }

    /**
     * Handle subproject created event via Kafka.
     *
     * <p>
     * When a new subproject is created, this method checks if auto-generation is enabled for the organisation and triggers workflow generation if so.
     *
     * @param eventJson
     *            the subproject created event JSON payload
     */
    @KafkaListener(topics = TOPIC_SUBPROJECT_CREATED, groupId = "workflow-service-group")
    public void handleSubprojectCreated(String eventJson) {
        try {
            SubprojectCreatedEvent event = objectMapper.readValue(eventJson, SubprojectCreatedEvent.class);
            processSubprojectCreatedEvent(event);
        } catch (Exception parseEx) {
            log.error("Failed to parse subproject created event: {}", eventJson, parseEx);
        }
    }

    private void processSubprojectCreatedEvent(SubprojectCreatedEvent event) {
        log.info("Received subproject created event: subprojectId={}, tenantId={}, orgId={}",
            event.subprojectId(), event.tenantId(), event.orgId());

        try {
            var response = subProjectWorkflowGenerator.generateWorkflowsOnSubprojectCreation(
                event.tenantId(),
                event.orgId(),
                event.subprojectId(),
                event.projectId(),
                event.buildingType(),
                event.constructionPhase());

            log.info("Workflow generation completed for subproject {}: {} generated, {} skipped",
                event.subprojectId(), response.getGeneratedCount(), response.getSkippedCount());
        } catch (Exception e) {
            log.error("Failed to generate workflows for subproject {}",
                event.subprojectId(), e);
            // Don't re-throw - we don't want to fail the subproject creation
            // if workflow generation fails
        }
    }

    /**
     * Subproject created event payload.
     */
    public record SubprojectCreatedEvent(
            String tenantId,
            UUID orgId,
            UUID subprojectId,
            UUID projectId,
            String buildingType,
            String constructionPhase) {
    }
}
