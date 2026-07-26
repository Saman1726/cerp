package io.cerp.vendor.service;

import io.cerp.vendor.entity.Vendor;
import io.cerp.vendor.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of VendorService with event-driven vendor registration.
 * 
 * Registration flow: 1. register() creates a Vendor record (status=pending) without tenantId/userId 2. Publishes VendorRegisteredEvent to Kafka topic
 * vendor.registered.v1 3. identity-service consumes event, creates tenant/user/membership 4. identity-service publishes identity.vendor.v1 event 5.
 * VendorIdentityCreatedConsumer calls linkIdentity() to update vendor
 */
@Service
public class VendorServiceImpl implements VendorService {

    private static final Logger log = LoggerFactory.getLogger(VendorServiceImpl.class);

    private final VendorRepository vendorRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String vendorRegistrationTopic;

    public VendorServiceImpl(
            VendorRepository vendorRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.topics.vendor-registration:vendor.registered.v1}") String vendorRegistrationTopic) {
        this.vendorRepository = vendorRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.vendorRegistrationTopic = vendorRegistrationTopic;
    }

    @Override
    @Transactional
    public Vendor create(UUID tenantId, UUID userId, String legalName, String contactName,
            String phone, String website, String address, String city,
            String country, List<String> categories) {
        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId);
        vendor.setUserId(userId);
        vendor.setLegalName(legalName);
        vendor.setContactName(contactName);
        vendor.setPhone(phone);
        vendor.setWebsite(website);
        vendor.setAddress(address);
        vendor.setCity(city);
        vendor.setCountry(country);
        vendor.setCategories(categories != null ? categories : List.of());
        vendor.setStatus("pending");
        return vendorRepository.save(vendor);
    }

    @Override
    @Transactional
    public Vendor register(String email, String password, String legalName, String contactName,
            String phone, String website, String address, String city,
            String country, String region, String tenantName, List<String> categories) {

        Vendor vendor = new Vendor();
        vendor.setLegalName(legalName);
        vendor.setContactName(contactName);
        vendor.setPhone(phone);
        vendor.setWebsite(website);
        vendor.setAddress(address);
        vendor.setCity(city);
        vendor.setCountry(country);
        vendor.setCategories(categories != null ? categories : List.of());
        vendor.setStatus("pending");

        // Save vendor first without tenantId/userId (will be updated by event consumer)
        vendor = vendorRepository.save(vendor);

        // Publish Kafka event for identity-service to create credentials
        try {
            VendorRegistrationEvent event = new VendorRegistrationEvent();
            event.setVendorId(vendor.getId());
            event.setEmail(email);
            event.setPassword(password);
            event.setLegalName(legalName);
            event.setContactName(contactName);
            event.setPhone(phone);
            event.setWebsite(website);
            event.setAddress(address);
            event.setCity(city);
            event.setCountry(country);
            event.setRegion(region);
            event.setTenantName(tenantName != null ? tenantName : legalName);
            event.setCategories(categories != null ? categories : List.of());
            event.setTimestamp(OffsetDateTime.now());

            Message<VendorRegistrationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("spring.kafka.topic", vendorRegistrationTopic)
                .build();

            kafkaTemplate.send(message);

            log.info("Vendor registration event published for vendorId={}, email={}", vendor.getId(), email);
        } catch (Exception e) {
            log.error("Failed to publish vendor registration event for vendorId={}: {}", vendor.getId(), e.getMessage(), e);
            // Don't fail the request - vendor is already saved, event can be retried
        }

        return vendor;
    }

    @Override
    public Optional<Vendor> findById(UUID id) {
        return vendorRepository.findById(id);
    }

    @Override
    public Optional<Vendor> findByTenantId(UUID tenantId) {
        return vendorRepository.findByTenantId(tenantId);
    }

    @Override
    public List<Vendor> findAll() {
        return vendorRepository.findAll();
    }

    @Override
    public List<Vendor> findByStatus(String status) {
        return vendorRepository.findByStatus(status);
    }

    @Override
    public List<Vendor> findByCategoriesContains(String category) {
        return vendorRepository.findByCategoriesContains(category);
    }

    @Override
    @Transactional
    public Vendor updateStatus(UUID id, String status) {
        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));
        vendor.setStatus(status);
        return vendorRepository.save(vendor);
    }

    @Override
    @Transactional
    public Vendor linkIdentity(UUID vendorId, UUID tenantId, UUID userId) {
        Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));

        boolean wasUnlinked = vendor.getTenantId() == null;

        vendor.setTenantId(tenantId);
        vendor.setUserId(userId);
        vendor = vendorRepository.save(vendor);

        if (wasUnlinked) {
            log.info("Vendor identity linked: vendorId={}, tenantId={}, userId={}", vendorId, tenantId, userId);
        } else {
            log.warn("Vendor identity already linked for vendorId={}, overwriting", vendorId);
        }

        return vendor;
    }

    @Override
    @Transactional
    public Vendor register(String email, String password, String legalName, String contactName,
            String phone, String website, String address, String city,
            String country, String region, String tenantName, UUID tenantId, UUID userId, List<String> categories) {

        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId);
        vendor.setUserId(userId);
        vendor.setLegalName(legalName);
        vendor.setContactName(contactName);
        vendor.setPhone(phone);
        vendor.setWebsite(website);
        vendor.setAddress(address);
        vendor.setCity(city);
        vendor.setCountry(country);
        vendor.setCategories(categories != null ? categories : List.of());
        vendor.setStatus("pending");

        // Save vendor with tenantId/userId directly
        vendor = vendorRepository.save(vendor);

        log.info("Vendor created directly with tenantId={}, userId={}", tenantId, userId);

        return vendor;
    }

    /**
     * Kafka event payload for vendor registration. Consumed by identity-service VendorRegistrationConsumer.
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

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
