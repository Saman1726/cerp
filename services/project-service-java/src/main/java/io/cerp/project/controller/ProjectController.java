package io.cerp.project.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.project.config.RequestIdentity;
import io.cerp.project.dto.AssignedUserResponse;
import io.cerp.project.dto.ProjectCreateRequest;
import io.cerp.project.dto.ProjectResponse;
import io.cerp.project.dto.ProjectUpdateRequest;
import io.cerp.project.dto.ProjectUserAssignmentRequest;
import io.cerp.project.service.ProjectService;

/**
 * REST controller for project CRUD operations.
 *
 * <p>
 * Tenant context is extracted from the X-Tenant-Id header (injected by the API Gateway). User identity is extracted from the validated JWT (see RequestIdentity) - not
 * the X-User-Id header, which nothing upstream ever actually populates.
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * List all projects for the tenant. Optionally filter by assigned user ID. If userId is provided, only returns projects where that user is assigned (or projects with
     * no assigned users, which are visible to everyone).
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @org.springframework.web.bind.annotation.RequestParam(value = "userId", required = false) String userId) {
        log.debug("Listing projects for tenant: {}, userId: {}", tenantId, userId);
        List<ProjectResponse> projects;
        if (userId != null && !userId.isBlank()) {
            projects = projectService.findByAssignedUserId(tenantId, userId);
        } else {
            projects = projectService.findAll(tenantId);
        }
        return ResponseEntity.ok(projects);
    }

    /**
     * List projects for the tenant and a specific organization.
     */
    @GetMapping("/by-org/{orgId}")
    public ResponseEntity<List<ProjectResponse>> listByOrg(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID orgId) {
        log.debug("Listing projects for tenant: {}, org: {}", tenantId, orgId);
        List<ProjectResponse> projects = projectService.findByOrg(tenantId, orgId);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get a single project by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        ProjectResponse project = projectService.findById(tenantId, id);
        return ResponseEntity.ok(project);
    }

    /**
     * Create a new project.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProjectResponse> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String creatorId = RequestIdentity.creatorId(jwt).toString();
        ProjectResponse created = projectService.create(tenantId, request.orgId(), request, creatorId, jwt.getTokenValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing project (partial update).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String updaterId = RequestIdentity.creatorId(jwt).toString();
        ProjectResponse updated = projectService.update(tenantId, id, request, updaterId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a project.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        projectService.delete(tenantId, id, jwt.getTokenValue());
    }

    /**
     * Assign users to a project with roles. Each assignment includes a user ID (email) and a role (VIEWER, EDITOR, or ADMIN).
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ProjectResponse> assignUsers(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUserAssignmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (request.assignments() == null || request.assignments().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String actingUserId = RequestIdentity.creatorId(jwt).toString();
        ProjectResponse updated = projectService.assignUsersWithRoles(tenantId, id, request.assignments(), actingUserId, jwt.getTokenValue());
        return ResponseEntity.ok(updated);
    }

    /**
     * Assign users to a project (legacy endpoint for backward compatibility - assigns users with VIEWER role by default). Accepts body: { "userIds": ["email1", "email2"]
     * }
     */
    @PostMapping("/{id}/assign-legacy")
    public ResponseEntity<ProjectResponse> assignUsersLegacy(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {
        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) body.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String actingUserId = RequestIdentity.creatorId(jwt).toString();
        ProjectResponse updated = projectService.assignUsers(tenantId, id, userIds, actingUserId, jwt.getTokenValue());
        return ResponseEntity.ok(updated);
    }

    /**
     * Get a project's assigned users enriched with their emails (assignedUserIds itself is UUID-keyed, not human-readable) - used by the Assign Users popup for
     * display. Resolved on demand rather than folded into the main project response, to avoid an identity-service round trip per assignee on every project list/get.
     */
    @GetMapping("/{id}/assigned-users")
    public ResponseEntity<List<AssignedUserResponse>> getAssignedUsers(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        List<AssignedUserResponse> users = projectService.getAssignedUsersWithEmails(tenantId, id, jwt.getTokenValue());
        return ResponseEntity.ok(users);
    }

    /**
     * Remove a user from a project.
     */
    @DeleteMapping("/{id}/assign/{userIdToRemove}")
    public ResponseEntity<ProjectResponse> removeUser(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @PathVariable String userIdToRemove,
            @AuthenticationPrincipal Jwt jwt) {
        ProjectResponse updated = projectService.removeUser(tenantId, id, userIdToRemove, jwt.getTokenValue());
        return ResponseEntity.ok(updated);
    }
}
