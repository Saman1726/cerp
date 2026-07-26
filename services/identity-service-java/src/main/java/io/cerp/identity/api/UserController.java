package io.cerp.identity.api;

import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
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

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) {
            super(msg);
        }
    }
}
