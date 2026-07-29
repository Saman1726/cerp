# Procurement → Delivery → Financials Workflow

## Sequence Diagram

This is the standalone Mermaid sequence diagram for the end-to-end procurement lifecycle.

```mermaid
sequenceDiagram
    participant PM as Project Manager
    participant GW as API Gateway
    participant PS as Project Service
    participant Proc as Procurement Service
    participant WF as Workflow Service
    participant Fin as Financials Service
    participant Mktp as Marketplace Service
    participant Del as Delivery Service
    participant Inv as Inventory Service

    %% Step 1: Create Procurement Request
    PM->>GW: POST /procurement-requests
    GW->>PS: VerifyProjectAccess(tenantId, projectId)
    PS-->>GW: OK
    GW->>Proc: CreateProcurementRequest
    Proc->>Proc: Validate(BudgetAvailable, CatalogItemExists)
    Proc->>Proc: Emit ProcurementRequestCreated
    Proc-->>GW: 201 Created({requestId, status: "Draft"})

    %% Step 2: Submit for Approval
    PM->>GW: POST /procurement-requests/{id}/submit
    GW->>Proc: SubmitForApproval(requestId)
    Proc->>Proc: Validate(AllLineItemsPopulated, EvidenceAttached)
    Proc->>WF: CreateApprovalTask(requestId, approverRole)
    Proc->>Proc: Emit ProcurementRequestSubmittedForApproval
    Proc-->>GW: 200 OK

    %% Step 3: Approval Review
    Appr->>GW: POST /workflow/tasks/{taskId}/approve
    GW->>WF: ApproveTask(taskId, userId)
    WF->>WF: Validate(Role, SLA)
    WF->>Proc: ExecuteApproval(requestId, "Approved")
    Proc->>Proc: Emit RequestApproved
    Proc->>Fin: CommitmentRecorded(requestId, amount)
    Fin->>Fin: LockBudget(requestId, amount)
    Fin-->>Proc: BudgetLocked
    Proc-->>WF: ApprovalComplete
    WF-->>GW: 200 OK
    GW-->>Appr: {status: "Approved"}

    %% Step 4: Start Tender (Optional)
    PM->>GW: POST /procurement-requests/{id}/start-tender
    GW->>Proc: StartTender(requestId)
    Proc->>Mktp: CreateTender(requestId, requirements)
    Mktp->>Mktp: Emit TenderOpened
    Mktp-->>Proc: TenderCreated({tenderId})
    Proc-->>GW: 200 OK

    %% Step 5: Award Tender
    PM->>GW: POST /tenders/{tenderId}/award
    GW->>Mktp: AwardTender(tenderId, winningBidId)
    Mktp->>Mktp: Emit TenderAwarded
    Mktp->>Proc: LinkAwardToRequest(tenderId, requestId)
    Proc->>Proc: Emit AwardedBidLinked
    Proc-->>GW: 200 OK

    %% Step 6: Issue Purchase Order
    PM->>GW: POST /procurement-requests/{id}/issue-po
    GW->>Proc: IssuePO(requestId)
    Proc->>Proc: Validate(Approved, VendorApproved)
    Proc->>Mktp: CheckVendorApproval(vendorId)
    Mktp-->>Proc: OK
    Proc->>Proc: Emit POIssued
    Proc->>Del: CreateDeliveryOrder(requestId, poId)
    Del->>Del: Emit DeliveryScheduled
    Proc-->>GW: 201 Created({poId, status: "Issued"})

    %% Step 7: Vendor Dispatches
    Vendor->>GW: POST /deliveries/{deliveryId}/dispatch
    GW->>Del: DispatchDelivery(deliveryId)
    Del->>Del: Emit ShipmentDispatched
    Del-->>GW: 200 OK

    %% Step 8: Site Receipt Confirmation
    SiteEng->>GW: POST /deliveries/{deliveryId}/confirm (mobile)
    GW->>Del: ConfirmDelivery(deliveryId, evidence)
    Del->>Del: Validate(ETA, Quantity)
    Del->>Del: Emit DeliveryConfirmed

    alt Discrepancy Detected
        Del->>Del: Emit DiscrepancyRaised
        Del->>WF: CreateDiscrepancyTask(deliveryId)
        Note over Del,WF: Discrepancy Resolution Flow
    end

    Del->>Inv: StockReceived(deliveryId, quantities)
    Inv->>Inv: UpdateStock(deliveryId, quantities)
    Inv-->>Del: StockUpdated

    %% Step 9: Financial Invoice Processing
    Del->>Fin: InvoiceIssued(deliveryId, confirmedAmount)
    Fin->>Fin: Validate(POMatch, QuantityMatch)
    Fin->>Fin: Emit InvoiceApproved

    %% Step 10: Commission Accrual
    alt Marketplace Category
        Del->>Mktp: CommissionAccrued(poId, amount)
        Mktp->>Mktp: CheckCommissionCap(projectId, category)
        Mktp-->>Del: CommissionApplied
    end

    Del-->>GW: 200 OK({deliveryId, status: "Confirmed"})
    GW-->>SiteEng: {confirmation, quantities, evidenceUrl}
```

## Service Interaction Summary

| Interaction | From                | To                  | Purpose                 |
| ----------- | ------------------- | ------------------- | ----------------------- |
| 1           | PM Client           | API Gateway         | Verify project access   |
| 2           | API Gateway         | Procurement Service | Create request          |
| 3           | Procurement Service | Workflow Service    | Create approval task    |
| 4           | Workflow Service    | Financials Service  | Record commitment       |
| 5           | Procurement Service | Marketplace Service | Check vendor approval   |
| 6           | Procurement Service | Delivery Service    | Create delivery order   |
| 7           | Delivery Service    | Inventory Service   | Update stock            |
| 8           | Delivery Service    | Financials Service  | Issue invoice           |
| 9           | Delivery Service    | Marketplace Service | Accrue commission       |
| 10          | Delivery Service    | Workflow Service    | Create discrepancy task |

## Event Propagation Chain

```
ProcurementRequestCreated
    → ApprovalTaskCreated (Workflow Service)
    → RequestApproved
        → CommitmentRecorded (Financials Service)
        → BudgetLocked (Financials Service)
    → POIssued
        → DeliveryScheduled (Delivery Service)
    → ShipmentDispatched
        → DeliveryConfirmed (Delivery Service)
            → StockReceived (Inventory Service)
            → InvoiceIssued (Financials Service)
            → CommissionAccrued (Marketplace Service)
```

## File Information

- **Workflow**: Procurement → Delivery → Financials
- **Complexity**: Highest (10 steps, 15+ events, 9 services)
- **Phase Priority**: Phase 2 (highest business impact)
- **Related Workflows**: Material Delivery, Marketplace Bidding, Financial Close
