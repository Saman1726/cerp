package io.cerp.catalog.api;

import io.cerp.catalog.dto.CatalogItemResponse;
import io.cerp.catalog.dto.CreatePriceRequest;
import io.cerp.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(path = "/api/catalog/prices")
public class CatalogPriceController {

    private final CatalogService service;

    public CatalogPriceController(CatalogService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemResponse create(@Valid @RequestBody CreatePriceRequest request) {
        return service.createPrice(request);
    }
}
