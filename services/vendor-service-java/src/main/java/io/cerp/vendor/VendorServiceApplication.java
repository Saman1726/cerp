package io.cerp.vendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vendor Service (Phase 2 foundation) - Provides Vendor onboarding and directory capabilities (CRUD + approval) with multi-tenant isolation. - JWT/OAuth2 resource server
 * is configured at the application layer (to be added). - PostgreSQL with RLS enforced via app.tenant_id session variable (RlsTenantAspect to be added). - Kafka events
 * published for VendorCreated/VendorUpdated/VendorApproved/RatingUpdated (publisher to be added).
 */
@SpringBootApplication
public class VendorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VendorServiceApplication.class, args);
    }
}
