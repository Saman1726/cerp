package io.cerp.vendor.api;

import io.cerp.vendor.dto.AddRatingRequest;
import io.cerp.vendor.dto.CreateVendorRequest;
import io.cerp.vendor.dto.UpdateVendorRequest;
import io.cerp.vendor.dto.VendorResponse;
import io.cerp.vendor.service.VendorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(path = "/api/vendors")
public class VendorController {

    private final VendorService service;

    public VendorController(VendorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorResponse create(@Valid @RequestBody CreateVendorRequest request) {
        return service.createVendor(request);
    }

    @PatchMapping("/{id}")
    public VendorResponse update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateVendorRequest request) {
        return service.updateVendor(id, request);
    }

    @PostMapping("/{id}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public VendorResponse addRating(@PathVariable("id") UUID id, @Valid @RequestBody AddRatingRequest request) {
        return service.addRating(id, request);
    }

    @GetMapping("/{id}")
    public VendorResponse get(@PathVariable("id") UUID id) {
        return service.getVendor(id);
    }

    @GetMapping
    public List<VendorResponse> list(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "city", required = false) String city) {
        return service.listVendors(category, name, city);
    }
}
