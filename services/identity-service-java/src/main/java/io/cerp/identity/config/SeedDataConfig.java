package io.cerp.identity.config;

import io.cerp.identity.domain.Membership;
import io.cerp.identity.domain.Tenant;
import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.repo.MembershipRepository;
import io.cerp.identity.repo.TenantRepository;
import io.cerp.identity.repo.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seed data for development: creates the "Metal" vendor user with password "secret" so that login works immediately after first registration.
 */
@Configuration
public class SeedDataConfig {

    private static final Logger log = LoggerFactory.getLogger(SeedDataConfig.class);

    @Bean
    public CommandLineRunner seedVendorUser(UserAccountRepository userRepo,
            TenantRepository tenantRepo,
            MembershipRepository membershipRepo,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed tenant
            UUID vendorTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            if (tenantRepo.findById(vendorTenantId).isEmpty()) {
                Tenant vendorTenant = new Tenant();
                vendorTenant.setId(vendorTenantId);
                vendorTenant.setName("Metal Trading (MTRD)");
                vendorTenant.setRegion("mtrd");
                tenantRepo.save(vendorTenant);
                log.info("Seeded tenant: MTRD - Metal Trading");
            }

            // Seed user
            String seedEmail = "metal@cerp.dev";
            String seedUsername = "Metal";
            String seedPassword = "secret";

            if (userRepo.findByEmail(seedEmail).isEmpty()) {
                UserAccount user = new UserAccount();
                user.setEmail(seedEmail);
                user.setName(seedUsername);
                user.setPasswordHash(passwordEncoder.encode(seedPassword));
                user.setStatus("active");
                userRepo.save(user);
                log.info("Seeded vendor user: {} ({}) with password: {}", seedUsername, seedEmail, seedPassword);
            } else {
                log.info("Vendor user {} already exists", seedEmail);
            }

            // Seed membership for the user
            UserAccount user = userRepo.findByEmail(seedEmail).orElse(null);
            if (user != null && membershipRepo.findByTenantIdAndUserId(vendorTenantId, user.getId()).isEmpty()) {
                Membership membership = new Membership();
                membership.setTenantId(vendorTenantId);
                membership.setUserId(user.getId());
                membership.setRoles(List.of("vendor"));
                membership.setScopes(Map.of());
                membershipRepo.save(membership);
                log.info("Seeded membership for user {} in tenant MTRD with roles: vendor", seedEmail);
            }
        };
    }
}
