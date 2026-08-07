package io.cerp.project.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Resolves a user's plan/quota limits from identity-service, mirroring workflow-service's IdentityPermissionClient construction style (plain @Component, RestClient built
 * inline from @Value-injected config, fail-{open,closed} decided per-caller rather than baked into this class).
 */
@Component
public class IdentitySubscriptionClient {

    private static final Logger log = LoggerFactory.getLogger(IdentitySubscriptionClient.class);

    private final RestClient restClient;

    public IdentitySubscriptionClient(@Value("${cerp.identity-service.url:http://localhost:8090}") String identityServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(identityServiceUrl).build();
    }

    public record PlanLimits(
            Integer maxConstructionSites,
            Integer maxBuildings,
            Integer maxOwnersPerProject,
            Integer maxManagersPerProject,
            Integer maxMembersPerProject) {
    }

    /**
     * Returns empty if the user has no subscription or identity-service could not be reached/errored - callers decide whether that means fail-open or fail-closed for
     * their specific check (quota checks in this service fail closed - see QuotaCheckUnavailableException).
     */
    @SuppressWarnings("unchecked")
    public Optional<PlanLimits> getPlanLimits(UUID userId) {
        try {
            Map<String, Object> response = restClient.get()
                .uri("/api/v1/users/{userId}/subscription", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
            if (response == null) {
                return Optional.empty();
            }
            Map<String, Object> limits = (Map<String, Object>) response.get("limits");
            if (limits == null) {
                return Optional.empty();
            }
            return Optional.of(new PlanLimits(
                asInteger(limits.get("maxConstructionSites")),
                asInteger(limits.get("maxBuildings")),
                asInteger(limits.get("maxOwnersPerProject")),
                asInteger(limits.get("maxManagersPerProject")),
                asInteger(limits.get("maxMembersPerProject"))));
        } catch (Exception e) {
            log.warn("Failed to fetch plan limits for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }
}
