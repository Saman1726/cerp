package io.cerp.core.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase-orders")
public class ProcurementController {

    private final ProcurementService service;

    public ProcurementController(ProcurementService service) {
        this.service = service;
    }

    @GetMapping
    public List<PurchaseOrderWithReceiptsResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrderWithReceiptsResponse findOne(@PathVariable String id) {
        return service.findOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse raise(@Valid @RequestBody RaisePurchaseOrderRequest req) {
        return service.raise(req);
    }

    @PostMapping("/{id}/receipts")
    public PurchaseOrderWithReceiptsResponse recordGoodsReceipt(
            @PathVariable String id,
            @Valid @RequestBody RecordGoodsReceiptRequest req) {
        return service.recordGoodsReceipt(id, req);
    }
}

/**
 * In-memory implementation to mirror NestJS procurement endpoints. This includes a minimal Party Registry stub to validate vendor status until partner-system-java is
 * wired.
 */
@Service
class ProcurementService {

    private final Map<String, PurchaseOrderEntity> purchaseOrders = new ConcurrentHashMap<>();
    private final Map<String, List<GoodsReceiptEntity>> receiptsByPo = new ConcurrentHashMap<>();
    private final PartyRegistryGateway partyRegistry = new InMemoryPartyRegistry();

    public List<PurchaseOrderWithReceiptsResponse> findAll() {
        return purchaseOrders.values().stream()
            .sorted(Comparator.comparing(PurchaseOrderEntity::orderedAt).reversed())
            .map(this::toWithReceiptsResponse)
            .collect(Collectors.toList());
    }

    public PurchaseOrderWithReceiptsResponse findOne(String id) {
        PurchaseOrderEntity po = purchaseOrders.get(id);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order " + id + " not found");
        }
        return toWithReceiptsResponse(po);
    }

    public PurchaseOrderResponse raise(RaisePurchaseOrderRequest req) {
        // Validate projectId as UUID
        if (!isUuid(req.projectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid projectId");
        }

        PartyRegistryEntry vendor = partyRegistry.findById(req.vendorPartnerId());
        if (vendor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor not found in the partner directory");
        }
        if (!"active".equals(vendor.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor is not active");
        }

        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        PurchaseOrderEntity entity = new PurchaseOrderEntity(
            id,
            req.projectId(),
            vendor.id(),
            vendor.name(),
            req.description(),
            "ordered",
            now,
            now);
        purchaseOrders.put(id, entity);
        // No receipts initially
        return toResponse(entity);
    }

    public PurchaseOrderWithReceiptsResponse recordGoodsReceipt(String purchaseOrderId, RecordGoodsReceiptRequest req) {
        PurchaseOrderEntity po = purchaseOrders.get(purchaseOrderId);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order " + purchaseOrderId + " not found");
        }

        Instant now = Instant.now();
        GoodsReceiptEntity gr = new GoodsReceiptEntity(
            UUID.randomUUID().toString(),
            purchaseOrderId,
            req.notes(),
            now);
        receiptsByPo.computeIfAbsent(purchaseOrderId, k -> new ArrayList<>()).add(gr);

        // Update PO status to 'received'
        PurchaseOrderEntity updated = new PurchaseOrderEntity(
            po.id(),
            po.projectId(),
            po.vendorPartnerId(),
            po.vendorPartnerName(),
            po.description(),
            "received",
            po.orderedAt(),
            now);
        purchaseOrders.put(purchaseOrderId, updated);

        return toWithReceiptsResponse(updated);
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private PurchaseOrderResponse toResponse(PurchaseOrderEntity po) {
        return new PurchaseOrderResponse(
            po.id(),
            po.projectId(),
            po.vendorPartnerId(),
            po.vendorPartnerName(),
            po.description(),
            po.status(),
            DateTimeFormatter.ISO_INSTANT.format(po.orderedAt()),
            DateTimeFormatter.ISO_INSTANT.format(po.updatedAt()));
    }

    private PurchaseOrderWithReceiptsResponse toWithReceiptsResponse(PurchaseOrderEntity po) {
        List<GoodsReceiptEntity> list = receiptsByPo.getOrDefault(po.id(), List.of());
        List<GoodsReceiptResponse> receipts = list.stream()
            .sorted(Comparator.comparing(GoodsReceiptEntity::receivedAt))
            .map(gr -> new GoodsReceiptResponse(
                gr.id(),
                gr.purchaseOrderId(),
                gr.notes(),
                DateTimeFormatter.ISO_INSTANT.format(gr.receivedAt())))
            .collect(Collectors.toList());

        return new PurchaseOrderWithReceiptsResponse(
            po.id(),
            po.projectId(),
            po.vendorPartnerId(),
            po.vendorPartnerName(),
            po.description(),
            po.status(),
            DateTimeFormatter.ISO_INSTANT.format(po.orderedAt()),
            DateTimeFormatter.ISO_INSTANT.format(po.updatedAt()),
            receipts);
    }
}

// DTOs mirror packages/shared-types/src/procurement.ts
record RaisePurchaseOrderRequest(
        @NotBlank String projectId,
        @NotBlank String vendorPartnerId,
        @NotBlank @Size(min = 1, max = 500) String description) {
}

record RecordGoodsReceiptRequest(
        @Size(max = 2000) String notes) {
}

// Responses mirror shared-types
record PurchaseOrderResponse(
        String id,
        String projectId,
        String vendorPartnerId,
        String vendorPartnerName,
        String description,
        String status,
        String orderedAt,
        String updatedAt) {
}

record GoodsReceiptResponse(
        String id,
        String purchaseOrderId,
        String notes,
        String receivedAt) {
}

record PurchaseOrderWithReceiptsResponse(
        String id,
        String projectId,
        String vendorPartnerId,
        String vendorPartnerName,
        String description,
        String status,
        String orderedAt,
        String updatedAt,
        List<GoodsReceiptResponse> receipts) {
}

// Internal entities
record PurchaseOrderEntity(
        String id,
        String projectId,
        String vendorPartnerId,
        String vendorPartnerName,
        String description,
        String status,
        Instant orderedAt,
        Instant updatedAt) {
}

record GoodsReceiptEntity(
        String id,
        String purchaseOrderId,
        String notes,
        Instant receivedAt) {
}

// Minimal Party Registry gateway and stub implementation to validate vendors
interface PartyRegistryGateway {
    PartyRegistryEntry findById(String id);
}

record PartyRegistryEntry(
        String id,
        String name,
        String status // "active" | "inactive"
) {
}

class InMemoryPartyRegistry implements PartyRegistryGateway {
    private final Map<String, PartyRegistryEntry> partners = new HashMap<>();

    InMemoryPartyRegistry() {
        partners.put("fake-partner-active", new PartyRegistryEntry("fake-partner-active", "Fake Partner Active", "active"));
        partners.put("fake-partner-inactive", new PartyRegistryEntry("fake-partner-inactive", "Fake Partner Inactive", "inactive"));
        // Add more as needed or wire HTTP client once partner-system-java is available.
    }

    @Override
    public PartyRegistryEntry findById(String id) {
        return partners.get(id);
    }
}
