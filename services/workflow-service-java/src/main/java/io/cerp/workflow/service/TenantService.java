package io.cerp.workflow.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.workflow.domain.Tenant;
import io.cerp.workflow.repository.TenantRepository;

/**
 * Service for resolving tenant IDs from tenant codes.
 * 
 * <p>
 * This service is called by TenantRequestFilter to look up the UUID of a tenant based on the code provided in the X-Tenant-Id header. The resolved UUID is then stored in
 * TenantContext for request-scoped access.
 * </p>
 */
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Resolve the tenant UUID by its code.
     * 
     * <p>
     * Looks up an active tenant by the provided code (e.g., 'default', 'acme-corp'). Returns the UUID of the tenant, or empty if not found.
     * </p>
     * 
     * @param tenantCode
     *            the tenant code from the X-Tenant-Id header
     * @return Optional containing the tenant UUID if found and active, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<UUID> getTenantIdByCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            log.debug("Empty or null tenant code provided");
            return Optional.empty();
        }

        return tenantRepository.findByCodeAndActiveIsTrue(tenantCode.trim()).map(Tenant::getId);
    }

    /**
     * Get the tenant UUID by its code, throwing an exception if not found.
     * 
     * <p>
     * Used when a tenant is required for the operation.
     * </p>
     * 
     * @param tenantCode
     *            the tenant code
     * @return the tenant UUID
     * @throws IllegalArgumentException
     *             if no active tenant is found with the given code
     */
    @Transactional(readOnly = true)
    public UUID getTenantIdByCodeOrThrow(String tenantCode) {
        return getTenantIdByCode(tenantCode)
            .orElseThrow(() -> new IllegalArgumentException("Active tenant not found with code: " + tenantCode));
    }

    /**
     * Check if a tenant exists and is active.
     * 
     * @param tenantCode
     *            the tenant code
     * @return true if the tenant exists and is active, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean tenantExists(String tenantCode) {
        return getTenantIdByCode(tenantCode).isPresent();
    }
}
