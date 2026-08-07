package io.cerp.core.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Auth endpoints for the web app. - POST /auth/login accepts username + password - Tries identity-service /auth/login first, falls back to partner-system - POST
 * /auth/refresh issues new tokens from a refresh token
 *
 * For bootstrap/demo: - If identity-service fails, we discover the correct partner by iterating known partner codes from Partner System and attempting a login until one
 * succeeds. - Tokens are unsigned "dev" JWTs carrying exp/sub/role for UX middleware. API does not verify them.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;
    private final String keycloakUrl;
    private final String keycloakRealm;
    private final String keycloakClientId;
    private final String keycloakClientSecret;

    public AuthController(
            RestTemplate restTemplate,
            @Value("${cerp.identity-service.url:http://localhost:8090}") String identityServiceUrl,
            @Value("${cerp.keycloak.url:http://localhost:8081}") String keycloakUrl,
            @Value("${cerp.keycloak.realm:cerp}") String keycloakRealm,
            @Value("${cerp.keycloak.client-id:cerp-identity-service}") String keycloakClientId,
            @Value("${cerp.keycloak.client-secret:cerp-identity-service-dev-secret}") String keycloakClientSecret) {
        this.restTemplate = restTemplate;
        this.identityServiceUrl = identityServiceUrl;
        this.keycloakUrl = keycloakUrl;
        this.keycloakRealm = keycloakRealm;
        this.keycloakClientId = keycloakClientId;
        this.keycloakClientSecret = keycloakClientSecret;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        // Strategy: Try identity-service for login
        try {
            ResponseEntity<IdentityLoginResponse> resp = restTemplate.postForEntity(
                identityServiceUrl + "/auth/login",
                new IdentityLoginRequest(req.username(), req.password()),
                IdentityLoginResponse.class);

            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                // Pass through identity-service tokens directly — they already contain UUID sub and tenantId
                String accessToken = resp.getBody().accessToken();
                String refreshToken = resp.getBody().refreshToken();
                IdentityLoginResponse.IdentityUser identityUser = resp.getBody().user();
                List<String> roles = identityUser.roles();
                String primaryRole = roles != null && !roles.isEmpty() ? roles.get(0) : "user";

                return new LoginResponse(accessToken, refreshToken,
                    new AuthUser(identityUser.id(), identityUser.email(), primaryRole, identityUser.type()));
            }
        } catch (Exception e) {
            // identity-service not available or error
        }

        // If identity-service login fails, return unauthorized
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest req) {
        // Delegate to Keycloak's own refresh-token grant - the old code decoded our
        // unsigned dev JWT and hand-minted a "new" one locally, which only ever worked
        // because nothing downstream actually verified a signature. Real Keycloak tokens
        // are signed; a hand-minted replacement would just be rejected everywhere else.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakClientId);
        form.add("client_secret", keycloakClientSecret);
        form.add("refresh_token", req.refreshToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, Object> body;
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
            body = resp.getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (body == null || body.get("access_token") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String newAccess = (String) body.get("access_token");
        String newRefresh = (String) body.get("refresh_token");

        // internal_user_id/email are present on every Keycloak token issued for our realm
        // (see KeycloakBootstrap/KeycloakAdminClient) without another round-trip to
        // identity-service; role/type aren't recoverable here (they'd need identity-service
        // itself) - left null, same accepted limitation as before. Deliberately read
        // "internal_user_id", not the token's own "sub" - Keycloak's internal id is not our
        // app's user id (see KeycloakAdminClient's javadoc).
        String payloadJson = decodeJwtPayload(newAccess);
        String internalUserId = payloadJson != null ? extractJsonString(payloadJson, "internal_user_id") : null;
        String email = payloadJson != null ? extractJsonString(payloadJson, "email") : null;

        AuthUser user = new AuthUser(internalUserId, email, null, null);
        return new LoginResponse(newAccess, newRefresh, user);
    }

    // ----- Helper: read-only claim extraction from a JWT payload (works whether the
    // token is signed or not - signing only affects the signature segment) -----

    private static String decodeJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2)
                return null;
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonString(String json, String key) {
        // naive extractor for fields like "key":"value"
        String needle = "\"" + key + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0)
            return null;
        int from = start + needle.length();
        int to = json.indexOf('"', from);
        if (to < 0)
            return null;
        return json.substring(from, to);
    }

    // ----- Identity-service request/response DTOs -----

    private record IdentityLoginRequest(String username, String password) {
    }

    private record IdentityLoginResponse(
            String accessToken,
            String refreshToken,
            IdentityUser user) {

        private record IdentityUser(
                String id,
                String email,
                String name,
                String type,
                List<String> roles) {
        }
    }

    // ----- DTOs matching packages/shared-types/src/auth.ts -----

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken) {
    }

    public record AuthUser(
            String id,
            String email,
            String role,
            String type) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            AuthUser user) {
    }
}
