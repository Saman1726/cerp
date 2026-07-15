package io.cerp.partner.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors services/partner-system auth endpoints: - POST /auth/login Uses in-memory data for migration bootstrap.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public PartnerLoginResult login(@Valid @RequestBody PartnerLoginRequest req) {
        return service.login(req);
    }
}

@Service
class AuthService {

    private final Map<String, Partner> partnersByCode = new HashMap<>();
    private final Map<String, PartnerUser> users = new HashMap<>(); // key: partnerCode + ":" + username

    AuthService() {
        // Seed partners
        var acme = new Partner("p-100", "ACME", "ACME Contractors GmbH");
        var mega = new Partner("p-200", "MEGA", "MegaBuild AG");
        partnersByCode.put(acme.code(), acme);
        partnersByCode.put(mega.code(), mega);

        // Seed users (plaintext passwords only for bootstrap/migration)
        users.put(key("ACME", "john"), new PartnerUser("john", "secret", "admin", "ACME"));
        users.put(key("ACME", "jane"), new PartnerUser("jane", "secret", "user", "ACME"));
        users.put(key("MEGA", "mario"), new PartnerUser("mario", "secret", "user", "MEGA"));
    }

    public PartnerLoginResult login(PartnerLoginRequest req) {
        Partner partner = partnersByCode.get(req.partnerCode());
        if (partner == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid partner code or credentials");
        }

        PartnerUser user = users.get(key(req.partnerCode(), req.username()));
        if (user == null || !user.password().equals(req.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid partner code or credentials");
        }

        return new PartnerLoginResult(
            partner.id(),
            partner.code(),
            partner.name(),
            user.username(),
            user.role());
    }

    private static String key(String partnerCode, String username) {
        return partnerCode + ":" + username;
    }
}

// Request DTO mirrors services/partner-system/src/auth/dto/partner-login.dto.ts
record PartnerLoginRequest(
        @NotBlank String partnerCode,
        @NotBlank String username,
        @NotBlank String password) {
}

// Response mirrors packages/core-api/src/party-registry/party-registry.types.ts PartnerLoginResult
record PartnerLoginResult(
        String partnerId,
        String partnerCode,
        String partnerName,
        String username,
        String role) {
}

// Internal seed types
record Partner(
        String id,
        String code,
        String name) {
}

record PartnerUser(
        String username,
        String password,
        String role,
        String partnerCode) {
}
