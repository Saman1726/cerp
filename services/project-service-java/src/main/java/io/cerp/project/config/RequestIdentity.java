package io.cerp.project.config;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the caller's identity-service user id out of a validated Keycloak JWT (see SecurityConfig's oauth2ResourceServer). Deliberately reads the "internal_user_id"
 * claim, not the token's own "sub" - Keycloak assigns its own internal id on user creation and does not honor identity-service's, so "sub" is not our app's user id (see
 * identity-service's KeycloakAdminClient for the full explanation). Every token issued through identity-service's login carries this claim; its absence means the token
 * didn't come from our realm/client as expected.
 */
public final class RequestIdentity {

    private RequestIdentity() {
    }

    public static UUID creatorId(Jwt jwt) {
        String raw = jwt.getClaimAsString("internal_user_id");
        if (raw == null) {
            throw new IllegalStateException("JWT is missing the internal_user_id claim");
        }
        return UUID.fromString(raw);
    }
}
