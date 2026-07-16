package io.cerp.procurement.service;

import io.cerp.procurement.dto.CreateProcurementRequest;
import io.cerp.procurement.dto.ProcurementRequestResponse;
import io.cerp.procurement.dto.UpdateProcurementRequest;
import io.cerp.procurement.events.ProcurementEventsPublisher;
import io.cerp.procurement.model.ProcurementRequest;
import io.cerp.procurement.repo.ProcurementRequestRepository;
import io.cerp.procurement.security.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProcurementServiceImpl implements ProcurementService {

    private final ProcurementRequestRepository repository;
    private final ProcurementEventsPublisher events;

    public ProcurementServiceImpl(ProcurementRequestRepository repository, ProcurementEventsPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Override
    @Transactional
    public ProcurementRequestResponse createRequest(CreateProcurementRequest req) {
        UUID tenantId = requireTenant();

        ProcurementRequest pr = new ProcurementRequest();
        // id and timestamps handled by entity callbacks, but explicit id is also fine
        pr.setId(UUID.randomUUID());
        pr.setTenantId(tenantId);
        pr.setTitle(req.getTitle());
        pr.setDescription(req.getDescription());
        // default status is "draft" by entity field

        ProcurementRequest saved = repository.save(pr);
        ProcurementRequestResponse resp = ProcurementRequestResponse.from(saved);
        events.publishRequestEvent("ProcurementRequestCreated", resp);
        return resp;
    }

    @Override
    @Transactional
    public ProcurementRequestResponse updateRequest(UUID id, UpdateProcurementRequest req) {
        requireTenant(); // ensure tenant is present so RLS aspect can tag connection

        ProcurementRequest pr = repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        String oldStatus = pr.getStatus();

        if (req.getTitle() != null) {
            pr.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            pr.setDescription(req.getDescription());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            pr.setStatus(req.getStatus());
        }

        ProcurementRequest saved = repository.save(pr);
        ProcurementRequestResponse resp = ProcurementRequestResponse.from(saved);

        // Emit more specific event when status transitions to a terminal/important state; otherwise generic update
        String eventType = "ProcurementRequestUpdated";
        if (req.getStatus() != null && !req.getStatus().isBlank() && !req.getStatus().equalsIgnoreCase(oldStatus)) {
            String newStatus = req.getStatus().trim().toLowerCase();
            switch (newStatus) {
                case "submitted":
                    eventType = "ProcurementRequestSubmitted";
                    break;
                case "approved":
                    eventType = "ProcurementRequestApproved";
                    break;
                case "ordered":
                    eventType = "ProcurementRequestOrdered";
                    break;
                case "closed":
                    eventType = "ProcurementRequestClosed";
                    break;
                default:
                    eventType = "ProcurementRequestStatusChanged";
                    break;
            }
        }

        events.publishRequestEvent(eventType, resp);
        return resp;
    }

    @Override
    public ProcurementRequestResponse getRequest(UUID id) {
        requireTenant();
        ProcurementRequest pr = repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        return ProcurementRequestResponse.from(pr);
    }

    @Override
    public List<ProcurementRequestResponse> listRequests(String status) {
        requireTenant();
        List<ProcurementRequest> items = (status == null || status.isBlank())
                ? repository.findAll()
                : repository.findByStatus(status);
        List<ProcurementRequestResponse> out = new ArrayList<>(items.size());
        for (ProcurementRequest pr : items) {
            out.add(ProcurementRequestResponse.from(pr));
        }
        return out;
    }

    private UUID requireTenant() {
        return TenantContext
            .get()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant id is required"));
    }
}
