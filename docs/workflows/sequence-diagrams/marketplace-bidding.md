# Marketplace Bidding Workflow

## Sequence Diagram

```mermaid
sequenceDiagram
    participant PM
    participant GW as API Gateway
    participant Proc as Procurement Service
    participant Mktp as Marketplace Service
    participant Fin as Financials Service
    participant Del as Delivery Service

    %% Step 1: Create Tender
    PM->>GW: POST /tenders (open/invited, category, requirements)
    GW->>Proc: VerifyProjectAccess(tenantId, projectId)
    GW->>Mktp: CreateTender
    Mktp->>Mktp: Validate(Category, Region, Requirements)
    Mktp->>Mktp: Emit TenderOpened
    alt Open Tender
        Mktp->>Mktp: PublishToMarketplace(category, region)
    else Invited Tender
        Mktp->>Mktp: SendInvitations(vendorIds)
    end
    Mktp-->>GW: 201 Created({tenderId, status: "Open"})

    %% Step 2: Submit Bid
    Vendor->>GW: POST /tenders/{tenderId}/bids
    GW->>Mktp: SubmitBid(tenderId, vendorId, pricing, leadTime, attachments)
    Mktp->>Mktp: DebitCreditLedger(vendorId, bidCreditCost)
    Mktp->>Mktp: ValidateBid(pricing, units, compliance)
    Mktp->>Mktp: Emit BidSubmitted
    Mktp-->>GW: 201 Created({bidId, creditsDebited})

    Note over Mktp: Tender Closing Period

    %% Step 3: Close Evaluation
    PM->>GW: POST /tenders/{tenderId}/close-evaluation
    GW->>Mktp: CloseEvaluation(tenderId)
    Mktp->>Mktp: Emit TenderEvaluated
    Mktp->>Mktp: ScoreBids(price, leadTime, rating, completeness)
    Mktp->>Mktp: RecommendWinner(scoredBids)

    %% Step 4: Award Tender
    PM->>GW: POST /tenders/{tenderId}/award (select winner)
    GW->>Mktp: AwardTender(tenderId, winningBidId)
    Mktp->>Mktp: Emit TenderAwarded
    Mktp->>Mktp: RefundCredits(nonWinningVendorIds)
    Mktp->>Proc: LinkAwardToRequest(tenderId, procurementRequestId)
    Proc->>Proc: Emit AwardedBidLinked
    Proc->>Del: CreateDeliveryOrder(procurementRequestId)
    Note over Mktp,Proc: Commission not yet accrued - waits for DeliveryConfirmed
    Mktp-->>GW: 200 OK({awardId, commissionRate, capAmount})

    %% Step 5: Commission Cap Check
    Mktp->>Mktp: CheckCommissionCap(projectId, category, vendorId)
    alt Cap Reached
        Mktp->>Mktp: Emit CommissionCapReached
    else Cap Not Reached
        Mktp->>Mktp: MarkCommissionPending(poId)
    end
```

## Service Interaction Summary

| Interaction | From                | To                  | Purpose                           |
| ----------- | ------------------- | ------------------- | --------------------------------- |
| 1           | PM                  | API Gateway         | Create tender                     |
| 2           | API Gateway         | Marketplace Service | Create and publish tender         |
| 3           | Vendor              | API Gateway         | Submit bid                        |
| 4           | Marketplace Service | Marketplace Service | Debit credit ledger               |
| 5           | PM                  | API Gateway         | Close evaluation                  |
| 6           | PM                  | API Gateway         | Award tender                      |
| 7           | Marketplace Service | Procurement Service | Link award to procurement request |
| 8           | Procurement Service | Delivery Service    | Create delivery order             |
| 9           | Marketplace Service | Marketplace Service | Check and apply commission cap    |

## Credit Economy Rules

| Event                          | Credit Action                                    |
| ------------------------------ | ------------------------------------------------ |
| BidSubmitted                   | Debit bidCreditCost from vendor                  |
| TenderCancelled (system)       | Full refund to all bidders                       |
| TenderAwarded                  | Refund non-winning bids                          |
| Winning bid delivery confirmed | No credit effect (commission handled separately) |
| GoldenBid purchase             | Debit premium placement credit cost              |

## Commission Lifecycle

```
TenderAwarded → CommissionMarkedPending
    → DeliveryConfirmed → CommissionAccrued
    → CommissionCapReached (if cap reached for project/category)
```

## Evaluation Scoring Model

| Factor          | Weight | Data Source          |
| --------------- | ------ | -------------------- |
| Price           | 40%    | Bid.pricing          |
| Lead Time       | 25%    | Bid.leadTime         |
| Supplier Rating | 20%    | Vendor reputation    |
| Completeness    | 15%    | Bid compliance check |

## File Information

- **Workflow**: Marketplace Bidding
- **Complexity**: High (5 steps, 10 events, 5 services)
- **Phase Priority**: Phase 2-3
- **Related Workflows**: Procurement → Delivery → Financials, Commission Tracking
