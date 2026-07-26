package io.cerp.identity.service;

import io.cerp.identity.domain.ResourceMembership;
import io.cerp.identity.domain.ResourceMembership.ResourceType;
import io.cerp.identity.repo.MembershipRepository;
import io.cerp.identity.repo.ResourceMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing resource-level access control. Handles permission checks, membership assignment, and resource access queries.
 */
@Service
public class ResourceAccessService {

    private static final Logger log = LoggerFactory.getLogger(ResourceAccessService.class);
    private static final Set<String> ALL_PERMISSIONS = Set.of("read", "write", "delete", "assign");

    private final ResourceMembershipRepository resourceMembershipRepo;
    private final MembershipRepository membershipRepo;

    public ResourceAccessService(ResourceMembershipRepository resourceMembershipRepo,
            MembershipRepository membershipRepo) {
        this.resourceMembershipRepo = resourceMembershipRepo;
        this.membershipRepo = membershipRepo;
    }

    // ── Permission Checks ──

    /**
     * Check if a user has a specific permission on a resource. Returns true if user is tenant admin/manager OR has direct resource permission.
     */
    public boolean hasPermission(UUID userId, ResourceType resourceType, UUID resourceId, String permission) {
        if (!ALL_PERMISSIONS.contains(permission)) {
            return false;
        }

        // Check tenant-level admin/manager (full access to all resources)
        if (isTenantAdminOrManager(userId)) {
            log.debug("User {} has tenant-level admin/manager, allowing access to {}/{}", userId, resourceType, resourceId);
            return true;
        }

        // Check direct resource membership
        Optional<ResourceMembership> membership = resourceMembershipRepo
            .findByResourceTypeAndResourceIdAndUserId(resourceType, resourceId, userId);

        boolean hasPermission = membership.map(m -> Boolean.TRUE.equals(m.getPermissions().get(permission)))
            .orElse(false);

        log.debug("User {} permission '{}' on {}/{}: {}", userId, permission, resourceType, resourceId, hasPermission);
        return hasPermission;
    }

    /**
     * Check if a user has read permission on a resource.
     */
    public boolean canRead(UUID userId, ResourceType resourceType, UUID resourceId) {
        return hasPermission(userId, resourceType, resourceId, "read");
    }

    /**
     * Check if a user has write permission on a resource.
     */
    public boolean canWrite(UUID userId, ResourceType resourceType, UUID resourceId) {
        return hasPermission(userId, resourceType, resourceId, "write");
    }

    /**
     * Check if a user has delete permission on a resource.
     */
    public boolean canDelete(UUID userId, ResourceType resourceType, UUID resourceId) {
        return hasPermission(userId, resourceType, resourceId, "delete");
    }

    /**
     * Check if a user has assign (manage members) permission on a resource.
     */
    public boolean canAssign(UUID userId, ResourceType resourceType, UUID resourceId) {
        return hasPermission(userId, resourceType, resourceId, "assign");
    }

    /**
     * Get all resource IDs of a given type that a user has access to.
     */
    public List<UUID> getAccessibleResourceIds(UUID userId, ResourceType resourceType) {
        // If admin/manager, return all resource IDs (caller should provide this list)
        if (isTenantAdminOrManager(userId)) {
            return List.of(); // Signal to caller that user has full access
        }

        return resourceMembershipRepo.findResourceIdsByUserIdAndType(userId, resourceType);
    }

    /**
     * Check if a user is admin or manager at the tenant level.
     */
    private boolean isTenantAdminOrManager(UUID userId) {
        List<String> roles = membershipRepo.findRolesByUserId(userId);
        if (roles == null) {
            return false;
        }
        return roles.stream().anyMatch(r -> "admin".equalsIgnoreCase(r) || "manager".equalsIgnoreCase(r));
    }

    // ── Resource Membership CRUD ──

    /**
     * Create a new resource membership.
     */
    @Transactional
    public ResourceMembership createMembership(ResourceType resourceType, UUID resourceId, UUID userId,
            List<String> roles, Map<String, Boolean> permissions, UUID invitedBy) {
        // Check if membership already exists
        Optional<ResourceMembership> existing = resourceMembershipRepo
            .findByResourceTypeAndResourceIdAndUserId(resourceType, resourceId, userId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User already has access to this resource");
        }

        ResourceMembership membership = new ResourceMembership(
            resourceType, resourceId, userId,
            roles != null ? roles : List.of("viewer"),
            permissions != null ? permissions
                    : Map.of(
                        "read", true,
                        "write", false,
                        "delete", false,
                        "assign", false),
            invitedBy);

        resourceMembershipRepo.save(membership);
        log.info("Created resource membership: user={} on {}/{}", userId, resourceType, resourceId);
        return membership;
    }

    /**
     * Update an existing resource membership's roles and permissions.
     */
    @Transactional
    public ResourceMembership updateMembership(UUID membershipId,
            List<String> roles, Map<String, Boolean> permissions) {
        ResourceMembership membership = resourceMembershipRepo.findById(membershipId)
            .orElseThrow(() -> new NoSuchElementException("Resource membership not found: " + membershipId));

        if (roles != null) {
            membership.setRoles(roles);
        }
        if (permissions != null) {
            membership.setPermissions(permissions);
        }

        resourceMembershipRepo.save(membership);
        log.info("Updated resource membership: {} (roles={}, permissions={})", membershipId, roles, permissions);
        return membership;
    }

    /**
     * Delete a resource membership (revoke access).
     */
    @Transactional
    public void deleteMembership(UUID membershipId) {
        if (!resourceMembershipRepo.existsById(membershipId)) {
            throw new NoSuchElementException("Resource membership not found: " + membershipId);
        }
        resourceMembershipRepo.deleteById(membershipId);
        log.info("Deleted resource membership: {}", membershipId);
    }

    /**
     * Get all memberships for a specific resource.
     */
    @Transactional(readOnly = true)
    public List<ResourceMembership> getResourceMembers(ResourceType resourceType, UUID resourceId) {
        return resourceMembershipRepo.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    /**
     * Get the membership for a specific user on a resource.
     */
    @Transactional(readOnly = true)
    public Optional<ResourceMembership> getUserMembership(ResourceType resourceType, UUID resourceId, UUID userId) {
        return resourceMembershipRepo.findByResourceTypeAndResourceIdAndUserId(resourceType, resourceId, userId);
    }

    /**
     * Get all resource memberships for a user.
     */
    @Transactional(readOnly = true)
    public List<ResourceMembership> getUserMemberships(UUID userId) {
        return resourceMembershipRepo.findByUserId(userId);
    }

    /**
     * Delete all memberships for a specific resource (cleanup when resource is deleted).
     */
    @Transactional
    public void deleteResourceMemberships(ResourceType resourceType, UUID resourceId) {
        resourceMembershipRepo.deleteByResourceTypeAndResourceId(resourceType, resourceId);
        log.info("Deleted all memberships for {}/{}", resourceType, resourceId);
    }

    // ── Resource Access Info (for login response) ──

    /**
     * Get structured access info for a user to include in login response.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserAccessInfo(UUID userId) {
        List<ResourceMembership> memberships = resourceMembershipRepo.findByUserId(userId);

        return memberships.stream().map(m -> {
            Map<String, Object> info = new HashMap<>();
            info.put("resourceType", m.getResourceType().name());
            info.put("resourceId", m.getResourceId());
            info.put("roles", m.getRoles());
            info.put("permissions", m.getPermissions());
            return info;
        }).collect(Collectors.toList());
    }

    // ── Default Permissions ──

    /**
     * Get default permissions map.
     */
    public static Map<String, Boolean> getDefaultPermissions() {
        return Map.of(
            "read", true,
            "write", false,
            "delete", false,
            "assign", false);
    }

    /**
     * Viewer permissions.
     */
    public static Map<String, Boolean> getViewerPermissions() {
        return Map.of(
            "read", true,
            "write", false,
            "delete", false,
            "assign", false);
    }

    /**
     * Member permissions (read + write).
     */
    public static Map<String, Boolean> getMemberPermissions() {
        return Map.of(
            "read", true,
            "write", true,
            "delete", false,
            "assign", false);
    }

    /**
     * Manager permissions (read + write + delete).
     */
    public static Map<String, Boolean> getManagerPermissions() {
        return Map.of(
            "read", true,
            "write", true,
            "delete", true,
            "assign", false);
    }

    /**
     * Owner permissions (full access).
     */
    public static Map<String, Boolean> getOwnerPermissions() {
        return Map.of(
            "read", true,
            "write", true,
            "delete", true,
            "assign", true);
    }
}
