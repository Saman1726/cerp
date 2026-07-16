package io.cerp.core.constructionsites;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

/**
 * Construction Sites API
 *
 * Domain model: - ConstructionSite: 1..n Buildings - Building: 1..n Floors, computed usableArea/totalArea = sum of floors - Floor: 0..n Units, attributes: type,
 * usableArea, totalArea - Unit: leaf node (no defined attributes beyond id/label at this stage)
 *
 * Persistence: in-memory store for initial implementation; swap with JPA later.
 */
@RestController
@RequestMapping("/construction-sites")
public class ConstructionSitesController {

    private final ConstructionSitesService service;

    public ConstructionSitesController(ConstructionSitesService service) {
        this.service = service;
    }

    // ----- Sites -----

    @GetMapping
    public List<ConstructionSiteResponse> listSites() {
        return service.listSites();
    }

    @GetMapping("/{siteId}")
    public ConstructionSiteResponse getSite(@PathVariable String siteId) {
        return service.getSite(siteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConstructionSiteResponse createSite(@Valid @RequestBody CreateConstructionSiteRequest req) {
        return service.createSite(req);
    }

    @PatchMapping("/{siteId}")
    public ConstructionSiteResponse updateSite(@PathVariable String siteId, @Valid @RequestBody UpdateConstructionSiteRequest req) {
        return service.updateSite(siteId, req);
    }

    @DeleteMapping("/{siteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSite(@PathVariable String siteId) {
        service.deleteSite(siteId);
    }

    // ----- Buildings -----

    @PostMapping("/{siteId}/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public ConstructionSiteResponse addBuilding(@PathVariable String siteId, @Valid @RequestBody CreateBuildingRequest req) {
        return service.addBuilding(siteId, req);
    }

    @PatchMapping("/{siteId}/buildings/{buildingId}")
    public ConstructionSiteResponse updateBuilding(@PathVariable String siteId, @PathVariable String buildingId, @Valid @RequestBody UpdateBuildingRequest req) {
        return service.updateBuilding(siteId, buildingId, req);
    }

    @DeleteMapping("/{siteId}/buildings/{buildingId}")
    public ConstructionSiteResponse deleteBuilding(@PathVariable String siteId, @PathVariable String buildingId) {
        return service.deleteBuilding(siteId, buildingId);
    }

    // ----- Floors -----

    @PostMapping("/{siteId}/buildings/{buildingId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public ConstructionSiteResponse addFloor(@PathVariable String siteId, @PathVariable String buildingId, @Valid @RequestBody CreateFloorRequest req) {
        return service.addFloor(siteId, buildingId, req);
    }

    @PatchMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}")
    public ConstructionSiteResponse updateFloor(@PathVariable String siteId, @PathVariable String buildingId, @PathVariable String floorId,
            @Valid @RequestBody UpdateFloorRequest req) {
        return service.updateFloor(siteId, buildingId, floorId, req);
    }

    @DeleteMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}")
    public ConstructionSiteResponse deleteFloor(@PathVariable String siteId, @PathVariable String buildingId, @PathVariable String floorId) {
        return service.deleteFloor(siteId, buildingId, floorId);
    }

    // ----- Units -----

    @PostMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}/units")
    @ResponseStatus(HttpStatus.CREATED)
    public ConstructionSiteResponse addUnit(@PathVariable String siteId, @PathVariable String buildingId, @PathVariable String floorId,
            @Valid @RequestBody CreateUnitRequest req) {
        return service.addUnit(siteId, buildingId, floorId, req);
    }

    @PatchMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}/units/{unitId}")
    public ConstructionSiteResponse updateUnit(@PathVariable String siteId, @PathVariable String buildingId, @PathVariable String floorId, @PathVariable String unitId,
            @Valid @RequestBody UpdateUnitRequest req) {
        return service.updateUnit(siteId, buildingId, floorId, unitId, req);
    }

    @DeleteMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}/units/{unitId}")
    public ConstructionSiteResponse deleteUnit(@PathVariable String siteId, @PathVariable String buildingId, @PathVariable String floorId, @PathVariable String unitId) {
        return service.deleteUnit(siteId, buildingId, floorId, unitId);
    }
}

/**
 * Service layer (package-private). In-memory store for now.
 */
@Service
class ConstructionSitesService {

    private final Map<String, ConstructionSiteEntity> store = new ConcurrentHashMap<>();

    // ----- Sites -----

    public List<ConstructionSiteResponse> listSites() {
        return store.values().stream()
            .sorted(Comparator.comparing(ConstructionSiteEntity::createdAt).reversed())
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public ConstructionSiteResponse getSite(String siteId) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        return toResponse(site);
    }

    public ConstructionSiteResponse createSite(CreateConstructionSiteRequest req) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        ConstructionSiteEntity site = new ConstructionSiteEntity(
            id,
            req.name(),
            req.address(),
            new ArrayList<>(),
            now,
            now);
        store.put(id, site);
        return toResponse(site);
    }

    public ConstructionSiteResponse updateSite(String siteId, UpdateConstructionSiteRequest req) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        String newName = req.name() != null ? req.name() : site.name();
        String newAddress = req.address() != null ? req.address() : site.address();
        ConstructionSiteEntity updated = new ConstructionSiteEntity(
            site.id(),
            newName,
            newAddress,
            site.buildings(), // reuse list reference
            site.createdAt(),
            Instant.now());
        store.put(siteId, updated);
        return toResponse(updated);
    }

    public void deleteSite(String siteId) {
        if (store.remove(siteId) == null)
            throw notFound("Site", siteId);
    }

    // ----- Buildings -----

    public ConstructionSiteResponse addBuilding(String siteId, CreateBuildingRequest req) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);

        String id = UUID.randomUUID().toString();
        BuildingEntity building = new BuildingEntity(id, req.name(), new ArrayList<>());

        // Enforce at least 1 building per site is possible; create simply appends
        site.buildings().add(building);
        touch(site);
        return toResponse(store.get(site.id()));
    }

    public ConstructionSiteResponse updateBuilding(String siteId, String buildingId, UpdateBuildingRequest req) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        BuildingEntity b = findBuilding(site, buildingId);

        if (req.name() != null) {
            b.setName(req.name());
        }
        touch(site);
        return toResponse(store.get(site.id()));
    }

    public ConstructionSiteResponse deleteBuilding(String siteId, String buildingId) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        // Ensure site keeps at least one building
        BuildingEntity existing = findBuilding(site, buildingId);
        if (site.buildings().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A construction site must have at least one building");
        }

        boolean removed = site.buildings().removeIf(b -> b.id().equals(existing.id()));
        if (!removed)
            throw notFound("Building", buildingId);
        touch(site);
        return toResponse(store.get(site.id()));
    }

    // ----- Floors -----

    public ConstructionSiteResponse addFloor(String siteId, String buildingId, CreateFloorRequest req) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        BuildingEntity b = findBuilding(site, buildingId);

        validateFloorAreas(req.usableArea(), req.totalArea());

        String id = UUID.randomUUID().toString();
        FloorEntity floor = new FloorEntity(id, req.type(), req.usableArea(), req.totalArea(), new ArrayList<>());
        b.floors().add(floor);
        touch(site);
        return toResponse(store.get(site.id()));
    }

    public ConstructionSiteResponse updateFloor(String siteId, String buildingId, String floorId, UpdateFloorRequest req) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        BuildingEntity b = findBuilding(site, buildingId);
        FloorEntity f = findFloor(b, floorId);

        if (req.type() != null)
            f.setType(req.type());
        if (req.usableArea() != null || req.totalArea() != null) {
            double newUsable = req.usableArea() != null ? req.usableArea() : f.usableArea();
            double newTotal = req.totalArea() != null ? req.totalArea() : f.totalArea();
            validateFloorAreas(newUsable, newTotal);
            f.setUsableArea(newUsable);
            f.setTotalArea(newTotal);
        }
        touch(site);
        return toResponse(store.get(site.id()));
    }

    public ConstructionSiteResponse deleteFloor(String siteId, String buildingId, String floorId) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        BuildingEntity b = findBuilding(site, buildingId);
        // Ensure building keeps at least one floor
        FloorEntity f = findFloor(b, floorId);
        if (b.floors().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A building must have at least one floor");
        }

        boolean removed = b.floors().removeIf(fl -> fl.id().equals(f.id()));
        if (!removed)
            throw notFound("Floor", floorId);
        touch(site);
        return toResponse(store.get(site.id()));
    }

    // ----- Units -----

    public ConstructionSiteResponse addUnit(String siteId, String buildingId, String floorId, CreateUnitRequest req) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        BuildingEntity b = findBuilding(site, buildingId);
        FloorEntity f = findFloor(b, floorId);

        String id = UUID.randomUUID().toString();
        UnitEntity unit = new UnitEntity(
            id,
            req.label(),
            req.usableArea(),
            req.bedrooms(),
            req.bathrooms(),
            req.balconies(),
            req.toilets());
        f.units().add(unit);
        touch(site);
        return toResponse(store.get(site.id()));
    }

    public ConstructionSiteResponse updateUnit(String siteId, String buildingId, String floorId, String unitId, UpdateUnitRequest req) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        BuildingEntity b = findBuilding(site, buildingId);
        FloorEntity f = findFloor(b, floorId);
        UnitEntity u = findUnit(f, unitId);

        if (req.label() != null) {
            u.setLabel(req.label());
        }
        if (req.usableArea() != null) {
            if (req.usableArea() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit usableArea must be non-negative");
            }
            u.setUsableArea(req.usableArea());
        }
        if (req.bedrooms() != null) {
            if (req.bedrooms() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit bedrooms must be non-negative");
            u.setBedrooms(req.bedrooms());
        }
        if (req.bathrooms() != null) {
            if (req.bathrooms() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit bathrooms must be non-negative");
            u.setBathrooms(req.bathrooms());
        }
        if (req.balconies() != null) {
            if (req.balconies() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit balconies must be non-negative");
            u.setBalconies(req.balconies());
        }
        if (req.toilets() != null) {
            if (req.toilets() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit toilets must be non-negative");
            u.setToilets(req.toilets());
        }
        touch(site);
        return toResponse(store.get(site.id()));
    }

    public ConstructionSiteResponse deleteUnit(String siteId, String buildingId, String floorId, String unitId) {
        ConstructionSiteEntity site = store.get(siteId);
        if (site == null)
            throw notFound("Site", siteId);
        BuildingEntity b = findBuilding(site, buildingId);
        FloorEntity f = findFloor(b, floorId);

        boolean removed = f.units().removeIf(u -> u.id().equals(unitId));
        if (!removed)
            throw notFound("Unit", unitId);
        touch(site);
        return toResponse(store.get(site.id()));
    }

    // ----- Helpers -----

    private void validateFloorAreas(double usable, double total) {
        if (usable < 0 || total < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Areas must be non-negative");
        }
        if (usable > total) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usable area cannot exceed total area");
        }
    }

    private BuildingEntity findBuilding(ConstructionSiteEntity site, String buildingId) {
        return site.buildings().stream()
            .filter(b -> b.id().equals(buildingId))
            .findFirst()
            .orElseThrow(() -> notFound("Building", buildingId));
    }

    private FloorEntity findFloor(BuildingEntity building, String floorId) {
        return building.floors().stream()
            .filter(f -> f.id().equals(floorId))
            .findFirst()
            .orElseThrow(() -> notFound("Floor", floorId));
    }

    private UnitEntity findUnit(FloorEntity floor, String unitId) {
        return floor.units().stream()
            .filter(u -> u.id().equals(unitId))
            .findFirst()
            .orElseThrow(() -> notFound("Unit", unitId));
    }

    private void touch(ConstructionSiteEntity site) {
        ConstructionSiteEntity updated = new ConstructionSiteEntity(
            site.id(),
            site.name(),
            site.address(),
            site.buildings(),
            site.createdAt(),
            Instant.now());
        store.put(site.id(), updated);
    }

    private ResponseStatusException notFound(String type, String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found");
    }

    // ----- Mapping -----

    private ConstructionSiteResponse toResponse(ConstructionSiteEntity e) {
        List<BuildingResponse> buildings = e.buildings().stream().map(b -> {
            double usableSum = b.floors().stream().mapToDouble(FloorEntity::usableArea).sum();
            double totalSum = b.floors().stream().mapToDouble(FloorEntity::totalArea).sum();

            List<FloorResponse> floors = b.floors().stream().map(f -> new FloorResponse(
                f.id(),
                f.type(),
                f.usableArea(),
                f.totalArea(),
                f.units().stream().map(u -> new UnitResponse(
                    u.id(),
                    u.label(),
                    u.usableArea(),
                    u.bedrooms(),
                    u.bathrooms(),
                    u.balconies(),
                    u.toilets())).collect(Collectors.toList())))
                .collect(Collectors.toList());

            return new BuildingResponse(
                b.id(),
                b.name(),
                usableSum,
                totalSum,
                floors);
        }).collect(Collectors.toList());

        return new ConstructionSiteResponse(
            e.id(),
            e.name(),
            e.address(),
            buildings,
            DateTimeFormatter.ISO_INSTANT.format(e.createdAt()),
            DateTimeFormatter.ISO_INSTANT.format(e.updatedAt()));
    }
}

// ----- DTOs and Responses -----

// Sites
record CreateConstructionSiteRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotBlank @Size(min = 1, max = 500) String address) {
}

record UpdateConstructionSiteRequest(
        @Size(min = 1, max = 200) String name,
        @Size(min = 1, max = 500) String address) {
}

record ConstructionSiteResponse(
        String id,
        String name,
        String address,
        List<BuildingResponse> buildings,
        String createdAt,
        String updatedAt) {
}

// Buildings
record CreateBuildingRequest(
        @NotBlank @Size(min = 1, max = 200) String name) {
}

record UpdateBuildingRequest(
        @Size(min = 1, max = 200) String name) {
}

record BuildingResponse(
        String id,
        String name,
        double usableArea, // sum of floors
        double totalArea, // sum of floors
        List<FloorResponse> floors) {
}

// Floors
record CreateFloorRequest(
        @NotBlank @Size(min = 1, max = 100) String type,
        @NotNull @Min(0) Double usableArea,
        @NotNull @Min(0) Double totalArea) {
}

record UpdateFloorRequest(
        @Size(min = 1, max = 100) String type,
        @Min(0) Double usableArea,
        @Min(0) Double totalArea) {
}

record FloorResponse(
        String id,
        String type,
        double usableArea,
        double totalArea,
        List<UnitResponse> units) {
}

// Units
record CreateUnitRequest(
        @NotBlank @Size(min = 1, max = 100) String label,
        @NotNull @Min(0) Double usableArea,
        @NotNull @Min(0) Integer bedrooms,
        @NotNull @Min(0) Integer bathrooms,
        @NotNull @Min(0) Integer balconies,
        @NotNull @Min(0) Integer toilets) {
}

record UpdateUnitRequest(
        @Size(min = 1, max = 100) String label,
        @Min(0) Double usableArea,
        @Min(0) Integer bedrooms,
        @Min(0) Integer bathrooms,
        @Min(0) Integer balconies,
        @Min(0) Integer toilets) {
}

record UnitResponse(
        String id,
        String label,
        double usableArea,
        int bedrooms,
        int bathrooms,
        int balconies,
        int toilets) {
}

// ----- Internal entities (mutable collections inside records via setters on fields that vary) -----

/**
 * Internal entity for in-memory storage.
 */
record ConstructionSiteEntity(
        String id,
        String name,
        String address,
        List<BuildingEntity> buildings,
        Instant createdAt,
        Instant updatedAt) {
    void setUpdatedAt(Instant t) { // records allow custom methods; fields are final but we keep updatedAt via accessor
        // Records are final; to update updatedAt we rely on outer service to replace the record OR we expose a workaround.
        // For simplicity and minimal copying, re-create a new record is more idiomatic, but we keep a setter-like method to signal intention.
        // However, since records are immutable, we cannot change updatedAt here. We will replace the entry in map when updating.
        // This method is a no-op to avoid accidental misuse.
    }
}

/**
 * Since we need mutability for nested collections and names, use simple POJOs for nested entities.
 */
final class BuildingEntity {
    private final String id;
    private String name;
    private final List<FloorEntity> floors;

    BuildingEntity(String id, String name, List<FloorEntity> floors) {
        this.id = id;
        this.name = name;
        this.floors = floors;
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    List<FloorEntity> floors() {
        return floors;
    }
}

final class FloorEntity {
    private final String id;
    private String type;
    private double usableArea;
    private double totalArea;
    private final List<UnitEntity> units;

    FloorEntity(String id, String type, double usableArea, double totalArea, List<UnitEntity> units) {
        this.id = id;
        this.type = type;
        this.usableArea = usableArea;
        this.totalArea = totalArea;
        this.units = units;
    }

    String id() {
        return id;
    }

    String type() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    double usableArea() {
        return usableArea;
    }

    void setUsableArea(double v) {
        this.usableArea = v;
    }

    double totalArea() {
        return totalArea;
    }

    void setTotalArea(double v) {
        this.totalArea = v;
    }

    List<UnitEntity> units() {
        return units;
    }
}

final class UnitEntity {
    private final String id;
    private String label;
    private double usableArea;
    private int bedrooms;
    private int bathrooms;
    private int balconies;
    private int toilets;

    UnitEntity(String id, String label, double usableArea, int bedrooms, int bathrooms, int balconies, int toilets) {
        this.id = id;
        this.label = label;
        this.usableArea = usableArea;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.balconies = balconies;
        this.toilets = toilets;
    }

    String id() {
        return id;
    }

    String label() {
        return label;
    }

    void setLabel(String label) {
        this.label = label;
    }

    double usableArea() {
        return usableArea;
    }

    void setUsableArea(double usableArea) {
        this.usableArea = usableArea;
    }

    int bedrooms() {
        return bedrooms;
    }

    void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

    int bathrooms() {
        return bathrooms;
    }

    void setBathrooms(int bathrooms) {
        this.bathrooms = bathrooms;
    }

    int balconies() {
        return balconies;
    }

    void setBalconies(int balconies) {
        this.balconies = balconies;
    }

    int toilets() {
        return toilets;
    }

    void setToilets(int toilets) {
        this.toilets = toilets;
    }
}
