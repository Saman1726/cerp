package io.cerp.vendor.client;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for the identity service user endpoints. Used to resolve email from JWT sub claim to user UUID.
 */
@Component
@ConditionalOnProperty(name = "IDENTITY_SERVICE_URL", matchIfMissing = false)
public class IdentityServiceClient {

    private final RestClient restClient;
    private final String identityServiceUrl;

    public IdentityServiceClient(
            @Value("${IDENTITY_SERVICE_URL:http://localhost:8080}") String identityServiceUrl,
            RestClient.Builder restClientBuilder) {
        this.identityServiceUrl = identityServiceUrl;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Look up a user by email and return the user ID.
     */
    public UUID resolveUserIdByEmail(String email) {
        String uri = UriComponentsBuilder
            .fromUriString(identityServiceUrl)
            .path("/users")
            .queryParam("email", email)
            .build()
            .toUriString();

        UserProfileResponse response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(UserProfileResponse.class);

        if (response == null) {
            throw new IllegalStateException("User not found for email: " + email);
        }
        return response.id();
    }

    public record UserProfileResponse(
            UUID id,
            String email,
            String name,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }
}
