# Workflows Documentation

This directory contains the enhanced documentation for Section 7 (High-Level Business Workflows) of the ConstructHub Concept vNext architecture.

## Directory Structure

```
docs/workflows/
├── README.md                          # This file
├── section7-enhancement-plan.md       # Implementation plan (original)
├── section7-enhanced.md               # Complete enhanced Section 7 content
├── sequence-diagrams/                 # Standalone Mermaid sequence diagrams
│   ├── procurement-delivery-financials.md
│   ├── material-delivery-discrepancy.md
│   ├── marketplace-bidding.md
│   ├── subcontractor-progress-payment.md
│   ├── user-assignment-access.md
│   └── document-lifecycle.md
├── state-machines/                    # Standalone Mermaid state diagrams
│   ├── procurement-request.md
│   ├── delivery.md
│   ├── tender.md
│   └── progress-claim.md
└── workflow-templates/                # JSON workflow configuration templates
    ├── procurement-standard.json
    ├── delivery-discrepancy.json
    └── bidding-workflow.json
```

## Quick Reference

### Workflows by Priority

| Priority | Workflow                                     | Phase     | Complexity |
| -------- | -------------------------------------------- | --------- | ---------- |
| 1        | User Assignment and Access Provisioning      | Phase 1   | Low        |
| 2        | Procurement → Delivery → Financials          | Phase 2   | Highest    |
| 3        | Material Delivery and Discrepancy Resolution | Phase 2   | Medium     |
| 4        | Document Lifecycle                           | Phase 2   | Medium     |
| 5        | Marketplace Bidding                          | Phase 2-3 | High       |
| 6        | Subcontractor Progress → Payment             | Phase 3   | Medium     |

### Phase 2+ Workflows

| Workflow           | Description                                        |
| ------------------ | -------------------------------------------------- |
| Vendor Onboarding  | KYC/KYB review → Approval → Marketplace activation |
| Dispute Resolution | Evidence → Mediation → Resolution                  |
| Financial Close    | Accruals → Reconciliation → Close                  |
| Material Request   | Catalog search → Quotation → PO                    |

## Key Concepts

### Saga Pattern

- **Orchestrator-based**: Used for complex cross-context workflows with multiple branches
- **Event choreography**: Used for simpler linear flows
- **Transactional outbox**: All sagas use this pattern for reliable event publishing

### State Machine Principles

Each workflow aggregate has an explicit state machine with:

- Defined states and transitions
- Command handler contracts
- Pre/post conditions
- Compensation flows for error scenarios

### Workflow Configuration

Templates support:

- Dynamic step insertion
- Role-based assignment with delegation
- Conditional branching via expression language
- Built-in KPI measurement points
- Automated compensation actions

## Related Documentation

- **Main Architecture**: `docs/ConstructHub_Concept_vNext.md`
- **Business Concept**: `docs/ConstructHub_Business_Concept.md`
- **API Architecture**: `docs/architecture/`
- **Engineering Governance**: `docs/engineering/`

## File Information

- **Last Updated**: 2026-01-15
- **Version**: 1.0
- **Status**: Implementation Ready
