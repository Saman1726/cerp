package io.cerp.vendor.service;

import io.cerp.vendor.dto.CategoryGroupResponse;
import io.cerp.vendor.dto.CreateVendorCertificationRequest;
import io.cerp.vendor.dto.CreateVendorProjectRequest;
import io.cerp.vendor.dto.VendorCertificationResponse;
import io.cerp.vendor.dto.VendorProjectResponse;
import io.cerp.vendor.entity.Vendor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vendor service interface. Implementations handle vendor CRUD and registration workflows.
 */
public interface VendorService {

        // Legacy methods (for backward compatibility with existing model/repo)
        Vendor create(UUID tenantId, UUID userId, String legalName, String contactName,
                        String phone, String website, String address, String city,
                        String country, List<String> categories);

        Optional<Vendor> findById(UUID id);

        Optional<Vendor> findByTenantId(UUID tenantId);

        Optional<Vendor> findByUserId(UUID userId);

        List<Vendor> findAll();

        List<Vendor> findByStatus(String status);

        List<Vendor> findByCategoriesContains(String category);

        Vendor updateStatus(UUID id, String status);

        // New event-driven registration methods
        /**
         * Register a new external vendor. Creates a pending vendor record and publishes a Kafka event for identity-service to create login credentials (tenant, user,
         * membership with role="vendor").
         */
        Vendor register(String email, String password, String legalName, String contactName,
                        String phone, String website, String address, String city,
                        String country, String region, String tenantName, List<String> vendorTypes,
                        List<UUID> categoryGroupIds, List<UUID> capabilityIds,
                        List<String> projectTypes, Integer yearsInBusiness,
                        String description, String logoUrl, String registrationNumber,
                        List<String> regions);

        /**
         * Register a new external vendor with explicit tenantId and userId.
         */
        Vendor register(String email, String password, String legalName, String contactName,
                        String phone, String website, String address, String city,
                        String country, String region, String tenantName, UUID tenantId, UUID userId,
                        List<String> vendorTypes, List<UUID> categoryGroupIds,
                        List<UUID> capabilityIds, List<String> projectTypes,
                        Integer yearsInBusiness, String description, String logoUrl,
                        String registrationNumber, List<String> regions,
                        List<String> categories);

        /**
         * After identity creation, link the vendor record with tenantId and userId. Called by VendorIdentityCreatedConsumer when identity-service completes.
         */
        Vendor linkIdentity(UUID vendorId, UUID tenantId, UUID userId);

        // Category Groups
        List<CategoryGroupResponse> findAllGroupsWithActiveCounts();

        // Certifications
        VendorCertificationResponse addCertification(UUID vendorId, CreateVendorCertificationRequest req);

        void removeCertification(UUID vendorId, UUID certId);

        // Projects
        VendorProjectResponse addProject(UUID vendorId, CreateVendorProjectRequest req);

        void removeProject(UUID vendorId, UUID projectId);
}
