package io.cerp.identity.events;

import io.cerp.identity.domain.Tenant;
import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.service.MembershipService;
import io.cerp.identity.service.TenantService;
import io.cerp.identity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.OffsetDateTime;

/**
 * Consumes vendor registration events from Kafka and creates the necessary identity records (tenant, user, membership) for the new vendor.
 *
 * Flow: 1. Receive VendorRegistrationEvent from vendor-service 2. Create Tenant 3. Create UserAccount 4. Create Membership with roles=["vendor"] 5. Emit
 * VendorIdentityCreated event with tenantId, userId for vendor-service to link
 */
@Component
@ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
public class VendorRegistrationConsumer {

    private static final Logger log = LoggerFactory.getLogger(VendorRegistrationConsumer.class);

    private final TenantService tenantService;
    private final UserService userService;
    private final MembershipService membershipService;
    private final IdentityEventPublisher eventPublisher;

    @Autowired
    public VendorRegistrationConsumer(TenantService tenantService,
            UserService userService,
            MembershipService membershipService,
            IdentityEventPublisher eventPublisher) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.membershipService = membershipService;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = "${app.topics.vendor-registration:vendor.registered.v1}", groupId = "${app.kafka.consumer.group-id:identity-service}")
    public void handleVendorRegistration(@Payload VendorRegistrationEvent event) {
        log.info("Processing vendor registration event for vendorId={}, email={}", event.getVendorId(), event.getEmail());

        try {
            // 1. Create Tenant
            Tenant tenant = tenantService.create(event.getTenantName(), event.getRegion());
            log.info("Created tenant id={} for vendorId={}", tenant.getId(), event.getVendorId());

            // 2. Create UserAccount
            UserAccount user = userService.create(event.getEmail(), event.getLegalName());
            log.info("Created user id={} for vendorId={}", user.getId(), event.getVendorId());

            // 3. Create Membership with vendor role
            List<String> roles = List.of("vendor");
            Map<String, Object> scopes = Map.of("vendorId", event.getVendorId().toString(), "selfService", true);
            membershipService.assign(tenant.getId(), user.getId(), roles, scopes);
            log.info("Created membership for tenantId={}, userId={}, vendorId={}",
                tenant.getId(), user.getId(), event.getVendorId());

            // 4. Publish event for vendor-service to link identity
            if (eventPublisher != null) {
                eventPublisher.publishVendorIdentityCreated(event.getVendorId(), tenant.getId(), user.getId());
            }

            log.info("Vendor identity creation complete for vendorId={}", event.getVendorId());

        } catch (Exception e) {
            log.error("Failed to process vendor registration event for vendorId={}: {}",
                event.getVendorId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Vendor registration event payload from vendor-service.
     */
    public static class VendorRegistrationEvent {
        private UUID vendorId;
        private String email;
        private String password;
        private String legalName;
        private String contactName;
        private String phone;
        private String website;
        private String address;
        private String city;
        private String country;
        private String region;
        private String tenantName;
        private List<String> categories;

        public UUID getVendorId() {
            return vendorId;
        }

        public void setVendorId(UUID vendorId) {
            this.vendorId = vendorId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getLegalName() {
            return legalName;
        }

        public void setLegalName(String legalName) {
            this.legalName = legalName;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(String tenantName) {
            this.tenantName = tenantName;
        }

        public List<String> getCategories() {
            return categories;
        }

        public void setCategories(List<String> categories) {
            this.categories = categories;
        }
    }
}
