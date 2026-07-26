package io.cerp.identity.config;

import io.cerp.identity.domain.Membership;
import io.cerp.identity.domain.ResourceMembership;
import io.cerp.identity.domain.ResourceMembership.ResourceType;
import io.cerp.identity.domain.Tenant;
import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.repo.MembershipRepository;
import io.cerp.identity.repo.ResourceMembershipRepository;
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
            ResourceMembershipRepository resourceMembershipRepo,
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

            // ── Seed John (regular user with construction sites and projects) ──

            // Seed tenant for John
            UUID johnTenantId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            if (tenantRepo.findById(johnTenantId).isEmpty()) {
                Tenant johnTenant = new Tenant();
                johnTenant.setId(johnTenantId);
                johnTenant.setName("John's Workspace");
                johnTenant.setRegion("john");
                tenantRepo.save(johnTenant);
                log.info("Seeded tenant: JOH - John's Workspace");
            }

            // Seed John's user account
            String johnEmail = "john@cerp.dev";
            String johnUsername = "John";
            String johnPassword = "secret";

            if (userRepo.findByEmail(johnEmail).isEmpty()) {
                UserAccount johnUser = new UserAccount();
                johnUser.setEmail(johnEmail);
                johnUser.setName(johnUsername);
                johnUser.setPasswordHash(passwordEncoder.encode(johnPassword));
                johnUser.setStatus("active");
                userRepo.save(johnUser);
                log.info("Seeded user: {} ({}) with password: {}", johnUsername, johnEmail, johnPassword);
            } else {
                log.info("User {} already exists", johnEmail);
            }

            // Seed membership for John
            UserAccount johnUser = userRepo.findByEmail(johnEmail).orElse(null);
            if (johnUser != null && membershipRepo.findByTenantIdAndUserId(johnTenantId, johnUser.getId()).isEmpty()) {
                Membership johnMembership = new Membership();
                johnMembership.setTenantId(johnTenantId);
                johnMembership.setUserId(johnUser.getId());
                johnMembership.setRoles(List.of("user", "owner"));
                johnMembership.setScopes(Map.of());
                membershipRepo.save(johnMembership);
                log.info("Seeded membership for user {} in tenant JOH with roles: user, owner", johnEmail);
            }

            // Seed resource memberships for John's construction sites and projects
            // Construction Site 1
            UUID constructionSite1Id = UUID.fromString("00000000-0000-0000-0000-000000000101");
            if (!resourceMembershipRepo.existsByResourceTypeAndResourceIdAndUserId(
                ResourceType.CONSTRUCTION_SITE, constructionSite1Id, johnUser.getId())) {
                ResourceMembership cs1Membership = new ResourceMembership(
                    ResourceType.CONSTRUCTION_SITE,
                    constructionSite1Id,
                    johnUser.getId(),
                    List.of("owner"),
                    Map.of(
                        "read", true,
                        "write", true,
                        "delete", true,
                        "assign", true),
                    johnUser.getId());
                resourceMembershipRepo.save(cs1Membership);
                log.info("Seeded resource membership for John on construction site 1");
            }

            // Construction Site 2
            UUID constructionSite2Id = UUID.fromString("00000000-0000-0000-0000-000000000102");
            if (!resourceMembershipRepo.existsByResourceTypeAndResourceIdAndUserId(
                ResourceType.CONSTRUCTION_SITE, constructionSite2Id, johnUser.getId())) {
                ResourceMembership cs2Membership = new ResourceMembership(
                    ResourceType.CONSTRUCTION_SITE,
                    constructionSite2Id,
                    johnUser.getId(),
                    List.of("owner"),
                    Map.of(
                        "read", true,
                        "write", true,
                        "delete", true,
                        "assign", true),
                    johnUser.getId());
                resourceMembershipRepo.save(cs2Membership);
                log.info("Seeded resource membership for John on construction site 2");
            }

            // Project 1
            UUID project1Id = UUID.fromString("00000000-0000-0000-0000-000000000201");
            if (!resourceMembershipRepo.existsByResourceTypeAndResourceIdAndUserId(
                ResourceType.PROJECT, project1Id, johnUser.getId())) {
                ResourceMembership proj1Membership = new ResourceMembership(
                    ResourceType.PROJECT,
                    project1Id,
                    johnUser.getId(),
                    List.of("owner"),
                    Map.of(
                        "read", true,
                        "write", true,
                        "delete", true,
                        "assign", true),
                    johnUser.getId());
                resourceMembershipRepo.save(proj1Membership);
                log.info("Seeded resource membership for John on project 1");
            }

            // Project 2
            UUID project2Id = UUID.fromString("00000000-0000-0000-0000-000000000202");
            if (!resourceMembershipRepo.existsByResourceTypeAndResourceIdAndUserId(
                ResourceType.PROJECT, project2Id, johnUser.getId())) {
                ResourceMembership proj2Membership = new ResourceMembership(
                    ResourceType.PROJECT,
                    project2Id,
                    johnUser.getId(),
                    List.of("owner"),
                    Map.of(
                        "read", true,
                        "write", true,
                        "delete", true,
                        "assign", true),
                    johnUser.getId());
                resourceMembershipRepo.save(proj2Membership);
                log.info("Seeded resource membership for John on project 2");
            }

            log.info("Seeding complete: Metal (vendor) and John (user) accounts are ready");
            log.info("  Metal: email=metal@cerp.dev, password=secret");
            log.info("  John: email=john@cerp.dev, password=secret");
        };
    }
}
