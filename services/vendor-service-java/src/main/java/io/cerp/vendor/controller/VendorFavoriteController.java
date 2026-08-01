package io.cerp.vendor.controller;

import io.cerp.vendor.client.IdentityServiceClient;
import io.cerp.vendor.entity.Vendor;
import io.cerp.vendor.model.VendorFavorite;
import io.cerp.vendor.service.VendorFavoriteService;
import io.cerp.vendor.service.VendorService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vendors")
public class VendorFavoriteController {

    private final VendorFavoriteService vendorFavoriteService;
    private final IdentityServiceClient identityServiceClient;
    private final VendorService vendorService;

    public VendorFavoriteController(
            VendorFavoriteService vendorFavoriteService,
            IdentityServiceClient identityServiceClient,
            VendorService vendorService) {
        this.vendorFavoriteService = vendorFavoriteService;
        this.identityServiceClient = identityServiceClient;
        this.vendorService = vendorService;
    }

    /**
     * Get all favorited vendors for the current user with full vendor details.
     */
    @GetMapping("/my-vendors")
    public List<VendorFavoriteWithDetails> getMyVendorsWithDetails(JwtAuthenticationToken authentication) {
        UUID userId = extractUserId(authentication);
        List<VendorFavorite> favorites = vendorFavoriteService.getMyFavorites(userId);

        return favorites.stream()
            .map(fav -> {
                Optional<Vendor> vendorOpt = vendorService.findById(fav.getVendorId());
                if (vendorOpt.isPresent()) {
                    Vendor vendor = vendorOpt.get();
                    VendorFavoriteWithDetails details = new VendorFavoriteWithDetails();
                    details.setVendorId(vendor.getId());
                    details.setTenantId(vendor.getTenantId());
                    details.setLegalName(vendor.getLegalName());
                    details.setContactName(vendor.getContactName());
                    details.setPhone(vendor.getPhone());
                    details.setWebsite(vendor.getWebsite());
                    details.setAddress(vendor.getAddress());
                    details.setCity(vendor.getCity());
                    details.setCountry(vendor.getCountry());
                    details.setStatus(vendor.getStatus());
                    details.setCategories(vendor.getCategories());
                    details.setCreatedAt(vendor.getCreatedAt());
                    details.setUpdatedAt(vendor.getUpdatedAt());
                    return details;
                }
                return null;
            })
            .filter(v -> v != null)
            .collect(Collectors.toList());
    }

    /**
     * Get favorite vendor IDs for the current user (used to mark favorites in search results).
     */
    @GetMapping("/my-vendor-ids")
    public List<UUID> getMyVendorIds(JwtAuthenticationToken authentication) {
        UUID userId = extractUserId(authentication);
        return vendorFavoriteService.getFavoriteVendorIds(userId);
    }

    /**
     * Add a vendor to favorites.
     */
    @PostMapping("/{vendorId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public VendorFavorite addFavorite(
            @PathVariable UUID vendorId,
            JwtAuthenticationToken authentication) {
        UUID userId = extractUserId(authentication);
        return vendorFavoriteService.addFavorite(userId, vendorId);
    }

    /**
     * Remove a vendor from favorites.
     */
    @DeleteMapping("/{vendorId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @PathVariable UUID vendorId,
            JwtAuthenticationToken authentication) {
        UUID userId = extractUserId(authentication);
        vendorFavoriteService.removeFavorite(userId, vendorId);
    }

    private UUID extractUserId(JwtAuthenticationToken authentication) {
        if (authentication == null || authentication.getToken() == null) {
            throw new IllegalStateException("Authentication token is required");
        }
        String sub = authentication.getToken().getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalStateException("User ID not found in token");
        }

        // Try parsing as UUID first (for tokens with UUID sub)
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            // sub is not a UUID (e.g., it's an email). Look up user ID from identity service.
            if (identityServiceClient != null) {
                return identityServiceClient.resolveUserIdByEmail(sub);
            }
            throw new IllegalStateException(
                "Invalid UUID in token sub claim and IdentityServiceClient is not available. sub=" + sub);
        }
    }

    /**
     * Vendor favorite with full vendor details.
     */
    public static class VendorFavoriteWithDetails {
        private UUID vendorId;
        private UUID tenantId;
        private String legalName;
        private String contactName;
        private String phone;
        private String website;
        private String address;
        private String city;
        private String country;
        private String status;
        private List<String> categories;
        private java.time.OffsetDateTime createdAt;
        private java.time.OffsetDateTime updatedAt;

        public UUID getVendorId() {
            return vendorId;
        }

        public void setVendorId(UUID vendorId) {
            this.vendorId = vendorId;
        }

        public UUID getTenantId() {
            return tenantId;
        }

        public void setTenantId(UUID tenantId) {
            this.tenantId = tenantId;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getCategories() {
            return categories;
        }

        public void setCategories(List<String> categories) {
            this.categories = categories;
        }

        public java.time.OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public java.time.OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(java.time.OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
