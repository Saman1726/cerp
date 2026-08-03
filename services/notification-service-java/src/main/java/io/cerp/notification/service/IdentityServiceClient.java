package io.cerp.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for resolving users via the Identity Service.
 * Used by the notification service to resolve recipients by email or role.
 */
@Service
public class IdentityServiceClient {

    private final RestClient restClient;

    public IdentityServiceClient(
            @Value("${notification.identity-service.url:http://identity-service-java:8090}") String identityServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(identityServiceUrl)
                .build();
    }

    /**
     * Resolve a user ID by email.
     * @param email the user's email address
     * @return the user ID, or null if not found
     */
    public UUID resolveUserByEmail(String email) {
        try {
            return restClient.get()
                    .uri("/api/users/by-email?email={0}", email)
                    .retrieve()
                    .toEntity(UUID.class)
                    .getBody();
        } catch (Exception e) {
            // Log but don't fail the entire job creation
            System.err.println("Failed to resolve user by email: " + email + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Resolve user IDs by role(s).
     * @param roles the role names
     * @param tenantId the tenant ID
     * @return list of user IDs with the specified roles
     */
    public List<UUID> resolveUsersByRoles(List<String> roles, String tenantId) {
        try {
            String rolesParam = String.join(",", roles);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/users/by-roles")
                            .queryParam("roles", rolesParam)
                            .queryParam("tenantId", tenantId)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UUID>>() {});
        } catch (Exception e) {
            System.err.println("Failed to resolve users by roles: " + roles + " - " + e.getMessage());
            return List.of();
        }
    }
}