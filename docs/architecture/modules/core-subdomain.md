# Core subdomain modules

The contexts cerp should build and refine itself - see
[../bounded-context-map.md](../bounded-context-map.md) for why these are
classified Core rather than Supporting, and
[../README.md](../README.md#how-to-read-the-catalog-entries) for the template
these entries follow.

---

## Land Acquisition

| | |
|---|---|
| **Subdomain** | Core |
| **Purpose** | Source, register, and track land parcels from initial identification through title verification to "banked" inventory ready to become a project. |
| **Status** | planned |
| **Aggregate root** | `LandParcel` |
| **Entities & value objects** | `TitleDocument`, `OwnershipRecord` (entities); `SurveyNumber`, `FsiRatio` (floor space index), `GeoBoundary`, `Money` (value objects) |
| **Domain events** | `LandParcelRegistered`, `LandParcelTitleVerified`, `LandParcelBanked` |
| **Inbound ports** | `RegisterLandParcelPort`, `VerifyTitlePort` |
| **Outbound ports** | `LandParcelRepositoryPort`, `SurveyRegistryGatewayPort` (external government land-registry lookup, integrated via Anticorruption Layer) |
| **Hydration strategy** | State-based |
| **Relationships** | Upstream of Legal & Compliance (title must clear legal review before a parcel is usable) and Project Delivery (a project is carved out of banked land) - both Customer-Supplier |

---

## Legal & Compliance

| | |
|---|---|
| **Subdomain** | Core |
| **Purpose** | Track legal proceedings (court cases, disputes) and the regulatory clearances/NOCs required before land or a project can proceed. Merges In4Suite's "Legal & Court Cases" and "Liaison & Clearances" modules - both are the same shape (a case with a status, checklist items, and a resolution) once modeled. |
| **Status** | planned |
| **Aggregate root** | `ComplianceCase` (a `caseType` discriminator distinguishes a court case from a clearance/NOC case) |
| **Entities & value objects** | `CaseHearing`, `ClearanceChecklist`, `ClearanceItem` (entities); `CaseStatus`, `ClearanceAuthority`, `CaseReference` (value objects) |
| **Domain events** | `ComplianceCaseOpened`, `ClearanceGranted`, `ComplianceCaseClosed` |
| **Inbound ports** | `OpenComplianceCasePort`, `RecordClearancePort` |
| **Outbound ports** | `ComplianceCaseRepositoryPort`, `LegalCounselNotificationGatewayPort` |
| **Hydration strategy** | State-based |
| **Relationships** | Gates the Land Acquisition → Project Delivery transition: a project cannot leave its setup phase until required clearances are granted, enforced as a domain policy that checks Legal & Compliance's published state rather than Project Delivery reaching into its tables. |

---

## Project Delivery

| | |
|---|---|
| **Subdomain** | Core |
| **Purpose** | The central project lifecycle - phases, sellable/leasable unit inventory, pricing plans, escalation rules, closure criteria. This is the aggregate every other delivery-side context references by `ProjectId`/`UnitId`. Corresponds to In4Suite's "Projects Setup & Control." |
| **Status** | existing, simplified - see `packages/core-api/src/properties`. Today's `Property` entity is a stand-in for what this context calls `Unit`; it will split into `Project` (the phase/pricing container) + `Unit` (a single sellable/leasable inventory item) once land, phases, and pricing plans are modeled for real. |
| **Aggregate root** | `Project` |
| **Entities & value objects** | `ProjectPhase`, `Unit`, `PricingPlan` (entities); `ProjectCode`, `Money`, `EscalationRule`, `ClosureCriteria` (value objects) |
| **Domain events** | `ProjectCreated`, `ProjectPhaseOpened`, `UnitInventoryPublished`, `ProjectClosed` |
| **Inbound ports** | `CreateProjectPort`, `PublishUnitInventoryPort` |
| **Outbound ports** | `ProjectRepositoryPort`, `LandParcelReadPort` (reads Land Acquisition's published projection, never its aggregate directly) |
| **Hydration strategy** | State-based |
| **Relationships** | Downstream of Land Acquisition + Legal & Compliance; upstream of Construction Engineering, Sales, and Leasing, all of which reference `UnitInventoryPublished` rather than querying Project Delivery's tables. |

---

## Construction Engineering

| | |
|---|---|
| **Subdomain** | Core |
| **Purpose** | Bill of Quantities (BOQ), work orders, contractor assignment, and site budget tracking against a project. Corresponds to In4Suite's "Engineering & Budget Control." |
| **Status** | existing as a placeholder page only (`apps/web/app/(app)/construction`) - no backend module yet. |
| **Aggregate root** | `WorkOrder` |
| **Entities & value objects** | `BillOfQuantities`, `BoqLineItem`, `SiteBudget` (entities); `Money`, `Quantity`, `UnitOfMeasure`, `ContractorRef` (value objects) |
| **Domain events** | `WorkOrderIssued`, `BoqApproved`, `SiteBudgetExceeded`, `WorkOrderCompleted` |
| **Inbound ports** | `IssueWorkOrderPort`, `ApproveBoqPort` |
| **Outbound ports** | `WorkOrderRepositoryPort`, `PartnerDirectoryReadPort` (resolve a contractor), `ProcurementGatewayPort` (raise a purchase requisition) |
| **Hydration strategy** | State-based |
| **Relationships** | Consumes Project Delivery's `ProjectId`; drives Procurement (Customer-Supplier); assigns work to Partner Directory contractors (Conformist - accepts that context's supplier/contractor model as-is). |

---

## Sales

| | |
|---|---|
| **Subdomain** | Core |
| **Purpose** | Unit bookings, payment schedules, receipts, and demand letters. Corresponds to In4Suite's "Sales & Collections"; fed by CRM & Leads. |
| **Status** | planned |
| **Aggregate root** | `Booking` |
| **Entities & value objects** | `PaymentSchedule`, `PaymentReceipt`, `DemandLetter` (entities); `Money`, `BookingStatus`, `PaymentMilestone` (value objects) |
| **Domain events** | `UnitBooked`, `PaymentReceived`, `DemandLetterIssued`, `BookingCancelled` |
| **Inbound ports** | `CreateBookingPort`, `RecordPaymentPort` |
| **Outbound ports** | `BookingRepositoryPort`, `UnitAvailabilityReadPort` (reads Project Delivery's published unit-inventory projection), `LedgerPostingGatewayPort` (Anticorruption Layer into Bookkeeping & Ledger) |
| **Hydration strategy** | State-based |
| **Relationships** | Downstream of CRM & Leads (Customer-Supplier) and Project Delivery; upstream of Cost & Invoicing and Bookkeeping & Ledger (ACL - a "payment received" is not the same concept as a ledger posting). |

---

## Leasing

| | |
|---|---|
| **Subdomain** | Core |
| **Purpose** | Tenant leases, term sheets, revenue-share agreements, and Common Area Maintenance (CAM) billing cycles. Corresponds to In4Suite's "Leasing & Revenue Shares." |
| **Status** | existing as a placeholder page only (`apps/web/app/(app)/tenants`) - no backend module yet. |
| **Aggregate root** | `Lease` |
| **Entities & value objects** | `TermSheet`, `RevenueShareAgreement`, `CamBillingCycle` (entities); `Money`, `LeaseTerm`, `RevenueSharePercentage` (value objects) |
| **Domain events** | `LeaseSigned`, `LeaseRenewed`, `CamBillIssued`, `LeaseTerminated` |
| **Inbound ports** | `SignLeasePort`, `IssueCamBillPort` |
| **Outbound ports** | `LeaseRepositoryPort`, `TenantDirectoryReadPort`, `LedgerPostingGatewayPort` |
| **Hydration strategy** | State-based |
| **Relationships** | Downstream of Project Delivery (leases apply to published units); feeds Cost & Invoicing (CAM billing) via ACL. |

---

## Facility Management

| | |
|---|---|
| **Subdomain** | Core |
| **Purpose** | Post-handover property upkeep - maintenance tickets, subcontracted work orders, maintenance schedules and billing. Corresponds to In4Suite's "Property/Facility Management." |
| **Status** | planned |
| **Aggregate root** | `MaintenanceTicket` |
| **Entities & value objects** | `SubcontractWorkOrder`, `MaintenanceSchedule` (entities); `Priority`, `SlaWindow`, `Money` (value objects) |
| **Domain events** | `MaintenanceTicketRaised`, `SubcontractWorkOrderIssued`, `MaintenanceTicketResolved` |
| **Inbound ports** | `RaiseMaintenanceTicketPort` |
| **Outbound ports** | `MaintenanceTicketRepositoryPort`, `PartnerDirectoryReadPort` (subcontractors), `ProcurementGatewayPort` |
| **Hydration strategy** | State-based |
| **Relationships** | Operates on units already delivered by Project Delivery or leased via Leasing; uses Partner Directory for subcontractors (Conformist). |
