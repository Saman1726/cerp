package io.cerp.project.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import io.cerp.project.client.IdentitySubscriptionClient;
import io.cerp.project.client.IdentitySubscriptionClient.PlanLimits;
import io.cerp.project.domain.Building;
import io.cerp.project.domain.ConstructionSite;
import io.cerp.project.domain.Floor;
import io.cerp.project.domain.Unit;
import io.cerp.project.dto.BuildingCreateRequest;
import io.cerp.project.dto.BuildingResponse;
import io.cerp.project.dto.ConstructionSiteCreateRequest;
import io.cerp.project.dto.ConstructionSiteResponse;
import io.cerp.project.dto.FloorCreateRequest;
import io.cerp.project.dto.FloorResponse;
import io.cerp.project.dto.UnitCreateRequest;
import io.cerp.project.dto.UnitResponse;
import io.cerp.project.repository.BuildingRepository;
import io.cerp.project.repository.ConstructionSiteRepository;
import io.cerp.project.repository.FloorRepository;
import io.cerp.project.repository.UnitRepository;

/**
 * Service for construction site CRUD operations.
 */
@Service
public class ConstructionSiteService {

    private final ConstructionSiteRepository constructionSiteRepository;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final UnitRepository unitRepository;
    private final BuildingService buildingService;
    private final IdentitySubscriptionClient identitySubscriptionClient;

    public ConstructionSiteService(ConstructionSiteRepository constructionSiteRepository,
            BuildingRepository buildingRepository,
            FloorRepository floorRepository,
            UnitRepository unitRepository,
            BuildingService buildingService,
            IdentitySubscriptionClient identitySubscriptionClient) {
        this.constructionSiteRepository = constructionSiteRepository;
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.unitRepository = unitRepository;
        this.buildingService = buildingService;
        this.identitySubscriptionClient = identitySubscriptionClient;
    }

    /**
     * Get all construction sites for a tenant.
     */
    public List<ConstructionSiteResponse> findAll(String tenantId) {
        return constructionSiteRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Get a single construction site by ID.
     */
    public ConstructionSiteResponse findById(String tenantId, UUID id) {
        ConstructionSite site = constructionSiteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return toResponse(site);
    }

    /**
     * Create a new construction site. Quota is enforced per creator (their own plan), not per tenant - many users can share a tenant (see the shared default tenant),
     * so a tenant-wide count would let one user's usage block every other user sharing it.
     */
    @Transactional
    public ConstructionSiteResponse create(String tenantId, ConstructionSiteCreateRequest request, UUID creatorUserId) {
        enforceConstructionSiteQuota(creatorUserId);

        ConstructionSite site = new ConstructionSite(tenantId, request.name(), request.address());
        site.setCreatedBy(creatorUserId.toString());
        site.setUpdatedBy(creatorUserId.toString());
        site = constructionSiteRepository.save(site);
        return toResponse(site);
    }

    private void enforceConstructionSiteQuota(UUID creatorUserId) {
        PlanLimits limits = identitySubscriptionClient.getPlanLimits(creatorUserId)
            .orElseThrow(() -> new QuotaCheckUnavailableException("Could not verify Construction Site quota"));
        if (limits.maxConstructionSites() != null
                && constructionSiteRepository.countByCreatedBy(creatorUserId.toString()) >= limits.maxConstructionSites()) {
            throw new QuotaExceededException(
                "Your plan allows at most " + limits.maxConstructionSites() + " Construction Site(s).");
        }
    }

    /**
     * Update a construction site.
     */
    @Transactional
    public ConstructionSiteResponse update(String tenantId, UUID id, ConstructionSiteCreateRequest request) {
        ConstructionSite site = constructionSiteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        site.setName(request.name());
        site.setAddress(request.address());
        site = constructionSiteRepository.save(site);
        return toResponse(site);
    }

    /**
     * Delete a construction site.
     */
    @Transactional
    public void delete(String tenantId, UUID id) {
        ConstructionSite site = constructionSiteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        constructionSiteRepository.delete(site);
    }

    /**
     * Add a building to a construction site. Quota is checked against the SITE's creator (whoever's plan the site itself already counts against), not the acting
     * caller - consistent with "the resource owner's plan governs it" used throughout this design (see Project.primaryOwnerId).
     */
    @Transactional
    public BuildingResponse addBuilding(String tenantId, UUID siteId, BuildingCreateRequest request) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        enforceBuildingQuota(site);

        Building building = new Building(request.name());
        building.setConstructionSite(site);
        building = buildingRepository.save(building);
        return buildingService.toResponse(building);
    }

    private void enforceBuildingQuota(ConstructionSite site) {
        if (site.getCreatedBy() == null) {
            // Sites created before this feature shipped have no recorded creator - nothing
            // to check a quota against; don't retroactively block them.
            return;
        }
        UUID siteCreatorId = UUID.fromString(site.getCreatedBy());
        PlanLimits limits = identitySubscriptionClient.getPlanLimits(siteCreatorId)
            .orElseThrow(() -> new QuotaCheckUnavailableException("Could not verify Building quota"));
        if (limits.maxBuildings() != null
                && buildingRepository.countByConstructionSiteCreatedBy(site.getCreatedBy()) >= limits.maxBuildings()) {
            throw new QuotaExceededException("This Construction Site's plan allows at most " + limits.maxBuildings() + " Building(s).");
        }
    }

    /**
     * Update a building.
     */
    @Transactional
    public BuildingResponse updateBuilding(String tenantId, UUID siteId, UUID buildingId, BuildingCreateRequest request) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        building.setName(request.name());
        building = buildingRepository.save(building);
        return buildingService.toResponse(building);
    }

    /**
     * Delete a building from a construction site.
     */
    @Transactional
    public void deleteBuilding(String tenantId, UUID siteId, UUID buildingId) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        buildingRepository.delete(building);
    }

    /**
     * Add a floor to a building.
     */
    @Transactional
    public FloorResponse addFloor(String tenantId, UUID siteId, UUID buildingId, FloorCreateRequest request) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        Floor floor = new Floor(request.type(), request.usableArea(), request.totalArea());
        floor.setBuilding(building);
        floor = floorRepository.save(floor);

        return new FloorResponse(
            floor.getId(),
            floor.getType(),
            floor.getUsableArea(),
            floor.getTotalArea(),
            List.of(),
            floor.getCreatedAt(),
            floor.getUpdatedAt());
    }

    /**
     * Update a floor.
     */
    @Transactional
    public FloorResponse updateFloor(String tenantId, UUID siteId, UUID buildingId, UUID floorId, FloorCreateRequest request) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        Floor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Floor not found"));

        if (!floor.getBuilding().getId().equals(buildingId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Floor does not belong to this building");
        }

        floor.setType(request.type());
        floor.setUsableArea(request.usableArea());
        floor.setTotalArea(request.totalArea());
        floor = floorRepository.save(floor);

        return new FloorResponse(
            floor.getId(),
            floor.getType(),
            floor.getUsableArea(),
            floor.getTotalArea(),
            List.of(),
            floor.getCreatedAt(),
            floor.getUpdatedAt());
    }

    /**
     * Delete a floor from a building.
     */
    @Transactional
    public void deleteFloor(String tenantId, UUID siteId, UUID buildingId, UUID floorId) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        Floor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Floor not found"));

        if (!floor.getBuilding().getId().equals(buildingId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Floor does not belong to this building");
        }

        floorRepository.delete(floor);
    }

    /**
     * Add a unit to a floor.
     */
    @Transactional
    public UnitResponse addUnit(String tenantId, UUID siteId, UUID buildingId, UUID floorId, UnitCreateRequest request) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        Floor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Floor not found"));

        if (!floor.getBuilding().getId().equals(buildingId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Floor does not belong to this building");
        }

        Unit unit = new Unit(request.label(), request.usableArea(), request.bedrooms(),
            request.bathrooms(), request.balconies(), request.toilets());
        unit.setFloor(floor);
        unit = unitRepository.save(unit);

        return new UnitResponse(
            unit.getId(),
            unit.getLabel(),
            unit.getUsableArea(),
            unit.getBedrooms(),
            unit.getBathrooms(),
            unit.getBalconies(),
            unit.getToilets(),
            unit.getCreatedAt(),
            unit.getUpdatedAt());
    }

    /**
     * Update a unit.
     */
    @Transactional
    public UnitResponse updateUnit(String tenantId, UUID siteId, UUID buildingId, UUID floorId, UUID unitId, UnitCreateRequest request) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        Floor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Floor not found"));

        if (!floor.getBuilding().getId().equals(buildingId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Floor does not belong to this building");
        }

        Unit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

        if (!unit.getFloor().getId().equals(floorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unit does not belong to this floor");
        }

        unit.setLabel(request.label());
        unit.setUsableArea(request.usableArea());
        unit.setBedrooms(request.bedrooms());
        unit.setBathrooms(request.bathrooms());
        unit.setBalconies(request.balconies());
        unit.setToilets(request.toilets());
        unit = unitRepository.save(unit);

        return new UnitResponse(
            unit.getId(),
            unit.getLabel(),
            unit.getUsableArea(),
            unit.getBedrooms(),
            unit.getBathrooms(),
            unit.getBalconies(),
            unit.getToilets(),
            unit.getCreatedAt(),
            unit.getUpdatedAt());
    }

    /**
     * Delete a unit from a floor.
     */
    @Transactional
    public void deleteUnit(String tenantId, UUID siteId, UUID buildingId, UUID floorId, UUID unitId) {
        ConstructionSite site = constructionSiteRepository.findById(siteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Construction site not found"));

        if (!site.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));

        if (!building.getConstructionSite().getId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building does not belong to this site");
        }

        Floor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Floor not found"));

        if (!floor.getBuilding().getId().equals(buildingId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Floor does not belong to this building");
        }

        Unit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

        if (!unit.getFloor().getId().equals(floorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unit does not belong to this floor");
        }

        unitRepository.delete(unit);
    }

    /**
     * Convert a ConstructionSite domain object to a response DTO with nested buildings, floors and units.
     */
    @Transactional(readOnly = true)
    public ConstructionSiteResponse toResponse(ConstructionSite site) {
        List<Building> buildings = buildingRepository.findByConstructionSiteId(site.getId());
        List<BuildingResponse> buildingResponses = buildings.stream()
            .map(buildingService::toResponse)
            .toList();

        return new ConstructionSiteResponse(
            site.getId(),
            site.getName(),
            site.getAddress(),
            buildingResponses,
            site.getCreatedAt(),
            site.getUpdatedAt());
    }
}
