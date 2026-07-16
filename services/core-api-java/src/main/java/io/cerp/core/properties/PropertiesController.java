package io.cerp.core.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Profile("!dev")
@RestController
@RequestMapping("/properties")
public class PropertiesController {

    private final PropertiesService service;

    public PropertiesController(PropertiesService service) {
        this.service = service;
    }

    @GetMapping
    public List<PropertyResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PropertyResponse findOne(@PathVariable String id) {
        return service.findOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@Valid @RequestBody CreatePropertyRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}")
    public PropertyResponse update(@PathVariable String id, @Valid @RequestBody UpdatePropertyRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}

/**
 * Package-private service and DTOs to keep everything in a single file for the initial migration step. We use an in-memory store to enable endpoints quickly. Persistence
 * can be swapped to JPA later.
 */
@Profile("!dev")
@Service
class PropertiesService {
    private final Map<String, PropertyEntity> store = new ConcurrentHashMap<>();

    public List<PropertyResponse> findAll() {
        return store.values().stream()
            .sorted(Comparator.comparing(PropertyEntity::createdAt).reversed())
            .map(PropertiesService::toResponse)
            .collect(Collectors.toList());
    }

    public PropertyResponse findOne(String id) {
        PropertyEntity entity = store.get(id);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + id + " not found");
        }
        return toResponse(entity);
    }

    public PropertyResponse create(CreatePropertyRequest req) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        PropertyEntity entity = new PropertyEntity(
            id,
            req.name(),
            req.address(),
            now,
            now);
        store.put(id, entity);
        return toResponse(entity);
    }

    public PropertyResponse update(String id, UpdatePropertyRequest req) {
        PropertyEntity entity = store.get(id);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + id + " not found");
        }
        String newName = req.name() != null ? req.name() : entity.name();
        String newAddress = req.address() != null ? req.address() : entity.address();
        PropertyEntity updated = new PropertyEntity(
            entity.id(),
            newName,
            newAddress,
            entity.createdAt(),
            Instant.now());
        store.put(id, updated);
        return toResponse(updated);
    }

    public void delete(String id) {
        if (store.remove(id) == null) {
            // Match DELETE semantics: 404 if not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + id + " not found");
        }
    }

    private static PropertyResponse toResponse(PropertyEntity e) {
        return new PropertyResponse(
            e.id(),
            e.name(),
            e.address(),
            DateTimeFormatter.ISO_INSTANT.format(e.createdAt()),
            DateTimeFormatter.ISO_INSTANT.format(e.updatedAt()));
    }
}

// Shared types: mirror packages/shared-types/src/properties.ts
record PropertyResponse(
        String id,
        String name,
        String address,
        String createdAt,
        String updatedAt) {
}

// DTOs: mirror CreatePropertyRequest and Partial(UpdatePropertyRequest)
record CreatePropertyRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotBlank @Size(min = 1, max = 500) String address) {
}

record UpdatePropertyRequest(
        @Size(min = 1, max = 200) String name,
        @Size(min = 1, max = 500) String address) {
}

// Internal entity for in-memory storage
record PropertyEntity(
        String id,
        String name,
        String address,
        Instant createdAt,
        Instant updatedAt) {
}
