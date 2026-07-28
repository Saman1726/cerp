package io.cerp.identity.api;

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

    public AuthController(UserService userService,
            PasswordEncoder passwordEncoder,
            ResourceAccessService resourceAccessService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.resourceAccessService = resourceAccessService;
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
            roles,
            resourceAccess);

        // Get user's tenant ID from membership; if none but user has vendor role, default to vendor tenant
        UUID tenantId = userService.getUserTenantId(user.getId()).orElse(null);
        if (tenantId == null && roles.contains("vendor")) {
            tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        }

        // Issue a simple dev token (unsigned JWT for now, will be replaced with Keycloak tokens later)
        long nowSec = System.currentTimeMillis() / 1000;
        String accessToken = makeDevJwt(user.getId().toString(), roles, tenantId, nowSec + 60L * 30L); // 30 min
        String refreshToken = makeDevJwt(user.getId().toString(), roles, tenantId, nowSec + 60L * 60L * 24L * 7L); // 7 days

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, userInfo));
    }

    // ----- Helper: simple unsigned JWT maker for dev -----

    private static String makeDevJwt(String sub, List<String> roles, UUID tenantId, long expSeconds) {
        String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String roleStr = String.join(",", roles);
        String tenantIdJson = tenantId != null ? ",\"tenantId\":\"" + tenantId.toString() + "\"" : "";
        String payload =
                base64Url(("{\"sub\":\"" + sub + "\",\"roles\":\"" + roleStr + "\"" + tenantIdJson + ",\"exp\":" + expSeconds + "}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return header + "." + payload + ".dev";
    }

    private static String base64Url(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    // ----- DTOs -----

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            User user) {

        public record User(
                String id,
                String email,
                String name,
                List<String> roles,
                List<Map<String, Object>> resourceAccess) {
        }
    }
}
