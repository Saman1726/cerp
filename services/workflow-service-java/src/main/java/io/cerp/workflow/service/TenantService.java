package io.cerp.workflow.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
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
 * 
 * <h3>Singleton Pattern</h3>
 * <p>
 * This service is a Spring singleton (default scope). Additionally, it can be accessed statically via {@link #getInstance()}. Example usage:
 * 
 * <pre>
 * // Static access (anywhere, even outside Spring-managed beans):
 * UUID tenantId = TenantService.getInstance().getTenantIdByCode("acme-corp");
 * 
 * // Dependency injection (recommended in Spring-managed beans):
 * &#64;RequiredArgsConstructor
 * public class SomeService {
 *     private final TenantService tenantService;
 * }
 * </pre>
 * </p>
 */
@Service
public class TenantService implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    /**
     * Spring ApplicationContext, set via {@link #setApplicationContext}.
     */
    private static ApplicationContext applicationContext;

    /**
     * Static instance of this singleton bean, set via {@link #init()} or lazily resolved via {@link #getInstance()}.
     */
    private static TenantService staticInstance;

    private final TenantRepository tenantRepository;

    /**
     * Constructor with dependency injection.
     * 
     * @param tenantRepository
     *            the repository for tenant data
     */
    public TenantService(@Lazy TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Called by Spring to provide the ApplicationContext reference.
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    /**
     * Returns the static singleton instance of this service.
     * 
     * <p>
     * If the instance has not yet been initialized by {@link #init()}, this method lazily resolves it from the Spring {@link ApplicationContext}. If the application
     * context is also not available, throws {@link IllegalStateException}.
     * </p>
     * 
     * <p>
     * <b>Warning:</b> Prefer dependency injection in Spring-managed beans. Static access is useful for non-Spring contexts (e.g., static methods, listeners, or legacy
     * code).
     * </p>
     * 
     * @return the singleton TenantService instance
     * @throws IllegalStateException
     *             if the Spring context is not available
     */
    public static TenantService getInstance() {
        if (staticInstance == null) {
            if (applicationContext != null && applicationContext.containsBean("tenantService")) {
                staticInstance = applicationContext.getBean(TenantService.class);
                log.debug("TenantService static instance resolved from ApplicationContext");
            } else {
                throw new IllegalStateException(
                    "TenantService has not been initialized and Spring ApplicationContext is not available. "
                            + "Ensure getInstance() is called after the application context is fully started, or prefer dependency injection.");
            }
        }
        return staticInstance;
    }

    /**
     * Initializes the static instance reference. Called by Spring after the bean is constructed and dependencies are injected.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        staticInstance = this;
        log.info("TenantService static singleton initialized");
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
     * Resolve the tenant String ID by its code.
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
    public String getTenantIdByCodeStr(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            log.debug("Empty or null tenant code provided");
            return "";
        }

        return getTenantIdByCodeOrThrow(tenantCode).toString();

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
