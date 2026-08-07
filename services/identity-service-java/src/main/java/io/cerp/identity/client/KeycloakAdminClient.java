package io.cerp.identity.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Issues real Keycloak tokens and provisions Keycloak users on behalf of identity-service.
 *
 * Keycloak does not honor a client-supplied "id" on user creation (confirmed empirically - it silently assigns its own, and any request against the id we asked for
 * 404s) - so identity-service's own UserAccount id cannot be assumed to equal the token's "sub" claim. Instead, every Keycloak user carries identity-service's id in a
 * dedicated "internal_user_id" attribute, exposed as a same-named token claim via a protocol mapper (see KeycloakBootstrap) - callers must read that claim, not "sub".
 */
@Component
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminClient(
            @Value("${cerp.keycloak.url}") String keycloakUrl,
            @Value("${cerp.keycloak.realm}") String realm,
            @Value("${cerp.keycloak.client-id}") String clientId,
            @Value("${cerp.keycloak.client-secret}") String clientSecret) {
        this.restClient = RestClient.builder().baseUrl(keycloakUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public record TokenResponse(String accessToken, String refreshToken) {
    }

    /**
     * Direct Access Grant: exchange a username+password for a real, signed token pair.
     */
    public TokenResponse getUserToken(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);
        return requestToken(form);
    }

    /**
     * Refresh-token grant: exchange a refresh token for a new signed token pair.
     */
    public TokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        return requestToken(form);
    }

    /**
     * Provision (or update) the Keycloak user backing this account, and set its password. Fails loudly (throws) rather than degrading silently - a user Keycloak doesn't
     * know about can never log in.
     *
     * tenantId is nullable: at initial user creation, no Membership (and therefore no tenant) exists yet - the tenant_id attribute/claim is set best-effort, not relied
     * on anywhere today (tenant resolution still goes through the existing X-Tenant-Id header, which already works). Pass null if not yet known.
     */
    public void upsertUser(UUID userId, String email, @Nullable String rawPassword, String accountType, @Nullable UUID tenantId) {
        String adminToken = getServiceAccountToken();
        Map<String, List<String>> attributes = buildAttributes(userId, accountType, tenantId);

        String keycloakUserId = findUserIdByUsername(adminToken, email);
        if (keycloakUserId != null) {
            Map<String, Object> updatedUser = new HashMap<>();
            updatedUser.put("username", email);
            updatedUser.put("email", email);
            updatedUser.put("enabled", true);
            updatedUser.put("emailVerified", true);
            updatedUser.put("attributes", attributes);
            restClient.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, keycloakUserId)
                .header("Authorization", "Bearer " + adminToken)
                .body(updatedUser)
                .retrieve()
                .toBodilessEntity();
        } else {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("username", email);
            newUser.put("email", email);
            newUser.put("enabled", true);
            newUser.put("emailVerified", true);
            newUser.put("attributes", attributes);
            restClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header("Authorization", "Bearer " + adminToken)
                .body(newUser)
                .retrieve()
                .toBodilessEntity();
            keycloakUserId = findUserIdByUsername(adminToken, email);
            if (keycloakUserId == null) {
                throw new IllegalStateException("Created Keycloak user for " + email + " but could not look it back up");
            }
        }

        if (rawPassword != null) {
            restClient.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, keycloakUserId)
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("type", "password", "value", rawPassword, "temporary", false))
                .retrieve()
                .toBodilessEntity();
        }
    }

    private Map<String, List<String>> buildAttributes(UUID userId, String accountType, @Nullable UUID tenantId) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("internal_user_id", List.of(userId.toString()));
        attributes.put("account_type", List.of(accountType));
        if (tenantId != null) {
            attributes.put("tenant_id", List.of(tenantId.toString()));
        }
        return attributes;
    }

    @Nullable
    private String findUserIdByUsername(String adminToken, String username) {
        List<Map<String, Object>> results = restClient.get()
            .uri("/admin/realms/{realm}/users?username={username}&exact=true", realm, username)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
            });
        return results != null && !results.isEmpty() ? (String) results.get(0).get("id") : null;
    }

    private String getServiceAccountToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        return requestToken(form).accessToken();
    }

    private TokenResponse requestToken(MultiValueMap<String, String> form) {
        Map<String, Object> body = restClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {
            });
        if (body == null) {
            throw new IllegalStateException("Empty token response from Keycloak");
        }
        return new TokenResponse((String) body.get("access_token"), (String) body.get("refresh_token"));
    }
}
