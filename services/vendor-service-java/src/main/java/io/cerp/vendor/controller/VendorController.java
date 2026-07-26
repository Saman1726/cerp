package io.cerp.vendor.controller;

import io.cerp.vendor.entity.Vendor;
import io.cerp.vendor.service.VendorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
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

    public record UpdateStatusRequest(@NotBlank String status) {
    }
}
