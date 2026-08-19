package io.cerp.vendor.service;

import io.cerp.vendor.dto.CategoryGroupResponse;
import io.cerp.vendor.dto.CreateVendorCertificationRequest;
import io.cerp.vendor.dto.CreateVendorProjectRequest;
import io.cerp.vendor.dto.VendorCertificationResponse;
import io.cerp.vendor.dto.VendorProjectResponse;
import io.cerp.vendor.dto.VendorRegistrationEvent;
import io.cerp.vendor.entity.Vendor;
import io.cerp.vendor.entity.VendorCertification;
import io.cerp.vendor.entity.VendorProject;
import io.cerp.vendor.repository.CategoryGroupRepository;
import io.cerp.vendor.repository.VendorCertificationRepository;
import io.cerp.vendor.repository.VendorProjectRepository;
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

@Service
public class VendorServiceImpl implements VendorService {

    private static final Logger log = LoggerFactory.getLogger(VendorServiceImpl.class);
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final VendorRepository vendorRepository;
    private final VendorCertificationRepository vendorCertificationRepository;
    private final VendorProjectRepository vendorProjectRepository;
    private final CategoryGroupRepository categoryGroupRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String vendorRegistrationTopic;

    public VendorServiceImpl(
            VendorRepository vendorRepository,
            VendorCertificationRepository vendorCertificationRepository,
            VendorProjectRepository vendorProjectRepository,
            CategoryGroupRepository categoryGroupRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.topics.vendor-registration:vendor.registered.v1}") String vendorRegistrationTopic) {
        this.vendorRepository = vendorRepository;
        this.vendorCertificationRepository = vendorCertificationRepository;
        this.vendorProjectRepository = vendorProjectRepository;
        this.categoryGroupRepository = categoryGroupRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.vendorRegistrationTopic = vendorRegistrationTopic;
    }

    @Override @Transactional
    public Vendor create(UUID tenantId, UUID userId, String legalName, String contactName,
            String phone, String website, String address, String city,
            String country, List<String> categories) {
        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId != null ? tenantId : DEFAULT_TENANT_ID); vendor.setUserId(userId);
        vendor.setLegalName(legalName); vendor.setContactName(contactName);
        vendor.setPhone(phone); vendor.setWebsite(website);
        vendor.setAddress(address); vendor.setCity(city); vendor.setCountry(country);
        vendor.setCategories(categories != null ? categories : List.of());
        vendor.setStatus("pending"); vendor.onCreate();
        return vendorRepository.save(vendor);
    }

    @Override @Transactional
    public Vendor register(String email, String password, String legalName, String contactName,
            String phone, String website, String address, String city,
            String country, String region, String tenantName,
            List<String> vendorTypes, List<UUID> categoryGroupIds,
            List<UUID> capabilityIds, List<String> projectTypes,
            Integer yearsInBusiness, String description, String logoUrl,
            String registrationNumber, List<String> regions) {
        return register(email, password, legalName, contactName, phone, website,
                address, city, country, region, tenantName, DEFAULT_TENANT_ID, null,
                vendorTypes, categoryGroupIds, capabilityIds, projectTypes,
                yearsInBusiness, description, logoUrl, registrationNumber, regions, List.of());
    }

    @Override @Transactional
    public Vendor register(String email, String password, String legalName, String contactName,
            String phone, String website, String address, String city,
            String country, String region, String tenantName, UUID tenantId, UUID userId,
            List<String> vendorTypes, List<UUID> categoryGroupIds,
            List<UUID> capabilityIds, List<String> projectTypes,
            Integer yearsInBusiness, String description, String logoUrl,
            String registrationNumber, List<String> regions, List<String> categories) {
        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId != null ? tenantId : DEFAULT_TENANT_ID);
        vendor.setUserId(userId);
        vendor.setLegalName(legalName); vendor.setContactName(contactName);
        vendor.setPhone(phone); vendor.setWebsite(website);
        vendor.setAddress(address); vendor.setCity(city); vendor.setCountry(country);
        vendor.setVendorTypes(vendorTypes != null ? vendorTypes : List.of());
        vendor.setCategoryGroupIds(categoryGroupIds != null ? categoryGroupIds : List.of());
        vendor.setCapabilityIds(capabilityIds != null ? capabilityIds : List.of());
        vendor.setProjectTypes(projectTypes != null ? projectTypes : List.of());
        vendor.setYearsInBusiness(yearsInBusiness);
        vendor.setDescription(description); vendor.setLogoUrl(logoUrl);
        vendor.setRegistrationNumber(registrationNumber);
        vendor.setRegions(regions != null ? regions : List.of());
        vendor.setCategories(categories != null ? categories : List.of());
        vendor.setStatus("pending");
        vendor = vendorRepository.save(vendor);
        VendorRegistrationEvent event = new VendorRegistrationEvent();
        event.setVendorId(vendor.getId()); event.setEmail(email); event.setPassword(password);
        event.setLegalName(legalName); event.setContactName(contactName);
        event.setPhone(phone); event.setWebsite(website);
        event.setAddress(address); event.setCity(city); event.setCountry(country);
        event.setRegion(region); event.setTenantName(tenantName);
        event.setVendorTypes(vendorTypes != null ? vendorTypes : List.of());
        event.setCategoryGroupIds(categoryGroupIds != null ? categoryGroupIds : List.of());
        event.setCapabilityIds(capabilityIds != null ? capabilityIds : List.of());
        event.setProjectTypes(projectTypes != null ? projectTypes : List.of());
        event.setYearsInBusiness(yearsInBusiness); event.setDescription(description);
        event.setLogoUrl(logoUrl); event.setRegistrationNumber(registrationNumber);
        event.setRegions(regions != null ? regions : List.of());
        event.setCategories(categories != null ? categories : List.of());
        event.setTimestamp(OffsetDateTime.now());
        Message<VendorRegistrationEvent> message = MessageBuilder.withPayload(event)
                .setHeader("vendorId", vendor.getId().toString()).build();
        try {
            kafkaTemplate.send(vendorRegistrationTopic, message);
            log.info("Vendor registration event published for vendor: {}", vendor.getId());
        } catch (Exception e) {
            log.warn("Failed to publish vendor registration event for vendor: {}, error: {}",
                    vendor.getId(), e.getMessage());
        }
        return vendor;
    }

    @Override @Transactional
    public Vendor linkIdentity(UUID vendorId, UUID tenantId, UUID userId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        vendor.setTenantId(tenantId); vendor.setUserId(userId);
        return vendorRepository.save(vendor);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Vendor> findById(UUID id) { return vendorRepository.findById(id); }
    @Override @Transactional(readOnly = true)
    public Optional<Vendor> findByTenantId(UUID tenantId) { return vendorRepository.findByTenantId(tenantId); }
    @Override @Transactional(readOnly = true)
    public Optional<Vendor> findByUserId(UUID userId) { return vendorRepository.findByUserId(userId); }
    @Override @Transactional(readOnly = true)
    public List<Vendor> findAll() { return vendorRepository.findAll(); }
    @Override @Transactional(readOnly = true)
    public List<Vendor> findByStatus(String status) { return vendorRepository.findByStatus(status); }
    @Override @Transactional(readOnly = true)
    public List<Vendor> findByCategoriesContains(String category) { return vendorRepository.findByCategoriesContains(category); }

    @Override @Transactional
    public Vendor updateStatus(UUID id, String status) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));
        vendor.setStatus(status); vendor.onUpdate();
        return vendorRepository.save(vendor);
    }

    @Override @Transactional(readOnly = true)
    public List<CategoryGroupResponse> findAllGroupsWithActiveCounts() {
        return categoryGroupRepository.findAllGroupsWithActiveCounts().stream().map(row ->
                new CategoryGroupResponse((UUID) row[0], (String) row[1], (String) row[2],
                        (Integer) row[3], (Boolean) row[4], (Long) row[7])).toList();
    }

    @Override @Transactional
    public VendorCertificationResponse addCertification(UUID vendorId, CreateVendorCertificationRequest req) {
        vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        VendorCertification cert = new VendorCertification();
        cert.setVendorId(vendorId); cert.setName(req.name());
        cert.setIssuingBody(req.issuingBody()); cert.setIssueDate(req.issueDate());
        cert.setExpiryDate(req.expiryDate()); cert.setCertificateNumber(req.certificateNumber());
        cert.setDocumentUrl(req.documentUrl()); cert.setActive(true);
        cert = vendorCertificationRepository.save(cert);
        return new VendorCertificationResponse(cert.getId().toString(), cert.getVendorId().toString(),
                cert.getName(), cert.getIssuingBody(), cert.getIssueDate(), cert.getExpiryDate(),
                cert.getCertificateNumber(), cert.getDocumentUrl(), cert.getActive(),
                cert.getCreatedAt(), cert.getUpdatedAt());
    }

    @Override @Transactional
    public void removeCertification(UUID vendorId, UUID certId) {
        VendorCertification cert = vendorCertificationRepository.findById(certId)
                .orElseThrow(() -> new RuntimeException("Certification not found: " + certId));
        if (!cert.getVendorId().equals(vendorId))
            throw new RuntimeException("Certification does not belong to vendor");
        vendorCertificationRepository.delete(cert);
    }

    @Override @Transactional
    public VendorProjectResponse addProject(UUID vendorId, CreateVendorProjectRequest req) {
        vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        VendorProject project = new VendorProject();
        project.setVendorId(vendorId); project.setName(req.name());
        project.setDescription(req.description()); project.setProjectType(req.projectType());
        project.setStartDate(req.startDate()); project.setEndDate(req.endDate());
        project.setEstimatedValue(req.estimatedValue()); project.setImageUrl(req.imageUrl());
        project = vendorProjectRepository.save(project);
        return new VendorProjectResponse(project.getId().toString(), project.getVendorId().toString(),
                project.getName(), project.getDescription(), project.getProjectType(),
                project.getStartDate(), project.getEndDate(), project.getEstimatedValue(),
                project.getImageUrl(), project.getCreatedAt(), project.getUpdatedAt());
    }

    @Override @Transactional
    public void removeProject(UUID vendorId, UUID projectId) {
        VendorProject project = vendorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        if (!project.getVendorId().equals(vendorId))
            throw new RuntimeException("Project does not belong to vendor");
        vendorProjectRepository.delete(project);
    }
}
