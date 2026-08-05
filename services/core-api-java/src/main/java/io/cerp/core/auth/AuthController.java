package io.cerp.core.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

    public AuthController(
            RestTemplate restTemplate,
            @Value("${cerp.identity-service.url:http://localhost:8090}") String identityServiceUrl) {
        this.restTemplate = restTemplate;
        this.identityServiceUrl = identityServiceUrl;
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

                return new LoginResponse(accessToken, refreshToken, new AuthUser(identityUser.id(), identityUser.email(), primaryRole));
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
        // Very lightweight "decode": extract sub/role from the payload of our dev JWT
        String payloadJson = decodeDevJwtPayload(req.refreshToken());
        if (payloadJson == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        String sub = extractJsonString(payloadJson, "sub");
        // Support both old ("role") and new ("roles") token formats
        String role = extractJsonString(payloadJson, "role");
        if (role == null) role = extractJsonString(payloadJson, "roles");
        String tenantId = extractJsonString(payloadJson, "tenantId");
        if (sub == null || role == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        long nowSec = Instant.now().getEpochSecond();
        String newAccess = makeDevJwt(sub, role, tenantId, nowSec + 60L * 30L);
        String newRefresh = makeDevJwt(sub, role, tenantId, nowSec + 60L * 60L * 24L * 7L);

        AuthUser user = new AuthUser(sub, sub, role);
        return new LoginResponse(newAccess, newRefresh, user);
    }

    // ----- Helper: extremely simple unsigned JWT maker/decoder for dev -----

    private static String makeDevJwt(String sub, String role, String tenantId, long expSeconds) {
        String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String tenantPart = tenantId != null ? ",\"tenantId\":\"" + tenantId + "\"" : "";
        String payload = base64Url(("{\"sub\":\"" + sub + "\",\"role\":\"" + role + "\"" + tenantPart + ",\"exp\":" + expSeconds + "}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".dev";
    }

    private static String decodeDevJwtPayload(String token) {
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

    private static String base64Url(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
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
            String role) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            AuthUser user) {
    }
}
