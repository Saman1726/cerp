package io.cerp.workflow.seed;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.cerp.workflow.service.TenantService;

/**
 * Initializes the default tenant at application startup if it does not already exist.
 * 
 * <p>
 * The default tenant has code 'default' and is used for development environments where the X-Tenant-Id header may not be provided.
 * </p>
 */
@Component
public class TenantSeedInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantSeedInitializer.class);

    private static final String DEFAULT_TENANT_CODE = "default";
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEFAULT_TENANT_NAME = "Default Tenant";
    private static final String DEFAULT_TENANT_DESCRIPTION = "System default tenant for development and initial setup";

    private final TenantService tenantService;

    public TenantSeedInitializer(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public void run(String... args) {
        // Check if default tenant already exists
        Optional<UUID> existingTenantId = tenantService.getTenantIdByCode(DEFAULT_TENANT_CODE);
        if (existingTenantId.isPresent()) {
            log.info("Tenant 'default' already exists with ID: {}", existingTenantId.get());
            return;
        }

        // Note: The Flyway migration V14 already seeds the tenant via SQL INSERT ... ON CONFLICT DO NOTHING.
        // This Java seed initializer is a backup in case the migration didn't run or was skipped.
        log.info("Tenant seed initializer: default tenant does not exist. It should have been created by Flyway migration V14.");
    }
}
