package io.cerp.catalog.api;

import io.cerp.catalog.dto.CatalogItemResponse;
import io.cerp.catalog.dto.CreateCatalogItemRequest;
import io.cerp.catalog.dto.UpdateCatalogItemRequest;
import io.cerp.catalog.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(path = "/api/catalog/items")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemResponse create(@Valid @RequestBody CreateCatalogItemRequest request) {
        return service.createItem(request);
    }

    @PatchMapping("/{id}")
    public CatalogItemResponse update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateCatalogItemRequest request) {
        return service.updateItem(id, request);
    }

    @GetMapping("/{id}")
    public CatalogItemResponse get(@PathVariable("id") UUID id) {
        return service.getItem(id);
    }

    @GetMapping
    public List<CatalogItemResponse> list(@RequestParam(name = "category", required = false) String category) {
        return service.listItems(category);
    }
}
