package io.cerp.project.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.cerp.project.service.CascadeArchivalException;

/**
 * Archives workflow-service's records for a Project being deleted: its Workflows (WorkflowSteps
 * are only ever shown nested under a Workflow, so hiding the parent is sufficient - see
 * WorkflowController's new /by-project/{projectId}/archive) and its Bidding/Procurement sagas
 * (both are rows in the same generic saga_events table, keyed by projectId, so one archive call
 * covers both - see SagaController's new /by-project/{projectId}/archive). Mirrors
 * IdentityUserClient's construction style (plain @Component, RestClient built inline from
 * @Value-injected config).
 */
@Component
public class WorkflowServiceClient {

    private final RestClient restClient;

    public WorkflowServiceClient(@Value("${cerp.workflow-service.url:http://localhost:8089}") String workflowServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(workflowServiceUrl).build();
    }

    /**
     * Archives every Workflow and every Bidding/Procurement saga tied to the given project, so
     * they drop out of default listings while the underlying rows - and their WorkflowSteps,
     * evidence, and approval history - remain intact for audit purposes. callerToken is the
     * acting user's own bearer token, forwarded as-is (these are authenticated, state-mutating
     * endpoints, same convention as IdentityUserClient.grantProjectMembership).
     */
    public void archiveByProjectId(String callerToken, UUID projectId) {
        archive(callerToken, projectId, "/api/v1/workflows/by-project/{projectId}/archive", "workflows");
        archive(callerToken, projectId, "/api/v1/sagas/by-project/{projectId}/archive", "bidding/procurement sagas");
    }

    private void archive(String callerToken, UUID projectId, String uriTemplate, String whatForErrorMessage) {
        try {
            restClient.post()
                .uri(uriTemplate, projectId)
                .header("Authorization", "Bearer " + callerToken)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            throw new CascadeArchivalException(
                "Could not archive " + whatForErrorMessage + " for project " + projectId + " via workflow-service", e);
        }
    }
}
