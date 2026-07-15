package io.cerp.partner.partners;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Mirrors services/partner-system partners endpoints: - GET /partners - GET /partners/{id} Uses in-memory data for migration bootstrap.
 */
@RestController
@RequestMapping("/partners")
public class PartnersController {

    private final PartnersService service;

    public PartnersController(PartnersService service) {
        this.service = service;
    }

    @GetMapping
    public List<PartyRegistryEntry> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PartyRegistryEntry findOne(@PathVariable String id) {
        return service.findOne(id);
    }
}

@Service
class PartnersService {
    private final Map<String, PartyRegistryEntry> partners = new ConcurrentHashMap<>();

    PartnersService() {
        // Seed some sample partners
        add(new PartyRegistryEntry("p-100", "ACME", "ACME Contractors GmbH", "contractor", "contact@acme.example", "active"));
        add(new PartyRegistryEntry("p-200", "MEGA", "MegaBuild AG", "contractor", "info@megabuild.example", "inactive"));
        add(new PartyRegistryEntry("p-300", "CITY", "City Council", "authority", "procurement@city.example", "active"));
    }

    private void add(PartyRegistryEntry p) {
        partners.put(p.id(), p);
    }

    List<PartyRegistryEntry> findAll() {
        return partners.values().stream()
            .sorted(Comparator.comparing(PartyRegistryEntry::name))
            .collect(Collectors.toList());
    }

    PartyRegistryEntry findOne(String id) {
        PartyRegistryEntry p = partners.get(id);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner " + id + " not found");
        }
        return p;
    }
}
