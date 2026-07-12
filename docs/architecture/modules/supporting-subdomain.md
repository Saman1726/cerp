# Supporting subdomain modules

Necessary for the business to run, but not what differentiates cerp from a
generic ERP - see [../bounded-context-map.md](../bounded-context-map.md) for
the classification rationale and [../README.md](../README.md#how-to-read-the-catalog-entries)
for the template these entries follow.

---

## CRM & Leads

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | Capture leads from multiple sources, qualify them, and track sales-funnel interactions. Corresponds to In4Suite's "Leads Management." |
| **Status** | planned |
| **Aggregate root** | `Lead` |
| **Entities & value objects** | `LeadInteraction`, `QualificationChecklist` (entities); `LeadSource`, `LeadScore`, `ContactInfo` (value objects) |
| **Domain events** | `LeadCaptured`, `LeadQualified`, `LeadConvertedToBooking` |
| **Inbound ports** | `CaptureLeadPort`, `QualifyLeadPort` |
| **Outbound ports** | `LeadRepositoryPort` |
| **Hydration strategy** | State-based |
| **Relationships** | Upstream of Sales (Customer-Supplier) - `LeadConvertedToBooking` is consumed by Sales to originate a `Booking`. |

---

## Procurement

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | Purchase orders, material inventory, goods receipt, and inter-site transfers. Corresponds to In4Suite's "Purchase & Inventory." |
| **Status** | planned |
| **Aggregate root** | `PurchaseOrder` |
| **Entities & value objects** | `MaterialInventoryItem`, `InterSiteTransfer`, `GoodsReceipt` (entities); `Money`, `Quantity`, `SupplierRef` (value objects) |
| **Domain events** | `PurchaseOrderRaised`, `GoodsReceived`, `InterSiteTransferCompleted` |
| **Inbound ports** | `RaisePurchaseOrderPort` |
| **Outbound ports** | `PurchaseOrderRepositoryPort`, `PartnerDirectoryReadPort` (suppliers), `LedgerPostingGatewayPort` (accrue liability) |
| **Hydration strategy** | State-based |
| **Relationships** | Receives requisitions from Construction Engineering and Facility Management; Conformist consumer of Partner Directory. |

---

## Partner Directory

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | The registry of external suppliers, contractors, and real-estate agents, plus login/SSO delegation for their portal access. Not an explicit In4Suite module by name, but the implicit registry every module referencing a "supplier," "contractor," or "agent" depends on - and the same concept behind In4Suite's Customer/Agent portals. |
| **Status** | **existing and working** - this is the one context in the full catalog already implemented end-to-end: `services/partner-system` (a standalone mock external system) + `apps/api/src/partners` (the integration module: `PartnersService` calling out over HTTP, `PartnersController`/`PartnerAuthController` as inbound adapters). |
| **Aggregate root** | `Partner` (see `services/partner-system/src/partners/partner.model.ts`) |
| **Entities & value objects** | `PartnerUser` (see `partner-user.model.ts`); `PartnerCode`, `PartnerType` (value objects, currently plain string fields - not yet formalized as value objects) |
| **Domain events** | none published yet - `services/partner-system` is a separate deployable with no event bus connection to cerp's own contexts today |
| **Inbound ports** | `apps/api/src/partners/partners.controller.ts` (`GET /partners`), `partner-auth.controller.ts` (`POST /auth/partner-login`, marked `@Public()`) |
| **Outbound ports** | `PartnersService` in `apps/api` is, in hexagonal terms, the outbound gateway adapter: it implements an (currently implicit, not yet extracted as a named interface) `PartnerDirectoryGatewayPort` over `fetch()` against `PARTNER_SYSTEM_URL` |
| **Hydration strategy** | N/A - stateless HTTP calls to an external system, no local aggregate persistence in `apps/api` today |
| **Relationships** | Open Host Service to every context needing a supplier/contractor/agent lookup (Construction Engineering, Procurement, Facility Management, Leasing); those contexts are Conformists, accepting Partner Directory's model as-is. |

---

## Cost & Invoicing

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | Operational, correctable invoicing against project budgets - raising invoices, tracking budget-vs-actual, disputes. The operational half of In4Suite's "Finance & Accounting" (see bounded-context-map.md for why it splits from Bookkeeping & Ledger). |
| **Status** | planned |
| **Aggregate root** | `Invoice` |
| **Entities & value objects** | `InvoiceLineItem`, `BudgetVsActualSnapshot` (entities); `Money`, `InvoiceStatus`, `TaxCode` (value objects) |
| **Domain events** | `InvoiceRaised`, `InvoiceApproved`, `InvoiceDisputed` |
| **Inbound ports** | `RaiseInvoicePort`, `ApproveInvoicePort` |
| **Outbound ports** | `InvoiceRepositoryPort`, `LedgerPostingGatewayPort` (ACL - only an *approved* invoice becomes a ledger posting; the invoice itself stays correctable pre-approval) |
| **Hydration strategy** | State-based |
| **Relationships** | Receives invoices from Procurement/Construction Engineering/Leasing (CAM billing); posts to Bookkeeping & Ledger via ACL once approved. |

---

## Bookkeeping & Ledger

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | The actual accounting ledger - append-only journal entries, corrections via reversing entries only, fiscal period close, and sync to external accounting systems. The ledger half of In4Suite's "Finance & Accounting." See hydration-and-projection.md for why this is the one context using event-sourced hydration. |
| **Status** | planned |
| **Aggregate root** | `JournalEntry` (immutable once posted) |
| **Entities & value objects** | `LedgerAccount`, `Posting` (a debit/credit line) (entities); `Money`, `AccountCode`, `FiscalPeriod` (value objects) |
| **Domain events** | `JournalEntryPosted`, `JournalEntryReversed`, `FiscalPeriodClosed` |
| **Inbound ports** | `PostJournalEntryPort` (deliberately has no corresponding update/delete port - corrections are new reversing entries, enforced structurally, not by convention) |
| **Outbound ports** | `JournalRepositoryPort` (event-sourced - see hydration-and-projection.md), `ExternalAccountingSystemGatewayPort` (ACL to Tally / SAP FICO / QuickBooks / Oracle Financials, matching In4Suite's actual published integration list) |
| **Hydration strategy** | **Event-sourced** - the one exception to the state-based default across this whole catalog |
| **Relationships** | Sink for Sales, Leasing, Cost & Invoicing, HR & Payroll, Expense & Reimbursement, and Asset Management, all of which post journal entries here via ACL; source for Reporting & Analytics. |

---

## HR & Payroll

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | Employee profiles, leave management, performance reviews, payroll runs, region-specific holiday calendars. Corresponds to In4Suite's "HR & Payroll." |
| **Status** | deferred - not needed until cerp's own team-management/back-office scope is prioritized over tenant-facing modules. |
| **Aggregate root** | `Employee` |
| **Entities & value objects** | `LeaveRequest`, `PerformanceReview`, `PayrollRun` (entities); `Money`, `EmploymentStatus`, `HolidayCalendarRegion` (value objects) |
| **Domain events** | `EmployeeOnboarded`, `PayrollRunCompleted`, `LeaveApproved` |
| **Inbound ports** | `OnboardEmployeePort` |
| **Outbound ports** | `EmployeeRepositoryPort`, `LedgerPostingGatewayPort` |
| **Hydration strategy** | State-based |
| **Relationships** | Posts payroll runs to Bookkeeping & Ledger via ACL. |

---

## Expense & Reimbursement

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | Employee expense claims, imprest accounts, and travel booking requests. Merges In4Suite's "Expenses & Reimbursement" and "Travel Management" - a travel request is structurally a pre-approval step feeding the same claim/approval/ledger-posting path, not a distinct consistency boundary (see bounded-context-map.md). |
| **Status** | deferred |
| **Aggregate root** | `ExpenseClaim` |
| **Entities & value objects** | `ExpenseLineItem`, `TravelBookingRequest`, `ImprestAccount` (entities); `Money`, `ApprovalStatus` (value objects) |
| **Domain events** | `ExpenseClaimSubmitted`, `ExpenseClaimApproved`, `TravelBookingRequested` |
| **Inbound ports** | `SubmitExpenseClaimPort` |
| **Outbound ports** | `ExpenseClaimRepositoryPort`, `LedgerPostingGatewayPort`, `TravelBookingGatewayPort` (external airline/hotel/train booking, ACL) |
| **Hydration strategy** | State-based |
| **Relationships** | Posts approved claims to Bookkeeping & Ledger via ACL. |

---

## Asset Management

| | |
|---|---|
| **Subdomain** | Supporting |
| **Purpose** | Fixed-asset register - equipment/vehicles the company itself owns (distinct from `Unit`s, which are inventory the company sells or leases to customers) - maintenance, insurance, depreciation. Corresponds to In4Suite's "Asset Management." |
| **Status** | deferred |
| **Aggregate root** | `Asset` |
| **Entities & value objects** | `MaintenanceRecord`, `InsurancePolicy`, `DepreciationSchedule` (entities); `Money`, `AssetTag`, `DepreciationMethod` (value objects) |
| **Domain events** | `AssetRegistered`, `AssetDepreciationPosted`, `AssetDisposed` |
| **Inbound ports** | `RegisterAssetPort` |
| **Outbound ports** | `AssetRepositoryPort`, `LedgerPostingGatewayPort` |
| **Hydration strategy** | State-based |
| **Relationships** | Posts depreciation schedules to Bookkeeping & Ledger via ACL. |
