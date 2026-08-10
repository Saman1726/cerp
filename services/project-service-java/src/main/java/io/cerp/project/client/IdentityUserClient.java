package io.cerp.project.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import io.cerp.project.service.PermissionSyncUnavailableException;

/**
 * Resolves user identities against identity-service and keeps its resource-level permissions (resource_members, consulted by workflow-service's write-permission
 * checks) in sync with project-level assignments. Mirrors IdentitySubscriptionClient's construction style (plain @Component, RestClient built inline from
 * @Value-injected config). Note the two request-mapping prefixes below are not a typo: identity-service's plain user CRUD lives at "/users" while its resource-
 * membership endpoints live at "/api/v1/resources/..." - that split is on the identity-service side, not introduced here.
 */
@Component
public class IdentityUserClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityUserClient.class);

    private final RestClient restClient;

    public IdentityUserClient(@Value("${cerp.identity-service.url:http://localhost:8090}") String identityServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(identityServiceUrl).build();
    }

    private record UserResponse(UUID id, String email) {
    }

    /**
     * Resolves an email-or-UUID string (the Assign Users modal sends emails; already-stored owner entries are UUIDs) to a real identity-service user UUID. Returns
     * empty if the string is neither a valid UUID nor a registered user's email - callers should treat that as a client error (e.g. "no such user"), not a sync
     * failure.
     */
    public Optional<UUID> resolveUserId(String emailOrUuid) {
        try {
            return Optional.of(UUID.fromString(emailOrUuid));
        } catch (IllegalArgumentException notAUuid) {
            try {
                UserResponse user = restClient.get()
                    .uri("/users?email={email}", emailOrUuid)
                    .retrieve()
                    .body(UserResponse.class);
                return Optional.ofNullable(user).map(UserResponse::id);
            } catch (HttpClientErrorException.NotFound notFound) {
                return Optional.empty();
            }
        }
    }

    /**
     * Resolves a user's UUID back to their email, for display purposes only. Empty if the user no longer exists; callers should fall back to showing the raw UUID
     * rather than failing the whole request over a display nicety. callerToken is the acting user's own bearer token (GET /users/{id} requires authentication,
     * unlike the email-lookup form above - see class-level note on the "/users" vs "/users/**" split) and is just forwarded, not inspected.
     */
    public Optional<String> resolveEmail(String callerToken, UUID userId) {
        try {
            UserResponse user = restClient.get()
                .uri("/users/{id}", userId)
                .header("Authorization", "Bearer " + callerToken)
                .retrieve()
                .body(UserResponse.class);
            return Optional.ofNullable(user).map(UserResponse::email);
        } catch (Exception e) {
            log.warn("Failed to resolve email for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Grants (or updates, if one already exists) a PROJECT-resource membership for userId with the given role, so workflow-service's write-permission checks see
     * this assignment. Role must be one of identity-service's recognized names: owner, manager, member, viewer. callerToken is the acting user's own bearer token -
     * these endpoints require authentication (deliberately: unlike the read-only permission-check endpoints, granting/revoking access is not something to leave
     * open to any unauthenticated caller) but don't otherwise care who the caller is, so the already-validated incoming token is simply forwarded.
     */
    public void grantProjectMembership(String callerToken, UUID projectId, UUID userId, String role) {
        try {
            restClient.post()
                .uri("/api/v1/resources/PROJECT/{resourceId}/members/{userId}/role/{role}", projectId, userId, role)
                .header("Authorization", "Bearer " + callerToken)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            throw new PermissionSyncUnavailableException(
                "Could not grant project permissions for user " + userId + " via identity-service", e);
        }
    }

    /**
     * Revokes a user's PROJECT-resource membership, if one exists. No-op if the user never had one.
     */
    @SuppressWarnings("unchecked")
    public void revokeProjectMembership(String callerToken, UUID projectId, UUID userId) {
        try {
            Map<String, Object> membership;
            try {
                membership = restClient.get()
                    .uri("/api/v1/resources/PROJECT/{resourceId}/members/{userId}", projectId, userId)
                    .header("Authorization", "Bearer " + callerToken)
                    .retrieve()
                    .body(Map.class);
            } catch (HttpClientErrorException.NotFound notFound) {
                return;
            }
            if (membership == null || membership.get("id") == null) {
                return;
            }
            restClient.delete()
                .uri("/api/v1/resources/memberships/{membershipId}", membership.get("id"))
                .header("Authorization", "Bearer " + callerToken)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            throw new PermissionSyncUnavailableException(
                "Could not revoke project permissions for user " + userId + " via identity-service", e);
        }
    }
}
