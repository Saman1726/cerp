package io.cerp.project.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.project.config.RequestIdentity;
import io.cerp.project.dto.BuildingCreateRequest;
import io.cerp.project.dto.BuildingResponse;
import io.cerp.project.dto.ConstructionSiteCreateRequest;
import io.cerp.project.dto.ConstructionSiteResponse;
import io.cerp.project.dto.FloorCreateRequest;
import io.cerp.project.dto.FloorResponse;
import io.cerp.project.dto.UnitCreateRequest;
import io.cerp.project.dto.UnitResponse;
import io.cerp.project.service.ConstructionSiteService;

/**
 * REST controller for construction site CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/construction-sites")
public class ConstructionSiteController {

    private static final Logger log = LoggerFactory.getLogger(ConstructionSiteController.class);

    private final ConstructionSiteService constructionSiteService;

    public ConstructionSiteController(ConstructionSiteService constructionSiteService) {
        this.constructionSiteService = constructionSiteService;
    }

    /**
     * List all construction sites for the tenant, filtered by creator.
     */
    @GetMapping
    public ResponseEntity<List<ConstructionSiteResponse>> list(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        log.debug("Listing construction sites for tenant: {} (user: {})", tenantId, userId);
        List<ConstructionSiteResponse> sites = constructionSiteService.findAll(tenantId, userId);
        return ResponseEntity.ok(sites);
    }

    /**
     * Get a single construction site by ID. Only the creator can access.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConstructionSiteResponse> getById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        ConstructionSiteResponse site = constructionSiteService.findById(tenantId, userId, id);
        return ResponseEntity.ok(site);
    }

    /**
     * Create a new construction site.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ConstructionSiteResponse> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ConstructionSiteCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ConstructionSiteResponse created = constructionSiteService.create(tenantId, request, RequestIdentity.creatorId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a construction site. Only the creator can update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConstructionSiteResponse> update(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody ConstructionSiteCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        ConstructionSiteResponse updated = constructionSiteService.update(tenantId, userId, id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a construction site. Only the creator can delete.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        constructionSiteService.delete(tenantId, userId, id);
    }

    /**
     * Add a building to a construction site. Only the site creator can modify.
     */
    @PostMapping("/{siteId}/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<BuildingResponse> addBuilding(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @Valid @RequestBody BuildingCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        BuildingResponse created = constructionSiteService.addBuilding(tenantId, userId, siteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a building. Only the site creator can modify.
     */
    @PutMapping("/{siteId}/buildings/{buildingId}")
    public ResponseEntity<BuildingResponse> updateBuilding(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @Valid @RequestBody BuildingCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        BuildingResponse updated = constructionSiteService.updateBuilding(tenantId, userId, siteId, buildingId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a building. Only the site creator can delete.
     */
    @DeleteMapping("/{siteId}/buildings/{buildingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuilding(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        constructionSiteService.deleteBuilding(tenantId, userId, siteId, buildingId);
    }

    /**
     * Add a floor to a building. Only the site creator can modify.
     */
    @PostMapping("/{siteId}/buildings/{buildingId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<FloorResponse> addFloor(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @Valid @RequestBody FloorCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        FloorResponse created = constructionSiteService.addFloor(tenantId, userId, siteId, buildingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a floor. Only the site creator can modify.
     */
    @PutMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}")
    public ResponseEntity<FloorResponse> updateFloor(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @Valid @RequestBody FloorCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        FloorResponse updated = constructionSiteService.updateFloor(tenantId, userId, siteId, buildingId, floorId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a floor. Only the site creator can delete.
     */
    @DeleteMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFloor(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        constructionSiteService.deleteFloor(tenantId, userId, siteId, buildingId, floorId);
    }

    /**
     * Add a unit to a floor. Only the site creator can modify.
     */
    @PostMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}/units")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UnitResponse> addUnit(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @Valid @RequestBody UnitCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        UnitResponse created = constructionSiteService.addUnit(tenantId, userId, siteId, buildingId, floorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a unit. Only the site creator can modify.
     */
    @PutMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}/units/{unitId}")
    public ResponseEntity<UnitResponse> updateUnit(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID unitId,
            @Valid @RequestBody UnitCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        UnitResponse updated = constructionSiteService.updateUnit(tenantId, userId, siteId, buildingId, floorId, unitId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a unit. Only the site creator can delete.
     */
    @DeleteMapping("/{siteId}/buildings/{buildingId}/floors/{floorId}/units/{unitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnit(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID siteId,
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID unitId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = RequestIdentity.creatorId(jwt).toString();
        constructionSiteService.deleteUnit(tenantId, userId, siteId, buildingId, floorId, unitId);
    }
}
