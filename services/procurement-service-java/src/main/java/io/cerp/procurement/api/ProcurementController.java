package io.cerp.procurement.api;

import io.cerp.procurement.dto.CreateProcurementRequest;
import io.cerp.procurement.dto.ProcurementRequestResponse;
import io.cerp.procurement.dto.UpdateProcurementRequest;
import io.cerp.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for procurement requests.
 */
@Validated
@RestController
@RequestMapping(path = "/api/procurement/requests")
public class ProcurementController {

    private final ProcurementService service;

    public ProcurementController(ProcurementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementRequestResponse create(@Valid @RequestBody CreateProcurementRequest request) {
        return service.createRequest(request);
    }

    @PatchMapping("/{id}")
    public ProcurementRequestResponse update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateProcurementRequest request) {
        return service.updateRequest(id, request);
    }

    @GetMapping("/{id}")
    public ProcurementRequestResponse get(@PathVariable("id") UUID id) {
        return service.getRequest(id);
    }

    @GetMapping
    public List<ProcurementRequestResponse> list(@RequestParam(name = "status", required = false) String status) {
        return service.listRequests(status);
    }
}
