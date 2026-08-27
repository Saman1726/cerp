package io.cerp.identity.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * One-time, idempotent bootstrap of the "cerp" Keycloak realm and its "cerp-identity-service" client, run at identity-service startup (see SeedDataConfig, which runs this
 * before syncing any users). Uses master-realm admin credentials via Keycloak's built-in "admin-cli" client - the standard way to bootstrap a fresh Keycloak instance
 * before any custom client exists. Declarative realm-import (--import-realm) was tried first but hit what looks like a bug in this Keycloak version
 * (ExportImportManager NPEs with "directory not found" even with the import directory correctly mounted) - this achieves the same end state via the Admin REST API instead.
 */
@Component
public class KeycloakBootstrap {

    private static final Logger log = LoggerFactory.getLogger(KeycloakBootstrap.class);
    private static final String REALM = "cerp";
    private static final String CLIENT_ID = "cerp-identity-service";

    private final RestClient restClient;
    private final String adminUsername;
    private final String adminPassword;
    private final String clientSecret;

    public KeycloakBootstrap(
            @Value("${cerp.keycloak.url}") String keycloakUrl,
            @Value("${cerp.keycloak.admin-username:admin}") String adminUsername,
            @Value("${cerp.keycloak.admin-password:admin}") String adminPassword,
            @Value("${cerp.keycloak.client-secret}") String clientSecret) {
        this.restClient = RestClient.builder().baseUrl(keycloakUrl).build();
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.clientSecret = clientSecret;
    }

    public void ensureRealmAndClientExist() {
        String adminToken = getMasterAdminToken();
        ensureRealmExists(adminToken);
        String clientInternalId = ensureClientExists(adminToken);
        ensureServiceAccountHasManageUsersRole(adminToken, clientInternalId);
        ensureInternalUserIdMapperExists(adminToken, clientInternalId);
        configureUserProfile(adminToken);
        log.info("Keycloak realm '{}' and client '{}' are ready", REALM, CLIENT_ID);
    }

    /**
     * Two fixes to the realm's declarative User Profile, both discovered empirically:
     *
     * 1. cerp's UserAccount has a single "name" field (a person's name for User/Vendor
     *    accounts, a company name like "Steel Supply Co" for Company accounts) - there is
     *    no firstName/lastName split to populate. Keycloak marks both required by default,
     *    which dynamically fails Direct Access Grant logins with "Account is not fully set
     *    up" for any user missing them. Relax both to optional.
     * 2. Keycloak's declarative User Profile silently drops any user attribute NOT declared
     *    in this schema - KeycloakAdminClient.upsertUser's internal_user_id/account_type/
     *    tenant_id attributes were being sent but discarded until declared here.
     */
    private void configureUserProfile(String adminToken) {
        Map<String, Object> profile = restClient.get()
            .uri("/admin/realms/{realm}/users/profile", REALM)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {
            });
        if (profile == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attributes = (List<Map<String, Object>>) profile.get("attributes");
        if (attributes == null) {
            return;
        }
        for (Map<String, Object> attribute : attributes) {
            if ("firstName".equals(attribute.get("name")) || "lastName".equals(attribute.get("name"))) {
                attribute.remove("required");
            }
        }
        for (String customAttribute : List.of("internal_user_id", "account_type", "tenant_id")) {
            boolean alreadyDeclared = attributes.stream().anyMatch(a -> customAttribute.equals(a.get("name")));
            if (!alreadyDeclared) {
                Map<String, Object> definition = new HashMap<>();
                definition.put("name", customAttribute);
                definition.put("permissions", Map.of("view", List.of("admin"), "edit", List.of("admin")));
                definition.put("multivalued", false);
                attributes.add(definition);
            }
        }

        restClient.put()
            .uri("/admin/realms/{realm}/users/profile", REALM)
            .header("Authorization", "Bearer " + adminToken)
            .body(profile)
            .retrieve()
            .toBodilessEntity();
    }

    /**
     * Exposes the "internal_user_id" user attribute as a same-named token claim. Downstream
     * services must read this claim for the caller's identity-service id - Keycloak assigns
     * its own "sub", which does not (and cannot be made to) match it. See KeycloakAdminClient.
     */
    private void ensureInternalUserIdMapperExists(String adminToken, String clientInternalId) {
        List<Map<String, Object>> existingMappers = restClient.get()
            .uri("/admin/realms/{realm}/clients/{id}/protocol-mappers/models", REALM, clientInternalId)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
            });
        boolean alreadyExists = existingMappers != null
            && existingMappers.stream().anyMatch(m -> "internal_user_id".equals(m.get("name")));
        if (alreadyExists) {
            return;
        }

        Map<String, Object> mapper = new HashMap<>();
        mapper.put("name", "internal_user_id");
        mapper.put("protocol", "openid-connect");
        mapper.put("protocolMapper", "oidc-usermodel-attribute-mapper");
        mapper.put("config", Map.of(
            "user.attribute", "internal_user_id",
            "claim.name", "internal_user_id",
            "jsonType.label", "String",
            "id.token.claim", "true",
            "access.token.claim", "true",
            "userinfo.token.claim", "true"));

        restClient.post()
            .uri("/admin/realms/{realm}/clients/{id}/protocol-mappers/models", REALM, clientInternalId)
            .header("Authorization", "Bearer " + adminToken)
            .body(mapper)
            .retrieve()
            .toBodilessEntity();
        log.info("Created 'internal_user_id' protocol mapper on client '{}'", CLIENT_ID);
    }

    /**
     * Obtain a Keycloak master-realm admin token using the built-in "admin-cli" client.
     * Retries with exponential backoff for up to 30 seconds to tolerate Keycloak
     * startup latency (Keycloak 24 takes ~30-45s in dev mode, so a simple
     * docker-compose "depends_on: service_started" is insufficient).
     */
    private String getMasterAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        int maxRetries = 15;
        long delayMs = 2000; // start at 2s, double each retry (~30s total window)
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Attempting to obtain Keycloak master admin token (attempt {}/{})", attempt, maxRetries);
                Map<String, Object> body = restClient.post()
                    .uri("/realms/master/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
                if (body == null || body.get("access_token") == null) {
                    throw new IllegalStateException("Could not obtain a Keycloak master admin token");
                }
                log.info("Successfully obtained Keycloak master admin token after {} attempt(s)", attempt);
                return (String) body.get("access_token");
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("Failed to obtain Keycloak master admin token (attempt {}/{}): {}. Retrying in {}ms...",
                        attempt, maxRetries, e.getMessage(), delayMs);
                    try { Thread.sleep(delayMs); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while waiting for Keycloak", ie);
                    }
                    delayMs = Math.min(delayMs * 2, 5000); // cap at 5s
                }
            }
        }
        throw new IllegalStateException("Could not obtain Keycloak master admin token after " + maxRetries + " attempts", lastException);
    }

    private void ensureRealmExists(String adminToken) {
        try {
            restClient.get()
                .uri("/admin/realms/{realm}", REALM)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            restClient.post()
                .uri("/admin/realms")
                .header("Authorization", "Bearer " + adminToken)
                .body(Map.of("realm", REALM, "enabled", true, "sslRequired", "none"))
                .retrieve()
                .toBodilessEntity();
            log.info("Created Keycloak realm '{}'", REALM);
        }
    }

    /**
     * Returns the client's internal (UUID) id - distinct from its clientId - needed for subsequent role-mapping calls.
     */
    private String ensureClientExists(String adminToken) {
        List<Map<String, Object>> existing = findClientsByClientId(adminToken, CLIENT_ID);
        if (!existing.isEmpty()) {
            return (String) existing.get(0).get("id");
        }

        Map<String, Object> newClient = new HashMap<>();
        newClient.put("clientId", CLIENT_ID);
        newClient.put("name", "CERP Identity Service");
        newClient.put("enabled", true);
        newClient.put("protocol", "openid-connect");
        newClient.put("publicClient", false);
        newClient.put("secret", clientSecret);
        newClient.put("standardFlowEnabled", false);
        newClient.put("implicitFlowEnabled", false);
        newClient.put("directAccessGrantsEnabled", true);
        newClient.put("serviceAccountsEnabled", true);

        restClient.post()
            .uri("/admin/realms/{realm}/clients", REALM)
            .header("Authorization", "Bearer " + adminToken)
            .body(newClient)
            .retrieve()
            .toBodilessEntity();
        log.info("Created Keycloak client '{}' in realm '{}'", CLIENT_ID, REALM);

        return findClientsByClientId(adminToken, CLIENT_ID).get(0).get("id").toString();
    }

    private void ensureServiceAccountHasManageUsersRole(String adminToken, String clientInternalId) {
        Map<String, Object> serviceAccountUser = restClient.get()
            .uri("/admin/realms/{realm}/clients/{id}/service-account-user", REALM, clientInternalId)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {
            });
        String serviceAccountUserId = serviceAccountUser != null ? (String) serviceAccountUser.get("id") : null;
        if (serviceAccountUserId == null) {
            throw new IllegalStateException("Could not resolve service account user for client " + CLIENT_ID);
        }

        String realmManagementId = findClientsByClientId(adminToken, "realm-management").get(0).get("id").toString();

        List<Map<String, Object>> existingRoles = restClient.get()
            .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientId}", REALM, serviceAccountUserId, realmManagementId)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
            });
        boolean alreadyGranted = existingRoles != null && existingRoles.stream().anyMatch(r -> "manage-users".equals(r.get("name")));
        if (alreadyGranted) {
            return;
        }

        Map<String, Object> manageUsersRole = restClient.get()
            .uri("/admin/realms/{realm}/clients/{id}/roles/manage-users", REALM, realmManagementId)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {
            });

        restClient.post()
            .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientId}", REALM, serviceAccountUserId, realmManagementId)
            .header("Authorization", "Bearer " + adminToken)
            .body(List.of(manageUsersRole))
            .retrieve()
            .toBodilessEntity();
        log.info("Granted 'manage-users' to {}'s service account", CLIENT_ID);
    }

    private List<Map<String, Object>> findClientsByClientId(String adminToken, String clientId) {
        List<Map<String, Object>> result = restClient.get()
            .uri("/admin/realms/{realm}/clients?clientId={clientId}", REALM, clientId)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
            });
        return result != null ? result : List.of();
    }
}
