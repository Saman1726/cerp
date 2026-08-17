package io.cerp.workflow.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.dto.WorkflowGenerationRequest;
import io.cerp.workflow.dto.WorkflowGenerationResponse;
import io.cerp.workflow.service.SubProjectWorkflowGenerator;
import io.cerp.workflow.service.WorkflowService;

/**
 * REST controller for workflow generation operations.
 */
@RestController
@RequestMapping("/api/v1")
public class WorkflowGenerationController {

    private final SubProjectWorkflowGenerator subProjectWorkflowGenerator;
    private final WorkflowService workflowService;

    public WorkflowGenerationController(SubProjectWorkflowGenerator subProjectWorkflowGenerator,
            WorkflowService workflowService) {
        this.subProjectWorkflowGenerator = subProjectWorkflowGenerator;
        this.workflowService = workflowService;
    }

    /**
     * Generate workflows for a subproject.
     *
     * <p>
     * This endpoint creates workflow instances for a subproject based on template-phase mappings and organisation configuration. It is idempotent - calling it multiple
     * times will not create duplicate workflows.
     *
     * <p>
     * Request body should contain:
     * <ul>
     * <li>subprojectId - the subproject to generate workflows for</li>
     * <li>tenantId - the tenant ID</li>
     * <li>orgId - the organisation ID</li>
     * <li>projectId - the parent project ID</li>
     * <li>buildingType - the building type (e.g., RESIDENTIAL, COMMERCIAL)</li>
     * <li>phaseTypes - optional list of phase types (if null, all matching are used)</li>
     * </ul>
     *
     * @param tenantId
     *            the tenant identifier
     * @param request
     *            the generation request
     * @return response with generated and skipped workflow details
     */
    @PostMapping("/{tenantId}/workflows/generate")
    public ResponseEntity<WorkflowGenerationResponse> generateWorkflows(
            @PathVariable String tenantId,
            @RequestBody WorkflowGenerationRequest request) {
        WorkflowGenerationResponse response = subProjectWorkflowGenerator.generateWorkflows(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get existing workflow IDs for a subproject (idempotency check).
     */
    @GetMapping("/{tenantId}/subprojects/{subprojectId}/workflows")
    public ResponseEntity<List<UUID>> getExistingWorkflows(
            @PathVariable String tenantId,
            @PathVariable UUID subprojectId) {
        List<UUID> workflowIds = subProjectWorkflowGenerator.getExistingWorkflowIds(tenantId, subprojectId);
        return ResponseEntity.ok(workflowIds);
    }

    /**
     * Check if workflows already exist for a subproject.
     */
    @GetMapping("/{tenantId}/subprojects/{subprojectId}/workflows/existing")
    public ResponseEntity<Boolean> workflowsExist(
            @PathVariable String tenantId,
            @PathVariable UUID subprojectId) {
        boolean exists = subProjectWorkflowGenerator.workflowsExistForSubproject(tenantId, subprojectId);
        return ResponseEntity.ok(exists);
    }

    /**
     * Get all workflows for a subproject with full details.
     */
    @GetMapping("/{tenantId}/subprojects/{subprojectId}/workflows/details")
    public ResponseEntity<List<Workflow>> getSubprojectWorkflows(
            @PathVariable String tenantId,
            @PathVariable UUID subprojectId) {
        List<Workflow> workflows = workflowService.getWorkflowsBySubprojectId(tenantId, subprojectId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Trigger workflow generation via event (Kafka-style synchronous equivalent).
     *
     * <p>
     * This endpoint is designed to be called from the project-service when a new subproject is created. It respects the organisation's auto-generation setting.
     * </p>
     *
     * @param tenantId
     *            the tenant ID
     * @param orgId
     *            the organisation ID
     * @param subprojectId
     *            the subproject ID
     * @param projectId
     *            the project ID
     * @param buildingType
     *            the building type
     * @param constructionPhase
     *            the construction Phase
     * @param authHeader
     *            the Authorization Bearer JWT header from which createdBy is extracted
     * @return response with generated workflow details
     */
    @PostMapping("/{tenantId}/workflows/auto-generate")
    public ResponseEntity<WorkflowGenerationResponse> autoGenerateWorkflows(
            @PathVariable String tenantId,
            @RequestParam UUID orgId,
            @RequestParam UUID subprojectId,
            @RequestParam UUID projectId,
            @RequestParam String buildingType,
            @RequestParam String constructionPhase,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String createdBy = extractUserIdFromHeader(authHeader);
        WorkflowGenerationResponse response = subProjectWorkflowGenerator
            .generateWorkflowsOnSubprojectCreation(tenantId, orgId, subprojectId, projectId, buildingType, constructionPhase, createdBy);

        if (response.getGeneratedCount() > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Extract userId from the Authorization Bearer JWT token payload.
     * Checks claims in priority order: internal_user_id (Keycloak custom), sub, userId.
     */
    private String extractUserIdFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.JsonNode root = new ObjectMapper().readTree(payload);
                com.fasterxml.jackson.databind.JsonNode internalUserId = root.get("internal_user_id");
                if (internalUserId != null && internalUserId.isTextual()) return internalUserId.asText();
                com.fasterxml.jackson.databind.JsonNode sub = root.get("sub");
                if (sub != null && sub.isTextual()) return sub.asText();
                com.fasterxml.jackson.databind.JsonNode userId = root.get("userId");
                if (userId != null && userId.isTextual()) return userId.asText();
            }
        } catch (Exception e) {
            // JWT decode failed — fall through to null
        }
        return null;
    }
}
