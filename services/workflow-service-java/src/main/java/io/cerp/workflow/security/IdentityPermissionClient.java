package io.cerp.workflow.security;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for calling identity-service REST API to check project permissions.
 * Used by workflow-service for saga listing access control.
 * Fail-open: if identity-service is unavailable, allows access by default.
 */
@Component
public class IdentityPermissionClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityPermissionClient.class);

    private final RestClient restClient;
    private final RestClient restClientNoRetry;

    public IdentityPermissionClient(
            @Value("${cerp.identity-service.url:http://localhost:8081}") String identityServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(identityServiceUrl).build();
        this.restClientNoRetry = RestClient.builder().baseUrl(identityServiceUrl).build();
    }

    /**
     * Get all project resource IDs that a user has access to.
     * Returns empty list if user is admin/manager (full access).
     */
    public List<UUID> getAccessibleProjects(UUID userId) {
        try {
            return restClient.get()
                .uri("/api/v1/users/{userId}/accessible-projects", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UUID>>() {
                });
        } catch (Exception e) {
            log.warn("Failed to fetch accessible projects for user {}: {}", userId, e.getMessage());
            return List.of(); // fail-open: empty list means no projects
        }
    }

    /**
     * Check if a user can write on a specific project.
     */
    public boolean canWriteOnProject(UUID userId, UUID projectId) {
        try {
            Map<String, Boolean> response = restClient.get()
                .uri("/api/v1/users/{userId}/can-write-project?projectId={projectId}", userId, projectId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Boolean>>() {
                });
            return response != null && Boolean.TRUE.equals(response.get("canWrite"));
        } catch (Exception e) {
            log.warn("Failed to check write permission for user {} on project {}: {}", userId, projectId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user can read on a specific project.
     */
    public boolean canReadOnProject(UUID userId, UUID projectId) {
        try {
            Map<String, Boolean> response = restClient.get()
                .uri("/api/v1/users/{userId}/can-read-project?projectId={projectId}", userId, projectId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Boolean>>() {
                });
            return response != null && Boolean.TRUE.equals(response.get("canRead"));
        } catch (Exception e) {
            log.warn("Failed to check read permission for user {} on project {}: {}", userId, projectId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user can delete on a specific project.
     */
    public boolean canDeleteOnProject(UUID userId, UUID projectId) {
        try {
            Map<String, Boolean> response = restClient.get()
                .uri("/api/v1/users/{userId}/can-delete-project?projectId={projectId}", userId, projectId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Boolean>>() {
                });
            return response != null && Boolean.TRUE.equals(response.get("canDelete"));
        } catch (Exception e) {
            log.warn("Failed to check delete permission for user {} on project {}: {}", userId, projectId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user is admin or manager at the tenant level (full access to all resources).
     */
    public boolean isTenantAdminOrManager(UUID userId) {
        try {
            Map<String, Boolean> response = restClient.get()
                .uri("/api/v1/users/{userId}/is-tenant-admin-or-manager", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Boolean>>() {
                });
            return response != null && Boolean.TRUE.equals(response.get("isAdminOrManager"));
        } catch (Exception e) {
            log.warn("Failed to check tenant admin status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Resolve a user's UUID from their email address.
     * Used to convert legacy email-based sub claims in JWT tokens to proper UUIDs.
     * Returns null if not found or if the identity service is unavailable.
     */
    public UUID resolveUserIdByEmail(String email) {
        try {
            Map<String, Object> response = restClientNoRetry.get()
                .uri("/api/v1/users?email={email}", email)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response != null && response.get("id") instanceof String idStr) {
                return UUID.fromString(idStr);
            }
            log.warn("No id field in identity service response for email: {}", email);
        } catch (Exception e) {
            log.warn("Failed to resolve userId by email {}: {}", email, e.getMessage());
        }
        return null;
    }

    /**
     * Return all userIds that are members of the given tenant.
     * Used to route vendor invite notifications to whoever currently logs in under that tenant,
     * regardless of what userId is stored on the vendor record (which can be stale).
     */
    public List<UUID> resolveVendorUserIdsByTenantId(UUID tenantId) {
        try {
            List<Map<String, Object>> memberships = restClientNoRetry.get()
                .uri("/memberships?tenantId={tenantId}", tenantId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (memberships == null) return List.of();
            return memberships.stream()
                .map(m -> m.get("userId"))
                .filter(id -> id instanceof String)
                .map(id -> {
                    try { return UUID.fromString((String) id); } catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (Exception e) {
            log.warn("Failed to resolve vendor users for tenantId {}: {}", tenantId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Check if a user has ANY project write permission (for saga creation).
     */
    public boolean hasAnyProjectWritePermission(UUID userId) {
        List<UUID> projects = getAccessibleProjects(userId);
        // Empty list means admin/manager (full access)
        if (projects.isEmpty()) {
            return true;
        }
        for (UUID projectId : projects) {
            if (canWriteOnProject(userId, projectId)) {
                return true;
            }
        }
        return false;
    }
}