package io.cerp.identity.api;

import io.cerp.identity.domain.ResourceMembership;
import io.cerp.identity.domain.ResourceMembership.ResourceType;
import io.cerp.identity.service.ResourceAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for managing resource-level memberships. Enables Owners/Managers to assign users to Projects and Construction Sites with granular
 * read/write/delete/assign permissions.
 */
@RestController
@RequestMapping("/api/v1")
public class ResourceMembershipController {

    private static final Logger log = LoggerFactory.getLogger(ResourceMembershipController.class);

    private final ResourceAccessService resourceAccessService;

    public ResourceMembershipController(ResourceAccessService resourceAccessService) {
        this.resourceAccessService = resourceAccessService;
    }

    // ── List Members ──

    /**
     * Get all users who have access to a specific resource.
     */
    @GetMapping("/resources/{resourceType}/{resourceId}/members")
    public ResponseEntity<List<ResourceMembershipDto>> getResourceMembers(
            @PathVariable String resourceType,
            @PathVariable UUID resourceId) {
        ResourceType type = parseResourceType(resourceType);
        List<ResourceMembership> memberships = resourceAccessService.getResourceMembers(type, resourceId);
        List<ResourceMembershipDto> dtos = memberships.stream()
            .map(ResourceMembershipDto::from)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    // ── Get User Membership ──

    /**
     * Get the access level for a specific user on a resource.
     */
    @GetMapping("/resources/{resourceType}/{resourceId}/members/{userId}")
    public ResponseEntity<ResourceMembershipDto> getUserMembership(
            @PathVariable String resourceType,
            @PathVariable UUID resourceId,
            @PathVariable UUID userId) {
        ResourceType type = parseResourceType(resourceType);
        return resourceAccessService.getUserMembership(type, resourceId, userId)
            .map(m -> ResponseEntity.ok(ResourceMembershipDto.from(m)))
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Create Membership ──

    /**
     * Grant a user access to a resource.
     */
    @PostMapping("/resources/{resourceType}/{resourceId}/members")
    public ResponseEntity<ResourceMembershipDto> createMembership(
            @PathVariable String resourceType,
            @PathVariable UUID resourceId,
            @RequestBody @Valid CreateResourceMembershipRequest request) {
        ResourceType type = parseResourceType(resourceType);
        ResourceMembership membership = resourceAccessService.createMembership(
            type, resourceId, request.userId(),
            request.roles(), request.permissions(),
            request.invitedBy());
        return ResponseEntity.status(201).body(ResourceMembershipDto.from(membership));
    }

    // ── Update Membership ──

    /**
     * Update a user's access level on a resource.
     */
    @PatchMapping("/resources/memberships/{membershipId}")
    public ResponseEntity<ResourceMembershipDto> updateMembership(
            @PathVariable UUID membershipId,
            @RequestBody @Valid UpdateResourceMembershipRequest request) {
        ResourceMembership membership = resourceAccessService.updateMembership(
            membershipId, request.roles(), request.permissions());
        return ResponseEntity.ok(ResourceMembershipDto.from(membership));
    }

    // ── Delete Membership ──

    /**
     * Revoke a user's access to a resource.
     */
    @DeleteMapping("/resources/memberships/{membershipId}")
    public ResponseEntity<Void> deleteMembership(@PathVariable UUID membershipId) {
        resourceAccessService.deleteMembership(membershipId);
        return ResponseEntity.noContent().build();
    }

    // ── Service-to-Service Endpoints for Workflow Service ──

    /**
     * Get all project resource IDs that a user has access to.
     * Returns empty list if user is admin/manager (full access).
     * Used by workflow-service for saga listing access control.
     * Internal endpoint - called service-to-service.
     */
    @GetMapping("/users/{userId}/accessible-projects")
    public ResponseEntity<List<UUID>> getAccessibleProjects(
            @PathVariable UUID userId) {
        List<UUID> projectIds = resourceAccessService.getAccessibleResourceIds(userId, ResourceType.PROJECT);
        return ResponseEntity.ok(projectIds);
    }

    /**
     * Check if a user has write permission on a specific project.
     * Returns true if user is admin/manager at tenant level OR has write permission on the project.
     * Internal endpoint - called service-to-service.
     */
    @GetMapping("/users/{userId}/can-write-project")
    public ResponseEntity<Map<String, Boolean>> canWriteOnProject(
            @PathVariable UUID userId,
            @RequestParam UUID projectId) {
        boolean canWrite = resourceAccessService.canWrite(userId, ResourceType.PROJECT, projectId);
        return ResponseEntity.ok(Map.of("canWrite", canWrite));
    }

    /**
     * Check if a user has read permission on a specific project.
     * Returns true if user is admin/manager at tenant level OR has read permission on the project.
     * Internal endpoint - called service-to-service.
     */
    @GetMapping("/users/{userId}/can-read-project")
    public ResponseEntity<Map<String, Boolean>> canReadOnProject(
            @PathVariable UUID userId,
            @RequestParam UUID projectId) {
        boolean canRead = resourceAccessService.canRead(userId, ResourceType.PROJECT, projectId);
        return ResponseEntity.ok(Map.of("canRead", canRead));
    }

    /**
     * Check if a user has delete permission on a specific project.
     * Internal endpoint - called service-to-service.
     */
    @GetMapping("/users/{userId}/can-delete-project")
    public ResponseEntity<Map<String, Boolean>> canDeleteOnProject(
            @PathVariable UUID userId,
            @RequestParam UUID projectId) {
        boolean canDelete = resourceAccessService.canDelete(userId, ResourceType.PROJECT, projectId);
        return ResponseEntity.ok(Map.of("canDelete", canDelete));
    }

    /**
     * Check if a user is admin or manager at the tenant level (full access to all resources).
     * Internal endpoint - called service-to-service.
     */
    @GetMapping("/users/{userId}/is-tenant-admin-or-manager")
    public ResponseEntity<Map<String, Boolean>> isTenantAdminOrManager(
            @PathVariable UUID userId) {
        boolean isAdminOrManager = resourceAccessService.isTenantAdminOrManager(userId);
        return ResponseEntity.ok(Map.of("isAdminOrManager", isAdminOrManager));
    }

    // ── Quick Role Assignment ──

    /**
     * Quickly assign a standard role to a user on a resource. This sets both roles and permissions based on the role name.
     */
    @PostMapping("/resources/{resourceType}/{resourceId}/members/{userId}/role/{roleName}")
    public ResponseEntity<ResourceMembershipDto> assignRole(
            @PathVariable String resourceType,
            @PathVariable UUID resourceId,
            @PathVariable UUID userId,
            @PathVariable String roleName) {
        ResourceType type = parseResourceType(resourceType);

        // Check if membership already exists, create if not
        Map<String, Boolean> permissions = resolveRolePermissions(roleName);
        List<String> roles = List.of(roleName);

        ResourceMembership membership;
        try {
            membership = resourceAccessService.createMembership(
                type, resourceId, userId, roles, permissions, null);
        } catch (IllegalArgumentException e) {
            // Update existing membership with the new role
            membership = resourceAccessService
                .getUserMembership(type, resourceId, userId)
                .orElseThrow(() -> new RuntimeException("No membership found"));
            membership = resourceAccessService.updateMembership(membership.getId(), roles, permissions);
        }

        return ResponseEntity.ok(ResourceMembershipDto.from(membership));
    }

    // ── Helper Methods ──

    private ResourceType parseResourceType(String type) {
        try {
            return ResourceType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid resource type: " + type
                    + ". Must be one of: PROJECT, CONSTRUCTION_SITE, SAGA");
        }
    }

    private Map<String, Boolean> resolveRolePermissions(String roleName) {
        return switch (roleName.toLowerCase()) {
            case "owner" -> ResourceAccessService.getOwnerPermissions();
            case "manager" -> ResourceAccessService.getManagerPermissions();
            case "member" -> ResourceAccessService.getMemberPermissions();
            case "viewer" -> ResourceAccessService.getViewerPermissions();
            default -> ResourceAccessService.getDefaultPermissions();
        };
    }

    // ── DTOs ──

    public record ResourceMembershipDto(
            UUID id,
            String resourceType,
            UUID resourceId,
            UUID userId,
            List<String> roles,
            Map<String, Boolean> permissions,
            UUID invitedBy,
            String createdAt) {

        public static ResourceMembershipDto from(ResourceMembership m) {
            return new ResourceMembershipDto(
                m.getId(),
                m.getResourceType().name(),
                m.getResourceId(),
                m.getUserId(),
                m.getRoles(),
                m.getPermissions(),
                m.getInvitedBy(),
                m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
        }
    }

    public record CreateResourceMembershipRequest(
            @NotNull(message = "userId is required") UUID userId,
            List<String> roles,
            Map<String, Boolean> permissions,
            UUID invitedBy) {
    }

    public record UpdateResourceMembershipRequest(
            List<String> roles,
            Map<String, Boolean> permissions) {
    }
}
