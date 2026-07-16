package io.cerp.core.constructionsites;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Temporary compatibility alias to support existing frontend that calls /properties. Delegates to ConstructionSitesService and maps to a minimal Property shape: id,
 * name, address, createdAt, updatedAt. This should be removed after the frontend migrates to /construction-sites and richer domain types.
 */
@RestController
@RequestMapping("/properties")
class PropertiesAliasController {

    private final ConstructionSitesService sites;

    PropertiesAliasController(ConstructionSitesService sites) {
        this.sites = sites;
    }

    @GetMapping
    public List<PropertyAliasResponse> findAll() {
        return sites.listSites().stream().map(PropertiesAliasController::toProperty).toList();
    }

    @GetMapping("/{id}")
    public PropertyAliasResponse findOne(@PathVariable String id) {
        return toProperty(sites.getSite(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyAliasResponse create(@Valid @RequestBody CreatePropertyAliasRequest req) {
        ConstructionSiteResponse site = sites.createSite(new CreateConstructionSiteRequest(req.name(), req.address()));
        return toProperty(site);
    }

    @PatchMapping("/{id}")
    public PropertyAliasResponse update(@PathVariable String id, @Valid @RequestBody UpdatePropertyAliasRequest req) {
        ConstructionSiteResponse site = sites.updateSite(id, new UpdateConstructionSiteRequest(req.name(), req.address()));
        return toProperty(site);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        sites.deleteSite(id);
    }

    private static PropertyAliasResponse toProperty(ConstructionSiteResponse s) {
        return new PropertyAliasResponse(s.id(), s.name(), s.address(), s.createdAt(), s.updatedAt());
    }
}

// Minimal DTOs matching packages/shared-types/src/properties.ts
record CreatePropertyAliasRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotBlank @Size(min = 1, max = 500) String address) {
}

record UpdatePropertyAliasRequest(
        @Size(min = 1, max = 200) String name,
        @Size(min = 1, max = 500) String address) {
}

record PropertyAliasResponse(
        String id,
        String name,
        String address,
        String createdAt,
        String updatedAt) {
}
