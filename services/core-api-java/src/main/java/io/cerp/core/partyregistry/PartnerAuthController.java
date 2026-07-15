package io.cerp.core.partyregistry;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Mirrors packages/core-api/src/party-registry/partner-auth.controller.ts Exposes POST /auth/partner-login and proxies to the external Partner System via
 * PartnerSystemClient.
 */
@RestController
@RequestMapping("/auth")
public class PartnerAuthController {

    private final PartnerSystemClient client;

    public PartnerAuthController(PartnerSystemClient client) {
        this.client = client;
    }

    @PostMapping("/partner-login")
    @ResponseStatus(HttpStatus.OK)
    public PartnerLoginResult partnerLogin(@Valid @RequestBody PartnerLoginRequest req) {
        return client.login(req);
    }
}
