package io.cerp.identity.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    public static record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank String name,
            String password) {
    }

    public static record UserResponse(
            UUID id,
            String email,
            String name,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public static record UpdateProfileRequest(
            String name) {
    }

    public static record UserProfileResponse(
            UUID id,
            String email,
            String name,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    private static UserResponse toResponse(UserAccount u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getStatus(), u.getCreatedAt(), u.getUpdatedAt());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest body) {
        UserAccount created;
        if (body.password() != null && !body.password().isBlank()) {
            created = userService.createWithPassword(body.email(), body.name(), body.password());
        } else {
            created = userService.create(body.email(), body.name());
        }
        return toResponse(created);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        Optional<UserAccount> u = userService.findById(id);
        return u.map(UserController::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @GetMapping
    public Object get(@RequestParam(value = "email", required = false) String email) {
        if (email != null && !email.isBlank()) {
            return userService.findByEmail(email)
                .map(UserController::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found by email: " + email));
        }
        List<UserAccount> all = userService.findAll();
        return all.stream().map(UserController::toResponse).toList();
    }

    /**
     * Get the current user's profile by extracting user ID from Authorization header.
     */
    @GetMapping("/profile")
    public UserProfileResponse getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = extractUserIdFromToken(authHeader);

        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("No authorization header");
        }

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid user ID format");
        }

        UserAccount user = userService.findById(userUuid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userUuid));

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt());
    }

    /**
     * Update the current user's profile.
     */
    @PatchMapping("/profile")
    public UserProfileResponse updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateProfileRequest update) {
        String userId = extractUserIdFromToken(authHeader);

        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("No authorization header");
        }

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid user ID format");
        }

        UserAccount user = userService.findById(userUuid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userUuid));

        if (update.name() != null && !update.name().isBlank()) {
            user.setName(update.name());
            userService.save(user);
        }

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt());
    }

    /**
     * Extract user ID from Bearer JWT token payload. Looks for "sub", "userId", or "id" claims in the JWT payload.
     */
    private String extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = parts[1];
            String base64 = payload.replace('-', '+').replace('_', '/');
            // Handle padding
            while (base64.length() % 4 != 0) {
                base64 += "=";
            }
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            // Check common JWT claim names for user ID
            for (String fieldName : new String[] { "sub", "userId", "id", "user_id" }) {
                if (node.has(fieldName)) {
                    return node.get(fieldName).asText();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) {
            super(msg);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String msg) {
            super(msg);
        }
    }
}
