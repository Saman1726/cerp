package io.cerp.organization.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.organization.domain.Organization;
import io.cerp.organization.dto.OrganizationCreateRequest;
import io.cerp.organization.dto.OrganizationResponse;
import io.cerp.organization.dto.OrganizationUpdateRequest;
import io.cerp.organization.repository.OrganizationRepository;

/**
 * Service for managing organizations.
 *
 * <p>
 * Handles CRUD operations, tenant isolation, and name uniqueness checks.
 */
@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    /**
     * Get all organizations for the given tenant.
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> findAll(String tenantId) {
        log.debug("Finding all organizations for tenant: {}", tenantId);
        return repository.findByTenantId(tenantId).stream()
            .map(OrganizationResponse::from)
            .toList();
    }

    /**
     * Get organization by ID within tenant scope.
     */
    @Transactional(readOnly = true)
    public OrganizationResponse findById(String tenantId, UUID id) {
        log.debug("Finding organization {} for tenant: {}", id, tenantId);
        return repository.findById(id)
            .filter(org -> org.getTenantId().equals(tenantId))
            .map(OrganizationResponse::from)
            .orElseThrow(() -> new OrganizationNotFoundException(tenantId, id));
    }

    /**
     * Create a new organization.
     */
    @Transactional
    public OrganizationResponse create(String tenantId, OrganizationCreateRequest request, String userId) {
        // Check name uniqueness within tenant
        if (repository.existsByTenantIdAndName(tenantId, request.name())) {
            throw new OrganizationNameAlreadyExistsException(tenantId, request.name());
        }

        Organization org = new Organization(tenantId, request.name(), request.currencyCode());

        // Map fields
        applyUpdateFields(org, request);

        org.setCreatedBy(userId);
        org.setUpdatedBy(userId);

        Organization saved = repository.save(org);
        log.info("Created organization {} for tenant: {}", saved.getId(), tenantId);

        return OrganizationResponse.from(saved);
    }

    /**
     * Update an existing organization.
     */
    @Transactional
    public OrganizationResponse update(String tenantId, UUID id, OrganizationUpdateRequest request, String userId) {
        Organization org = repository.findById(id)
            .filter(existing -> existing.getTenantId().equals(tenantId))
            .orElseThrow(() -> new OrganizationNotFoundException(tenantId, id));

        // Check name uniqueness if name is being changed
        if (request.name() != null && !request.name().equals(org.getName())) {
            if (repository.existsByTenantIdAndNameAndIdNot(tenantId, request.name(), id)) {
                throw new OrganizationNameAlreadyExistsException(tenantId, request.name());
            }
        }

        applyUpdateFields(org, request);
        org.setUpdatedBy(userId);

        Organization saved = repository.save(org);
        log.info("Updated organization {} for tenant: {}", saved.getId(), tenantId);

        return OrganizationResponse.from(saved);
    }

    /**
     * Delete an organization.
     */
    @Transactional
    public void delete(String tenantId, UUID id) {
        Organization org = repository.findById(id)
            .filter(existing -> existing.getTenantId().equals(tenantId))
            .orElseThrow(() -> new OrganizationNotFoundException(tenantId, id));

        repository.delete(org);
        log.info("Deleted organization {} for tenant: {}", id, tenantId);
    }

    /**
     * Apply update request fields to organization entity.
     */
    private void applyUpdateFields(Organization org, OrganizationUpdateRequest request) {
        if (request.name() != null)
            org.setName(request.name());
        if (request.legalForm() != null)
            org.setLegalForm(request.legalForm());
        if (request.taxId() != null)
            org.setTaxId(request.taxId());
        if (request.commercialRegister() != null)
            org.setCommercialRegister(request.commercialRegister());
        if (request.addressLine1() != null)
            org.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null)
            org.setAddressLine2(request.addressLine2());
        if (request.postalCode() != null)
            org.setPostalCode(request.postalCode());
        if (request.city() != null)
            org.setCity(request.city());
        if (request.countryCode() != null)
            org.setCountryCode(request.countryCode());
        if (request.contactEmail() != null)
            org.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null)
            org.setContactPhone(request.contactPhone());
        if (request.website() != null)
            org.setWebsite(request.website());
        if (request.currencyCode() != null)
            org.setCurrencyCode(request.currencyCode());
    }

    /**
     * Exception thrown when organization is not found within tenant scope.
     */
    public static class OrganizationNotFoundException extends RuntimeException {
        public OrganizationNotFoundException(String tenantId, UUID id) {
            super(String.format("Organization %s not found for tenant %s", id, tenantId));
        }
    }

    /**
     * Exception thrown when organization name already exists in tenant.
     */
    public static class OrganizationNameAlreadyExistsException extends RuntimeException {
        public OrganizationNameAlreadyExistsException(String tenantId, String name) {
            super(String.format("Organization name '%s' already exists for tenant %s", name, tenantId));
        }
    }
}
