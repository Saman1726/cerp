package io.cerp.project.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import io.cerp.project.dto.ProjectCreateRequest;
import io.cerp.project.dto.ProjectResponse;
import io.cerp.project.dto.ProjectUpdateRequest;
import io.cerp.project.dto.ProjectUserAssignmentRequest;
import io.cerp.project.service.ProjectService;

/**
 * REST controller for project CRUD operations.
 *
 * <p>
 * Tenant context is extracted from the X-Tenant-Id header (injected by the API Gateway). User identity is extracted from the JWT token.
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
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse created = projectService.create(tenantId, request.orgId(), request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing project (partial update).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        ProjectResponse updated = projectService.update(tenantId, id, request, userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a project.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        projectService.delete(tenantId, id);
    }

    /**
     * Assign users to a project with roles. Each assignment includes a user ID (email) and a role (VIEWER, EDITOR, or ADMIN).
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ProjectResponse> assignUsers(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUserAssignmentRequest request) {
        if (request.assignments() == null || request.assignments().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ProjectResponse updated = projectService.assignUsersWithRoles(tenantId, id, request.assignments(), userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Assign users to a project (legacy endpoint for backward compatibility - assigns users with VIEWER role by default). Accepts body: { "userIds": ["email1", "email2"]
     * }
     */
    @PostMapping("/{id}/assign-legacy")
    public ResponseEntity<ProjectResponse> assignUsersLegacy(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) body.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ProjectResponse updated = projectService.assignUsers(tenantId, id, userIds, userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove a user from a project.
     */
    @DeleteMapping("/{id}/assign/{userIdToRemove}")
    public ResponseEntity<ProjectResponse> removeUser(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @PathVariable UUID id,
            @PathVariable String userIdToRemove) {
        ProjectResponse updated = projectService.removeUser(tenantId, id, userIdToRemove);
        return ResponseEntity.ok(updated);
    }
}
