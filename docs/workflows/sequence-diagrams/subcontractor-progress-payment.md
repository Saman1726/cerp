# Subcontractor Progress → Payment Workflow

## Sequence Diagram

```mermaid
sequenceDiagram
    participant SubPM as Subcontractor PM
    participant SiteEng as Site Engineer
    participant GW as API Gateway
    participant Prog as Progress Service
    participant WF as Workflow Service
    participant Fin as Financials Service

    %% Step 1: Submit Progress Claim
    SubPM->>GW: POST /progress-claims (workPackageId, measurements, evidence)
    GW->>Prog: VerifySubcontractorAccess(tenantId, workPackageId)
    GW->>Prog: SubmitProgressClaim
    Prog->>Prog: Validate(WorkPackageExists, RatesDefined)
    Prog->>Prog: Emit ProgressClaimSubmitted
    Prog-->>GW: 201 Created({claimId, status: "Submitted"})

    %% Step 2: Verification
    SiteEng->>GW: POST /progress-claims/{claimId}/verify (mobile)
    GW->>Prog: VerifyProgress(claimId, verifiedQuantities, photos, notes)
    Prog->>Prog: Validate(claimId, SubcontractorActive)
    Prog->>WF: CreateVerificationTask(claimId, siteEngineerRole)
    Prog->>Prog: Emit MilestoneVerified
    Prog->>Fin: InvoiceIssued(claimId, verifiedAmount, rates)
    Fin->>Fin: Validate(RateCardMatch, BudgetAvailable)
    Fin->>Fin: Emit InvoiceApproved

    alt Verification Discrepancy
        Prog->>Prog: Emit ReworkRequested(quantityDifference)
        Prog->>WF: CreateReworkTask(claimId, subcontractorId)
        Note over Prog,Fin: Subcontractor addresses discrepancies
    end

    %% Step 3: Payment Scheduling
    Fin->>Fin: SchedulePayment(claimId, contractTerms)
    Fin-->>GW: 200 OK({invoiceId, paymentDate})
    GW-->>SiteEng: {verification, status: "Verified"}
```

## Service Interaction Summary

| Interaction | From               | To                 | Purpose                             |
| ----------- | ------------------ | ------------------ | ----------------------------------- |
| 1           | Subcontractor PM   | API Gateway        | Submit progress claim               |
| 2           | API Gateway        | Progress Service   | Verify access and submit claim      |
| 3           | Site Engineer      | API Gateway        | Verify progress (mobile)            |
| 4           | Progress Service   | Workflow Service   | Create verification task            |
| 5           | Progress Service   | Financials Service | Issue invoice based on verification |
| 6           | Financials Service | Financials Service | Validate rate card and budget       |
| 7           | Financials Service | Financials Service | Schedule payment                    |

## Rate Card Integration

| Element           | Source                      | Description                          |
| ----------------- | --------------------------- | ------------------------------------ |
| Rate Card         | Financials Service          | Pre-agreed rates per work unit       |
| Verified Quantity | Progress Claim verification | Site Engineer confirmed measurements |
| Invoice Amount    | Calculated                  | verifiedQuantity × agreedRate        |
| Budget Check      | Financials Service          | Validates against project budget     |

## Progress Claim Status Flow

```
Draft → Submitted → Under_Review → Verified → Invoice_Issued → Approved → Paid
                            ↓
                      Rework_Requested → Submitted (resubmit)
                            ↓
                        Rejected
```

## File Information

- **Workflow**: Subcontractor Progress → Payment
- **Complexity**: Medium (3 steps, 7 events, 5 services)
- **Phase Priority**: Phase 3
- **Related Workflows**: Procurement → Delivery → Financials
