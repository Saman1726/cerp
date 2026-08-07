package io.cerp.identity.api;

import io.cerp.identity.client.KeycloakAdminClient;
import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.service.ResourceAccessService;
import io.cerp.identity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Auth endpoints for the web app. - POST /auth/login accepts email + password - Returns a token payload with user info and roles
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ResourceAccessService resourceAccessService;
    private final KeycloakAdminClient keycloakAdminClient;

    public AuthController(UserService userService,
            PasswordEncoder passwordEncoder,
            ResourceAccessService resourceAccessService,
            KeycloakAdminClient keycloakAdminClient) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.resourceAccessService = resourceAccessService;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        // Find user by username (name) first, then by email
        UserAccount user = userService.findByNameForAuth(req.username())
            .or(() -> userService.findByEmailForAuth(req.username()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        // Check if user is active
        if (!"active".equals(user.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Account is not active");
        }

        // Verify password
        if (!userService.verifyPassword(user, req.password())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Get user's roles from memberships
        List<String> roles = userService.getUserRoles(user.getId());

        // Get user's resource-level access
        List<Map<String, Object>> resourceAccess = resourceAccessService.getUserAccessInfo(user.getId());

        // Build response
        LoginResponse.User userInfo = new LoginResponse.User(
            user.getId().toString(),
            user.getEmail(),
            user.getName(),
            user.getType().name(),
            roles,
            resourceAccess);

        // Issue a real, signed Keycloak token for this user (Direct Access Grant) - the
        // local bcrypt check above already verified the password; this exchanges it for
        // a token carrying a real "sub" claim (the user's own id) that downstream
        // services can validate and trust, instead of a hand-rolled unsigned JWT.
        KeycloakAdminClient.TokenResponse token = keycloakAdminClient.getUserToken(user.getEmail(), req.password());

        return ResponseEntity.ok(new LoginResponse(token.accessToken(), token.refreshToken(), userInfo));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshRequest req) {
        KeycloakAdminClient.TokenResponse token;
        try {
            token = keycloakAdminClient.refreshToken(req.refreshToken());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        // internal_user_id/email are present on every Keycloak token issued for our realm
        // (see KeycloakBootstrap/KeycloakAdminClient) without another round-trip to the DB;
        // roles/resourceAccess aren't recoverable from a refresh token alone, so they're left
        // empty here - an accepted limitation. Deliberately read "internal_user_id", not the
        // token's own "sub" - Keycloak's internal id is not our app's user id (see
        // KeycloakAdminClient's javadoc).
        String payloadJson = decodeJwtPayload(token.accessToken());
        String internalUserId = payloadJson != null ? extractJsonString(payloadJson, "internal_user_id") : null;
        String email = payloadJson != null ? extractJsonString(payloadJson, "email") : null;

        LoginResponse.User userInfo = new LoginResponse.User(
            internalUserId, email, null, null, List.of(), List.of());

        return ResponseEntity.ok(new LoginResponse(token.accessToken(), token.refreshToken(), userInfo));
    }

    // ----- Helper: read-only claim extraction from a JWT payload -----

    private static String decodeJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2)
                return null;
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonString(String json, String key) {
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

    // ----- DTOs -----

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            User user) {

        public record User(
                String id,
                String email,
                String name,
                String type,
                List<String> roles,
                List<Map<String, Object>> resourceAccess) {
        }
    }
}
