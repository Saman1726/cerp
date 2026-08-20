package io.cerp.catalog.api;

import io.cerp.catalog.dto.CreateMaterialServiceRequest;
import io.cerp.catalog.dto.CreatePriceTableRequest;
import io.cerp.catalog.dto.MaterialServiceResponse;
import io.cerp.catalog.dto.PriceTableResponse;
import io.cerp.catalog.dto.UpdateMaterialServiceRequest;
import io.cerp.catalog.dto.UpdatePriceTableRequest;
import io.cerp.catalog.service.MaterialServiceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(path = "/api/catalog/material-services")
public class MaterialServiceController {

    private final MaterialServiceService service;

    public MaterialServiceController(MaterialServiceService service) {
        this.service = service;
    }

    // ==================== Material/Service CRUD ====================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialServiceResponse createMaterialService(@Valid @RequestBody CreateMaterialServiceRequest request) {
        return service.createMaterialService(request);
    }

    @PatchMapping("/{id}")
    public MaterialServiceResponse updateMaterialService(@PathVariable("id") UUID id, @Valid @RequestBody UpdateMaterialServiceRequest request) {
        return service.updateMaterialService(id, request);
    }

    @GetMapping("/{id}")
    public MaterialServiceResponse getMaterialService(@PathVariable("id") UUID id) {
        return service.getMaterialService(id);
    }

    @GetMapping
    public List<MaterialServiceResponse> listMaterialServices(@RequestParam(name = "type", required = false) String type) {
        if (type != null && !type.isEmpty()) {
            return service.listMaterialServicesByType(type);
        }
        return service.listMaterialServices();
    }

    @GetMapping("/search")
    public List<MaterialServiceResponse> searchMaterials(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "q") String query) {
        return service.searchMaterials(type, query);
    }

    // ==================== Price Table CRUD ====================

    @PostMapping("/{materialServiceId}/prices")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceTableResponse createPriceTable(
            @PathVariable("materialServiceId") UUID materialServiceId,
            @Valid @RequestBody CreatePriceTableRequest request) {
        // Override the materialServiceId in the request with the path variable
        request.setMaterialServiceId(materialServiceId);
        return service.createPriceTable(request);
    }

    @PatchMapping("/prices/{priceId}")
    public PriceTableResponse updatePriceTable(@PathVariable("priceId") UUID priceId, @Valid @RequestBody UpdatePriceTableRequest request) {
        return service.updatePriceTable(priceId, request);
    }

    @GetMapping("/prices/{priceId}")
    public PriceTableResponse getPriceTable(@PathVariable("priceId") UUID priceId) {
        return service.getPriceTable(priceId);
    }

    @GetMapping("/{materialServiceId}/prices")
    public List<PriceTableResponse> listPriceTablesByMaterialService(@PathVariable("materialServiceId") UUID materialServiceId) {
        return service.listPriceTablesByMaterialService(materialServiceId);
    }

    @GetMapping("/prices")
    public List<PriceTableResponse> listAllPriceTables() {
        return service.listPriceTables();
    }
}