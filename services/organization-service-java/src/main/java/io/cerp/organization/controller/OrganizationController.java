package io.cerp.organization.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.organization.dto.OrganizationCreateRequest;
import io.cerp.organization.dto.OrganizationResponse;
import io.cerp.organization.dto.OrganizationUpdateRequest;
import io.cerp.organization.service.OrganizationService;

/**
 * REST controller for organization CRUD operations.
 *
 * <p>
 * Tenant context is extracted from the X-Tenant-Id header (injected by the API Gateway). User identity is extracted from the JWT token.
 */
@RestController
@RequestMapping("/api/orgs/v1/organizations")
public class OrganizationController {

    private static final Logger log = LoggerFactory.getLogger(OrganizationController.class);

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * List all organizations for the tenant.
     */
    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> list(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        log.debug("Listing organizations for tenant: {}", tenantId);
        List<OrganizationResponse> organizations = organizationService.findAll(tenantId);
        return ResponseEntity.ok(organizations);
    }

    /**
     * Get a single organization by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        OrganizationResponse org = organizationService.findById(tenantId, id);
        return ResponseEntity.ok(org);
    }

    /**
     * Create a new organization.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrganizationResponse> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @Valid @RequestBody OrganizationCreateRequest request) {
        OrganizationResponse created = organizationService.create(tenantId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing organization (partial update).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        OrganizationResponse updated = organizationService.update(tenantId, id, request, userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an organization.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        organizationService.delete(tenantId, id);
    }
}
