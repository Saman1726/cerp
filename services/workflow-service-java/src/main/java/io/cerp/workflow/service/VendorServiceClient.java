package io.cerp.workflow.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Client for resolving vendor information via the vendor-service-java microservice.
 * Used to map vendor IDs to user IDs for notification delivery.
 */
@Service
public class VendorServiceClient {

    private static final Logger log = LoggerFactory.getLogger(VendorServiceClient.class);

    private final RestClient restClient;

    public VendorServiceClient(
            @Value("${cerp.vendor-service.url:http://localhost:8085}") String vendorServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(vendorServiceUrl).build();
    }

    /**
     * Get vendor information by vendor ID.
     *
     * @param vendorId the vendor UUID
     * @param tenantId forwarded as X-Tenant-Id so the vendor service sets its tenant context
     * @return vendor info containing the userId, or null if not found
     */
    public VendorInfo getVendorById(UUID vendorId, String tenantId) {
        try {
            VendorResponse response = restClient.get()
                    .uri("/vendors/{id}", vendorId)
                    .header("X-Tenant-Id", tenantId != null ? tenantId : "")
                    .retrieve()
                    .body(VendorResponse.class);
            if (response != null) {
                log.debug("Resolved vendor: vendorId={}, userId={}, vendorTenantId={}", vendorId, response.userId(), response.tenantId());
                return new VendorInfo(response.id(), response.userId(), response.tenantId(), response.legalName());
            }
            log.warn("Vendor not found: {}", vendorId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to resolve vendor by id {}: {}", vendorId, e.getMessage());
            return null;
        }
    }

    /**
     * Resolve user IDs for a list of vendor IDs.
     *
     * @param vendorIds the vendor UUIDs
     * @param tenantId  forwarded as X-Tenant-Id to the vendor service
     * @return list of (vendorId, userId) pairs for vendors that were found
     */
    public List<VendorUserIdMapping> resolveUserIds(List<UUID> vendorIds, String tenantId) {
        return vendorIds.stream()
                .map(id -> getVendorById(id, tenantId))
                .filter(info -> info != null && info.userId() != null)
                .map(info -> new VendorUserIdMapping(info.vendorId(), info.userId(), info.vendorTenantId()))
                .toList();
    }

    public record VendorResponse(
            UUID id,
            UUID tenantId,
            UUID userId,
            String legalName,
            String contactName,
            String phone,
            String website,
            String address,
            String city,
            String country,
            String status,
            List<String> categories,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record VendorInfo(UUID vendorId, UUID userId, UUID vendorTenantId, String legalName) {
    }

    public record VendorUserIdMapping(UUID vendorId, UUID userId, UUID vendorTenantId) {
    }
}