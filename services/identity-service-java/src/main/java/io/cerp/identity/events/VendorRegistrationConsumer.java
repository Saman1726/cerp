package io.cerp.identity.events;

import io.cerp.identity.domain.Plan;
import io.cerp.identity.domain.UserAccount;
import io.cerp.identity.service.MembershipService;
import io.cerp.identity.service.SubscriptionService;
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

    // Every registration lands on this shared default tenant (see Part A / apps/web's
    // register-*/route.ts) - an Admin assigns a private tenant separately, later.
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserService userService;
    private final MembershipService membershipService;
    private final SubscriptionService subscriptionService;
    private final IdentityEventPublisher eventPublisher;

    @Autowired
    public VendorRegistrationConsumer(UserService userService,
            MembershipService membershipService,
            SubscriptionService subscriptionService,
            IdentityEventPublisher eventPublisher) {
        this.userService = userService;
        this.membershipService = membershipService;
        this.subscriptionService = subscriptionService;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = "${app.topics.vendor-registration:vendor.registered.v1}", groupId = "${app.kafka.consumer.group-id:identity-service}")
    public void handleVendorRegistration(@Payload VendorRegistrationEvent event) {
        log.info("Processing vendor registration event for vendorId={}, email={}", event.getVendorId(), event.getEmail());

        try {
            // 1. Create UserAccount. Uses createWithPassword (not create) so the vendor can
            // actually log in - the event carries a password specifically for this.
            UserAccount user = userService.createWithPassword(event.getEmail(), event.getLegalName(), event.getPassword(), UserAccount.AccountType.VENDOR);
            log.info("Created user id={} for vendorId={}", user.getId(), event.getVendorId());

            // 2. Create Membership on the default tenant with vendor role
            List<String> roles = List.of("vendor");
            Map<String, Object> scopes = Map.of("vendorId", event.getVendorId().toString(), "selfService", true);
            membershipService.assign(DEFAULT_TENANT_ID, user.getId(), roles, scopes);
            log.info("Created membership for tenantId={}, userId={}, vendorId={}",
                DEFAULT_TENANT_ID, user.getId(), event.getVendorId());

            // 3. Create Subscription (every account gets one, regardless of type)
            subscriptionService.create(user.getId(), Plan.FREEMIUM);
            log.info("Created Freemium subscription for userId={}, vendorId={}", user.getId(), event.getVendorId());

            // 4. Publish event for vendor-service to link identity
            if (eventPublisher != null) {
                eventPublisher.publishVendorIdentityCreated(event.getVendorId(), DEFAULT_TENANT_ID, user.getId());
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
        private String tenantName;
        private List<String> categories;
        private String region;
        private List<String> vendorTypes;
        private List<UUID> categoryGroupIds;
        private List<UUID> capabilityIds;
        private List<String> projectTypes;
        private Integer yearsInBusiness;
        private String description;
        private String logoUrl;
        private String registrationNumber;
        private List<String> regions;
        private OffsetDateTime timestamp;

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

        public List<String> getVendorTypes() {
            return vendorTypes;
        }

        public void setVendorTypes(List<String> vendorTypes) {
            this.vendorTypes = vendorTypes;
        }

        public List<UUID> getCategoryGroupIds() {
            return categoryGroupIds;
        }

        public void setCategoryGroupIds(List<UUID> categoryGroupIds) {
            this.categoryGroupIds = categoryGroupIds;
        }

        public List<UUID> getCapabilityIds() {
            return capabilityIds;
        }

        public void setCapabilityIds(List<UUID> capabilityIds) {
            this.capabilityIds = capabilityIds;
        }

        public List<String> getProjectTypes() {
            return projectTypes;
        }

        public void setProjectTypes(List<String> projectTypes) {
            this.projectTypes = projectTypes;
        }

        public Integer getYearsInBusiness() {
            return yearsInBusiness;
        }

        public void setYearsInBusiness(Integer yearsInBusiness) {
            this.yearsInBusiness = yearsInBusiness;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        public String getRegistrationNumber() {
            return registrationNumber;
        }

        public void setRegistrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
        }

        public List<String> getRegions() {
            return regions;
        }

        public void setRegions(List<String> regions) {
            this.regions = regions;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
