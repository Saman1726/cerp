package io.cerp.vendor.controller;

import io.cerp.vendor.entity.Vendor;
import io.cerp.vendor.service.VendorService;
import io.cerp.vendor.security.TenantContext;
import io.cerp.vendor.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorService vendorService;

    @Autowired
    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    public record CreateVendorRequest(
            String email,
            String password,
            String legalName,
            String contactName,
            String phone,
            String website,
            List<String> categories,
            String address,
            String city,
            String country,
            String region,
            String tenantName,
            UUID tenantId,
            UUID userId) {
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
            body.categories());
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
            List<String> categoryIds) {
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
