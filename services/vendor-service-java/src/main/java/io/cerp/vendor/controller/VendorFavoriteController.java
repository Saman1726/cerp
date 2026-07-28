package io.cerp.vendor.controller;

import io.cerp.vendor.client.IdentityServiceClient;
import io.cerp.vendor.model.VendorFavorite;
import io.cerp.vendor.service.VendorFavoriteService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

    public VendorFavoriteController(
            VendorFavoriteService vendorFavoriteService,
            IdentityServiceClient identityServiceClient) {
        this.vendorFavoriteService = vendorFavoriteService;
        this.identityServiceClient = identityServiceClient;
    }

    /**
     * Get all favorited vendors for the current user.
     */
    @GetMapping("/my-vendors")
    public List<VendorFavorite> getMyVendors(JwtAuthenticationToken authentication) {
        UUID userId = extractUserId(authentication);
        return vendorFavoriteService.getMyFavorites(userId);
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
}
