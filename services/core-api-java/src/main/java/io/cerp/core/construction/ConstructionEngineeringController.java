package io.cerp.core.construction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/work-orders")
public class ConstructionEngineeringController {

    private final ConstructionEngineeringService service;

    public ConstructionEngineeringController(ConstructionEngineeringService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkOrderResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public WorkOrderResponse findOne(@PathVariable String id) {
        return service.findOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderResponse issue(@Valid @RequestBody CreateWorkOrderRequest req) {
        return service.issue(req);
    }

    @PatchMapping("/{id}/complete")
    public WorkOrderResponse complete(@PathVariable String id) {
        return service.complete(id);
    }
}

/**
 * In-memory implementation mirroring NestJS construction-engineering endpoints. Validates contractor via a minimal Party Registry stub until partner-system-java is
 * wired.
 */
@Service
class ConstructionEngineeringService {

    private final Map<String, WorkOrderEntity> store = new ConcurrentHashMap<>();
    private final PartyRegistryGateway partyRegistry = new InMemoryPartyRegistry();

    public List<WorkOrderResponse> findAll() {
        return store.values().stream()
            .sorted(Comparator.comparing(WorkOrderEntity::issuedAt).reversed())
            .map(ConstructionEngineeringService::toResponse)
            .collect(Collectors.toList());
    }

    public WorkOrderResponse findOne(String id) {
        WorkOrderEntity e = store.get(id);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order " + id + " not found");
        }
        return toResponse(e);
    }

    public WorkOrderResponse issue(CreateWorkOrderRequest req) {
        // projectId must be a UUID
        if (!isUuid(req.projectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid projectId");
        }

        PartyRegistryEntry contractor = partyRegistry.findById(req.contractorPartnerId());
        if (contractor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contractor not found in the partner directory");
        }
        if (!"active".equals(contractor.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contractor is not active");
        }

        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        WorkOrderEntity entity = new WorkOrderEntity(
            id,
            req.projectId(),
            req.title(),
            // optional description defaults to null
            req.description() != null ? req.description() : null,
            contractor.id(),
            contractor.name(),
            "issued",
            now,
            null,
            now);
        store.put(id, entity);
        return toResponse(entity);
    }

    public WorkOrderResponse complete(String id) {
        WorkOrderEntity existing = store.get(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order " + id + " not found");
        }
        if ("completed".equals(existing.status())) {
            return toResponse(existing); // idempotent
        }
        Instant now = Instant.now();
        WorkOrderEntity updated = new WorkOrderEntity(
            existing.id(),
            existing.projectId(),
            existing.title(),
            existing.description(),
            existing.contractorPartnerId(),
            existing.contractorPartnerName(),
            "completed",
            existing.issuedAt(),
            now,
            now);
        store.put(id, updated);
        return toResponse(updated);
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static WorkOrderResponse toResponse(WorkOrderEntity e) {
        return new WorkOrderResponse(
            e.id(),
            e.projectId(),
            e.title(),
            e.description(),
            e.contractorPartnerId(),
            e.contractorPartnerName(),
            e.status(),
            DateTimeFormatter.ISO_INSTANT.format(e.issuedAt()),
            e.completedAt() != null ? DateTimeFormatter.ISO_INSTANT.format(e.completedAt()) : null,
            DateTimeFormatter.ISO_INSTANT.format(e.updatedAt()));
    }
}

// DTOs mirror packages/shared-types/src/construction-engineering.ts
record CreateWorkOrderRequest(
        @NotBlank String projectId,
        @NotBlank @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        @NotBlank String contractorPartnerId) {
}

// Response mirrors WorkOrder interface
record WorkOrderResponse(
        String id,
        String projectId,
        String title,
        String description,
        String contractorPartnerId,
        String contractorPartnerName,
        String status, // "issued" | "completed"
        String issuedAt,
        String completedAt, // nullable
        String updatedAt) {
}

// Internal entity
record WorkOrderEntity(
        String id,
        String projectId,
        String title,
        String description, // nullable
        String contractorPartnerId,
        String contractorPartnerName,
        String status, // "issued" | "completed"
        Instant issuedAt,
        Instant completedAt, // nullable
        Instant updatedAt) {
}

// Minimal Party Registry gateway and stub implementation
interface PartyRegistryGateway {
    PartyRegistryEntry findById(String id);
}

record PartyRegistryEntry(
        String id,
        String name,
        String status // "active" | "inactive"
) {
}

class InMemoryPartyRegistry implements PartyRegistryGateway {
    private final Map<String, PartyRegistryEntry> partners = new HashMap<>();

    InMemoryPartyRegistry() {
        partners.put("fake-partner-active", new PartyRegistryEntry("fake-partner-active", "Fake Partner Active", "active"));
        partners.put("fake-partner-inactive", new PartyRegistryEntry("fake-partner-inactive", "Fake Partner Inactive", "inactive"));
    }

    @Override
    public PartyRegistryEntry findById(String id) {
        return partners.get(id);
    }
}
