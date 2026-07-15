package io.cerp.core.partyregistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * HTTP client to the external Partner System service. Base URL comes from property: partner-system.base-url (env PARTNER_SYSTEM_URL)
 */
@Component
public class PartnerSystemClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;

    public PartnerSystemClient(@Value("${partner-system.base-url:http://localhost:3020}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<PartyRegistryEntry> findAll() {
        try {
            ResponseEntity<List<PartyRegistryEntry>> response = restTemplate.exchange(
                baseUrl + "/partners",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PartyRegistryEntry>>() {
                });
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw translate(ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Partner system is unreachable");
        }
    }

    public PartyRegistryEntry findById(String id) {
        try {
            return restTemplate.getForObject(baseUrl + "/partners/{id}", PartyRegistryEntry.class, id);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw translate(ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Partner system is unreachable");
        }
    }

    public PartnerLoginResult login(PartnerLoginRequest req) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PartnerLoginRequest> entity = new HttpEntity<>(req, headers);

            ResponseEntity<PartnerLoginResult> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                entity,
                PartnerLoginResult.class);

            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid partner code, username or password");
            }
            throw translate(ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Partner system is unreachable");
        }
    }

    private ResponseStatusException translate(HttpStatusCodeException ex) {
        int code = ex.getStatusCode().value();
        if (code >= 500) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Partner system returned an unexpected error");
        }
        if (code == 404) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Partner system returned an unexpected error");
    }
}
