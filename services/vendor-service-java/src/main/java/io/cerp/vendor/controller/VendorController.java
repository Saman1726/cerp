package io.cerp.vendor.controller;

import io.cerp.vendor.dto.CategoryGroupResponse;
import io.cerp.vendor.entity.Vendor;
import io.cerp.vendor.service.CategoryService;
import io.cerp.vendor.service.VendorService;
import io.cerp.vendor.security.TenantContext;
import io.cerp.vendor.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final CategoryService categoryService;

    @Autowired
    public VendorController(VendorService vendorService, CategoryService categoryService) {
        this.vendorService = vendorService;
        this.categoryService = categoryService;
    }

    public record CreateVendorRequest(
            String email,
            String password,
            String legalName,
            String contactName,
            String phone,
            String website,
            List<String> vendorTypes,
            List<UUID> categoryGroupIds,
            List<UUID> capabilityIds,
            List<String> projectTypes,
            Integer yearsInBusiness,
            String description,
            String logoUrl,
            String registrationNumber,
            List<String> regions,
            String address,
            String city,
            String country,
            String region,
            String tenantName,
            UUID tenantId,
            UUID userId,
            // Deprecated — use capabilityIds instead
            @Deprecated List<String> categories) {
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
            List<String> vendorTypes,
            List<UUID> categoryGroupIds,
            List<UUID> capabilityIds,
            List<String> projectTypes,
            Integer yearsInBusiness,
            String description,
            String logoUrl,
            String registrationNumber,
            List<String> regions,
            String status,
            @Deprecated List<String> categories,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    private static VendorResponse toResponse(Vendor v) {
        return new VendorResponse(
            v.getId(),
            v.getTenantId(),
            v.getUserId(),
            v.getLegalName(),
            v.getContactName(),
            v.getPhone(),
            v.getWebsite(),
            v.getAddress(),
            v.getCity(),
            v.getCountry(),
            v.getVendorTypes(),
            v.getCategoryGroupIds(),
            v.getCapabilityIds(),
            v.getProjectTypes(),
            v.getYearsInBusiness(),
            v.getDescription(),
            v.getLogoUrl(),
            v.getRegistrationNumber(),
            v.getRegions(),
            v.getStatus(),
            v.getCategories(),
            v.getCreatedAt(),
            v.getUpdatedAt());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorResponse create(@Valid @RequestBody CreateVendorRequest body) {
        Vendor vendor = vendorService.register(
            body.email(),
            body.password(),
            body.legalName(),
            body.contactName(),
            body.phone(),
            body.website(),
            body.address(),
            body.city(),
            body.country(),
            body.region(),
            body.tenantName(),
            body.tenantId(),
            body.userId(),
            body.vendorTypes() != null ? body.vendorTypes() : List.of(),
            body.categoryGroupIds() != null ? body.categoryGroupIds() : List.of(),
            body.capabilityIds() != null ? body.capabilityIds() : List.of(),
            body.projectTypes() != null ? body.projectTypes() : List.of(),
            body.yearsInBusiness(),
            body.description(),
            body.logoUrl(),
            body.registrationNumber(),
            body.regions() != null ? body.regions() : List.of(),
            body.categories() != null ? body.categories() : List.of());
        return toResponse(vendor);
    }

    @GetMapping("/{id}")
    public VendorResponse getById(@PathVariable UUID id) {
        Vendor vendor = vendorService.findById(id)
            .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));
        return toResponse(vendor);
    }

    @GetMapping
    public List<VendorResponse> list(
            @RequestParam(value = "status", required = false) String status) {
        if (status != null) {
            return vendorService.findByStatus(status).stream()
                .map(VendorController::toResponse)
                .toList();
        }
        return vendorService.findAll().stream()
            .map(VendorController::toResponse)
            .toList();
    }

    @GetMapping("/tenant/{tenantId}")
    public VendorResponse getByTenant(@PathVariable UUID tenantId) {
        Vendor vendor = vendorService.findByTenantId(tenantId)
            .orElseThrow(() -> new RuntimeException("Vendor not found for tenant: " + tenantId));
        return toResponse(vendor);
    }

    @PatchMapping("/{id}")
    public VendorResponse updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        Vendor vendor = vendorService.updateStatus(id, request.status());
        return toResponse(vendor);
    }

    // ── Category Groups ──────────────────────────────────────────────

    @GetMapping("/groups")
    public List<CategoryGroupResponse> getGroups() {
        return vendorService.findAllGroupsWithActiveCounts();
    }

    // ── Vendor Certifications ────────────────────────────────────────

    @PostMapping("/{vendorId}/certifications")
    public io.cerp.vendor.dto.VendorCertificationResponse addCertification(
            @PathVariable UUID vendorId,
            @Valid @RequestBody io.cerp.vendor.dto.CreateVendorCertificationRequest req) {
        return vendorService.addCertification(vendorId, req);
    }

    @DeleteMapping("/{vendorId}/certifications/{certId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCertification(@PathVariable UUID vendorId, @PathVariable UUID certId) {
        vendorService.removeCertification(vendorId, certId);
    }

    // ── Vendor Projects ──────────────────────────────────────────────

    @PostMapping("/{vendorId}/projects")
    public io.cerp.vendor.dto.VendorProjectResponse addProject(
            @PathVariable UUID vendorId,
            @Valid @RequestBody io.cerp.vendor.dto.CreateVendorProjectRequest req) {
        return vendorService.addProject(vendorId, req);
    }

    @DeleteMapping("/{vendorId}/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProject(@PathVariable UUID vendorId, @PathVariable UUID projectId) {
        vendorService.removeProject(vendorId, projectId);
    }

    /**
     * Get the current vendor's profile based on the user ID from the request context. The
     * userId is extracted by TenantRequestFilter from the X-User-Id header (or the JWT's
     * "internal_user_id" claim). Looking this up by tenantId instead would be wrong: tenant_id
     * on Vendor is a shared value today (and slated to represent a physical region later, not
     * an owner), so multiple vendors can - and do - share one tenant_id.
     */
    @GetMapping("/profile")
    public ResponseEntity<VendorProfileResponse> getVendorProfile() {
        UUID userId = UserContext.get().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Vendor vendor = vendorService.findByUserId(userId).orElse(null);

        if (vendor == null) {
            // Return empty profile - vendor hasn't created a profile yet
            vendor = new Vendor();
            vendor.setUserId(userId);
            return ResponseEntity.ok(toProfileResponse(vendor));
        }
        return ResponseEntity.ok(toProfileResponse(vendor));
    }

    @PostMapping("/profile")
    public ResponseEntity<VendorProfileResponse> postVendorProfile() {
        // Same logic as GET /profile - return the current vendor's profile
        UUID userId = UserContext.get().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Vendor vendor = vendorService.findByUserId(userId).orElse(null);

        if (vendor == null) {
            // Return empty profile - vendor hasn't created a profile yet
            vendor = new Vendor();
            vendor.setUserId(userId);
            return ResponseEntity.ok(toProfileResponse(vendor));
        }
        return ResponseEntity.ok(toProfileResponse(vendor));
    }

    /**
     * Update the current vendor's profile.
     */
    @PatchMapping("/profile")
    public ResponseEntity<VendorProfileResponse> updateVendorProfile(
            @RequestBody UpdateVendorProfileRequest request) {
        UUID userId = UserContext.get().orElse(null);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Vendor vendor = vendorService.findByUserId(userId).orElse(null);

        if (vendor == null) {
            // Create new vendor profile
            vendor = new Vendor();
            vendor.setUserId(userId);
            vendor.setTenantId(TenantContext.get().orElse(null));
            vendor.setLegalName(request.legalName() != null ? request.legalName() : "New Vendor");
            vendor.setCategories(List.of());
        }

        // Update fields - map frontend field names to Vendor entity fields
        if (request.legalName() != null)
            vendor.setLegalName(request.legalName());
        if (request.contactPerson() != null)
            vendor.setContactName(request.contactPerson());
        if (request.contactPhone() != null)
            vendor.setPhone(request.contactPhone());
        if (request.website() != null)
            vendor.setWebsite(request.website());
        if (request.addressLine1() != null)
            vendor.setAddress(request.addressLine1());
        if (request.city() != null)
            vendor.setCity(request.city());
        if (request.countryCode() != null)
            vendor.setCountry(request.countryCode());
        if (request.vendorTypes() != null)
            vendor.setVendorTypes(request.vendorTypes());
        if (request.categoryGroupIds() != null)
            vendor.setCategoryGroupIds(request.categoryGroupIds());
        if (request.capabilityIds() != null)
            vendor.setCapabilityIds(request.capabilityIds());
        if (request.projectTypes() != null)
            vendor.setProjectTypes(request.projectTypes());
        if (request.yearsInBusiness() != null)
            vendor.setYearsInBusiness(request.yearsInBusiness());
        if (request.description() != null)
            vendor.setDescription(request.description());
        if (request.logoUrl() != null)
            vendor.setLogoUrl(request.logoUrl());
        if (request.registrationNumber() != null)
            vendor.setRegistrationNumber(request.registrationNumber());
        if (request.regions() != null)
            vendor.setRegions(request.regions());

        vendor = vendorService.updateStatus(vendor.getId(), vendor.getStatus());
        return ResponseEntity.ok(toProfileResponse(vendor));
    }

    private VendorProfileResponse toProfileResponse(Vendor v) {
        return new VendorProfileResponse(
            v.getId() != null ? v.getId().toString() : null,
            v.getId() != null ? v.getId().toString() : UUID.randomUUID().toString(),
            v.getLegalName(),
            getCompanyName(v),
            v.getContactName(),
            null, // contactEmail - not stored in Vendor entity yet
            v.getPhone(),
            v.getWebsite(),
            getAddressLine1(v),
            getAddressLine2(v),
            null, // postalCode - not stored yet
            v.getCity(),
            v.getCountry(),
            v.getVendorTypes(),
            v.getCategoryGroupIds(),
            v.getCapabilityIds(),
            v.getProjectTypes(),
            v.getYearsInBusiness(),
            v.getDescription(),
            v.getLogoUrl(),
            v.getRegistrationNumber(),
            v.getRegions(),
            v.getTaxProfile(),
            getCategoriesForProfile(v),
            v.getStatus() != null ? v.getStatus() : "pending",
            null, // ratingAvg - not stored yet
            v.getCreatedAt(),
            v.getUpdatedAt());
    }

    private String getCompanyName(Vendor v) {
        // For now, return legalName as fallback
        return v.getLegalName();
    }

    private String getAddressLine1(Vendor v) {
        return v.getAddress();
    }

    private String getAddressLine2(Vendor v) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCategoriesForProfile(Vendor v) {
        List<UUID> capIds = v.getCapabilityIds();
        if (capIds == null || capIds.isEmpty()) {
            List<String> cats = v.getCategories();
            if (cats == null || cats.isEmpty()) {
                return List.of();
            }
            return cats.stream().map(cat -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("categoryId", cat);
                m.put("categoryName", cat);
                m.put("categoryCode", cat);
                return m;
            }).toList();
        }
        // Resolve capability IDs to category details
        return capIds.stream().map(capId -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("categoryId", capId.toString());
            m.put("categoryName", capId.toString());
            m.put("categoryCode", capId.toString());
            return m;
        }).toList();
    }

    public record UpdateVendorProfileRequest(
            String legalName,
            String companyName,
            String contactPerson,
            String contactEmail,
            String contactPhone,
            String website,
            String addressLine1,
            String addressLine2,
            String postalCode,
            String city,
            String countryCode,
            List<String> vendorTypes,
            List<UUID> categoryGroupIds,
            List<UUID> capabilityIds,
            List<String> projectTypes,
            Integer yearsInBusiness,
            String description,
            String logoUrl,
            String registrationNumber,
            List<String> regions,
            @Deprecated List<String> categoryIds) {
    }

    public record VendorProfileResponse(
            String id,
            String vendorId,
            String legalName,
            String companyName,
            String contactPerson,
            String contactEmail,
            String contactPhone,
            String website,
            String addressLine1,
            String addressLine2,
            String postalCode,
            String city,
            String countryCode,
            List<String> vendorTypes,
            List<UUID> categoryGroupIds,
            List<UUID> capabilityIds,
            List<String> projectTypes,
            Integer yearsInBusiness,
            String description,
            String logoUrl,
            String registrationNumber,
            List<String> regions,
            Object taxProfile,
            List<Map<String, Object>> categories,
            String status,
            Double ratingAvg,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }
}
