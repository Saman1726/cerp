# Material Delivery and Discrepancy Resolution Workflow

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Vendor
    participant GW as API Gateway
    participant Del as Delivery Service
    participant Inv as Inventory Service
    participant WF as Workflow Service
    participant Fin as Financials Service

    %% Step 1: Schedule Delivery
    Vendor->>GW: POST /deliveries/{id}/schedule
    GW->>Del: ScheduleDelivery(deliveryId, eta, asn)
    Del->>Del: Validate(POExists, VendorApproved)
    Del->>Del: Emit DeliveryScheduled
    Del-->>GW: 201 Created

    %% Step 2: Dispatch Shipment
    Vendor->>GW: POST /deliveries/{id}/dispatch
    GW->>Del: DispatchDelivery(deliveryId)
    Del->>Del: Emit ShipmentDispatched
    Del-->>GW: 200 OK

    %% Step 3: Site Receipt Confirmation
    SiteEng->>GW: POST /deliveries/{id}/confirm (mobile)
    GW->>Del: ConfirmDelivery(deliveryId, quantities, photos, signature, geotag)
    Del->>Del: Validate(POExists, ETAWithinWindow)
    Del->>Del: Emit DeliveryConfirmed

    alt Quantity/Quality Discrepancy
        Del->>Del: Emit DiscrepancyRaised(shortage=qty, damage=true, type=SHORT/DAMAGE)
        Del->>WF: CreateDiscrepancyTask(deliveryId, vendorId, discrepancyType)
        WF->>WF: AssignTo(PM, escalationSLA=5days)
        WF-->>Del: TaskCreated(taskId)
        Note over Del,Fin: Discrepancy Resolution Phase
    else No Discrepancy
        Note over Del: Clean delivery
    end

    %% Step 4: Inventory Update
    Del->>Inv: StockReceived(deliveryId, confirmedQuantities)
    Inv->>Inv: UpdateStockLedger(deliveryId, quantities)
    Inv->>Inv: Emit StockReceived
    Inv-->>Del: StockUpdated

    alt Credit Note Needed
        Del->>Fin: CreditNoteRequested(deliveryId, discrepancyAmount)
        Fin->>Fin: ValidateDiscrepancy(deliveryId)
        Fin->>Fin: Emit CreditNoteIssued
        Fin-->>Del: CreditNoteApplied
    end

    %% Step 5: Resolve Discrepancy
    Del->>Del: Emit DiscrepancyResolved
    Del-->>GW: 200 OK({deliveryId, status: "Confirmed"})
    GW-->>SiteEng: {confirmation, evidenceUrl, reference}
```

## Service Interaction Summary

| Interaction | From               | To                 | Purpose                                 |
| ----------- | ------------------ | ------------------ | --------------------------------------- |
| 1           | Vendor             | API Gateway        | Schedule delivery                       |
| 2           | Vendor             | API Gateway        | Dispatch shipment                       |
| 3           | Site Engineer      | API Gateway        | Confirm delivery (mobile with evidence) |
| 4           | Delivery Service   | Workflow Service   | Create discrepancy task                 |
| 5           | Delivery Service   | Inventory Service  | Update stock ledger                     |
| 6           | Delivery Service   | Financials Service | Request credit note                     |
| 7           | Financials Service | Delivery Service   | Issue and apply credit note             |

## Discrepancy Types

| Type       | Description                                  | Action                        |
| ---------- | -------------------------------------------- | ----------------------------- |
| SHORT      | Received quantity less than ordered quantity | Credit note for difference    |
| OVER       | Received quantity more than ordered quantity | Accept extra or return excess |
| DAMAGE     | Goods damaged in transit                     | Credit note + vendor claim    |
| WRONG_ITEM | Goods do not match PO specifications         | Full return + reshipment      |

## Discrepancy Resolution SLAs

| Phase             | SLA             | Responsible   |
| ----------------- | --------------- | ------------- |
| Triage            | 24 hours        | Site Engineer |
| Investigation     | 3 business days | PM            |
| Resolution        | 5 business days | PM/Vendor     |
| Credit Note Issue | 2 business days | Finance       |

## Event Propagation Chain

```
DeliveryScheduled
    → ShipmentDispatched
        → DeliveryConfirmed
            → DiscrepancyRaised (if issues)
                → DiscrepancyResolved
                    → StockReceived
                    → CreditNoteIssued (if applicable)
```

## File Information

- **Workflow**: Material Delivery and Discrepancy Resolution
- **Complexity**: Medium (5 steps, 8 events, 5 services)
- **Phase Priority**: Phase 2
- **Related Workflows**: Procurement → Delivery → Financials, Dispute Resolution
