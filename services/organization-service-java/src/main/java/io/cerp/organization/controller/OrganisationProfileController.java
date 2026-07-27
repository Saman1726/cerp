package io.cerp.organization.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.cerp.organization.domain.Organization;
import io.cerp.organization.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for organisation profile endpoints.
 *
 * <p>
 * Handles /api/organisation/profile requests (gateway strips /api, leaving /organisation/profile).
 */
@RestController
@RequestMapping("/organisation")
public class OrganisationProfileController {

    private static final Logger log = LoggerFactory.getLogger(OrganisationProfileController.class);

    private final OrganizationRepository organizationRepository;

    public OrganisationProfileController(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Get the organisation profile for the current tenant.
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getOrganisationProfile(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        log.debug("Fetching organisation profile for tenant: {}", tenantId);

        Organization org = organizationRepository.findFirstByTenantIdAndStatus(tenantId, Organization.OrganizationStatus.ACTIVE)
            .orElse(null);

        if (org == null) {
            // Return empty profile structure
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", null);
            profile.put("tenantId", tenantId);
            profile.put("name", "");
            profile.put("legalName", "");
            profile.put("description", "");
            profile.put("industry", "");
            profile.put("companySize", "");
            profile.put("website", "");
            profile.put("addressLine1", "");
            profile.put("addressLine2", "");
            profile.put("postalCode", "");
            profile.put("city", "");
            profile.put("countryCode", "");
            profile.put("phone", "");
            profile.put("email", "");
            profile.put("taxId", "");
            profile.put("vatNumber", "");
            profile.put("registrationNumber", "");
            profile.put("avatarUrl", "");
            profile.put("createdAt", OffsetDateTime.now().toString());
            return ResponseEntity.ok(profile);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", org.getId().toString());
        profile.put("tenantId", tenantId);
        profile.put("name", org.getName());
        profile.put("legalName", org.getName()); // Use name as legalName fallback
        profile.put("description", ""); // Not stored in entity
        profile.put("industry", ""); // Not stored in entity
        profile.put("companySize", ""); // Not stored in entity
        profile.put("website", org.getWebsite());
        profile.put("addressLine1", org.getAddressLine1());
        profile.put("addressLine2", org.getAddressLine2());
        profile.put("postalCode", org.getPostalCode());
        profile.put("city", org.getCity());
        profile.put("countryCode", org.getCountryCode());
        profile.put("phone", org.getContactPhone());
        profile.put("email", org.getContactEmail());
        profile.put("taxId", org.getTaxId());
        profile.put("vatNumber", org.getTaxId()); // Use taxId as vatNumber fallback
        profile.put("registrationNumber", org.getCommercialRegister());
        profile.put("avatarUrl", ""); // Not stored in entity
        profile.put("createdAt", org.getCreatedAt().atOffset(ZoneOffset.UTC).toString());
        profile.put("updatedAt", org.getUpdatedAt().atOffset(ZoneOffset.UTC).toString());

        return ResponseEntity.ok(profile);
    }

    /**
     * Update the organisation profile for the current tenant.
     */
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateOrganisationProfile(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody Map<String, Object> updates) {
        log.debug("Updating organisation profile for tenant: {}", tenantId);

        Organization org = organizationRepository.findFirstByTenantIdAndStatus(tenantId, Organization.OrganizationStatus.ACTIVE)
            .orElse(null);

        if (org == null) {
            // Create new organization
            String name = (String) updates.get("name");
            if (name == null || name.isBlank()) {
                name = "New Organisation";
            }
            org = new Organization(tenantId, name, "EUR");
        }

        // Update fields from request
        if (updates.containsKey("name")) {
            String name = (String) updates.get("name");
            if (name != null && !name.isBlank())
                org.setName(name);
        }
        if (updates.containsKey("website")) {
            String website = (String) updates.get("website");
            if (website != null)
                org.setWebsite(website);
        }
        if (updates.containsKey("addressLine1")) {
            String addr = (String) updates.get("addressLine1");
            if (addr != null)
                org.setAddressLine1(addr);
        }
        if (updates.containsKey("addressLine2")) {
            String addr = (String) updates.get("addressLine2");
            if (addr != null)
                org.setAddressLine2(addr);
        }
        if (updates.containsKey("postalCode")) {
            String pc = (String) updates.get("postalCode");
            if (pc != null)
                org.setPostalCode(pc);
        }
        if (updates.containsKey("city")) {
            String city = (String) updates.get("city");
            if (city != null)
                org.setCity(city);
        }
        if (updates.containsKey("countryCode")) {
            String cc = (String) updates.get("countryCode");
            if (cc != null)
                org.setCountryCode(cc);
        }
        if (updates.containsKey("phone")) {
            String phone = (String) updates.get("phone");
            if (phone != null)
                org.setContactPhone(phone);
        }
        if (updates.containsKey("email")) {
            String email = (String) updates.get("email");
            if (email != null)
                org.setContactEmail(email);
        }
        if (updates.containsKey("taxId")) {
            String taxId = (String) updates.get("taxId");
            if (taxId != null)
                org.setTaxId(taxId);
        }
        if (updates.containsKey("registrationNumber")) {
            String reg = (String) updates.get("registrationNumber");
            if (reg != null)
                org.setCommercialRegister(reg);
        }

        org = organizationRepository.save(org);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", org.getId().toString());
        profile.put("tenantId", tenantId);
        profile.put("name", org.getName());
        profile.put("legalName", org.getName());
        profile.put("description", "");
        profile.put("industry", "");
        profile.put("companySize", "");
        profile.put("website", org.getWebsite());
        profile.put("addressLine1", org.getAddressLine1());
        profile.put("addressLine2", org.getAddressLine2());
        profile.put("postalCode", org.getPostalCode());
        profile.put("city", org.getCity());
        profile.put("countryCode", org.getCountryCode());
        profile.put("phone", org.getContactPhone());
        profile.put("email", org.getContactEmail());
        profile.put("taxId", org.getTaxId());
        profile.put("vatNumber", org.getTaxId());
        profile.put("registrationNumber", org.getCommercialRegister());
        profile.put("avatarUrl", "");
        profile.put("createdAt", org.getCreatedAt().atOffset(ZoneOffset.UTC).toString());
        profile.put("updatedAt", org.getUpdatedAt().atOffset(ZoneOffset.UTC).toString());

        return ResponseEntity.ok(profile);
    }
}
