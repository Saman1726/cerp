package io.cerp.core.partyregistry;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Mirrors packages/core-api/src/party-registry/partners.controller.ts Exposes GET /partners and proxies to the external Partner System via PartnerSystemClient.
 */
@RestController
@RequestMapping("/partners")
public class PartnersController {

    private final PartnerSystemClient client;

    public PartnersController(PartnerSystemClient client) {
        this.client = client;
    }

    @GetMapping
    public List<PartyRegistryEntry> findAll() {
        return client.findAll();
    }

    @GetMapping("/{id}")
    public PartyRegistryEntry findOne(@PathVariable String id) {
        PartyRegistryEntry entry = client.findById(id);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner " + id + " not found");
        }
        return entry;
    }
}
