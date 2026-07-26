package io.cerp.identity.api;

import io.cerp.identity.domain.Tenant;
import io.cerp.identity.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;

    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public static record CreateTenantRequest(@NotBlank String name, String region) {
    }

    public static record TenantResponse(UUID id, String name, String region, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    private static TenantResponse toResponse(Tenant t) {
        return new TenantResponse(t.getId(), t.getName(), t.getRegion(), t.getCreatedAt(), t.getUpdatedAt());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest body) {
        Tenant created = tenantService.create(body.name(), body.region);
        return toResponse(created);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tenantService.delete(id);
    }

    @GetMapping("/{id}")
    public TenantResponse getById(@PathVariable UUID id) {
        Optional<Tenant> t = tenantService.findById(id);
        return t.map(TenantController::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.findAll().stream().map(TenantController::toResponse).toList();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) {
            super(msg);
        }
    }
}
