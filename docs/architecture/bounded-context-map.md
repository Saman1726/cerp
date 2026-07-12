# Bounded context map

In4Suite's 16 published modules plus its Business Intelligence layer
regroup into **19 bounded contexts** for cerp. The regrouping isn't 1:1
because DDD bounded contexts are drawn around **consistency boundaries and
ubiquitous language**, not around a vendor's marketing/pricing module list -
so a few In4Suite modules split (Finance & Accounting) and a few merge (Legal
+ Liaison/Clearances) once modeled that way. Two contexts
(Identity & Access, Tenancy & Platform) don't come from In4Suite at all - they
are infrastructure this repo already needed for multi-tenancy and auth.

## Subdomain classification

DDD splits bounded contexts into three strategic categories, and the category
should drive investment: build and refine **Core** yourself, buy or
lightly-adapt **Supporting**, and buy off-the-shelf for **Generic** wherever
possible.

### Core subdomain (the reason customers pick cerp over a generic ERP)

| Context | In4Suite module(s) | Status |
|---|---|---|
| Land Acquisition | Land Banking / Business Development | planned |
| Legal & Compliance | Legal & Court Cases + Liaison & Clearances | planned |
| Project Delivery | Projects Setup & Control | existing (as `properties`, simplified) |
| Construction Engineering | Engineering & Budget Control | existing (as the `construction` placeholder page) |
| Sales | Sales & Collections | planned |
| Leasing | Leasing & Revenue Shares | existing (as the `tenants` placeholder page) |
| Facility Management | Property/Facility Management | planned |

### Supporting subdomain (necessary, not differentiating)

| Context | In4Suite module(s) | Status |
|---|---|---|
| CRM & Leads | Leads Management | planned |
| Procurement | Purchase & Inventory | planned |
| Partner Directory | (implicit: suppliers/contractors/agents referenced across modules) | existing (`services/partner-system` + `apps/api/src/partners`) |
| Cost & Invoicing | Finance & Accounting (operational half) | planned |
| Bookkeeping & Ledger | Finance & Accounting (ledger half) | planned |
| HR & Payroll | HR & Payroll | deferred |
| Expense & Reimbursement | Expenses & Reimbursement + Travel Management | deferred |
| Asset Management | Asset Management | deferred |

### Generic subdomain (commodity - buy/reuse before building)

| Context | In4Suite module(s) | Status |
|---|---|---|
| Document Management | Document Management System | planned |
| Reporting & Analytics | Business Intelligence (Qlik) | existing (as the `dashboard` stat cards - see hydration-and-projection.md) |

### Platform (not from In4Suite - this repo's own infrastructure)

| Context | Status |
|---|---|
| Identity & Access | existing (`packages/core-api/src/auth`) |
| Tenancy & Platform | existing (`packages/core-api/src/tenancy`, `platform`) |

Why Finance & Accounting splits in two: **Cost & Invoicing** is operational,
high-volume, correctable data (a PM edits a draft invoice, fixes a typo,
re-submits) - ordinary CRUD-shaped consistency rules are fine. **Bookkeeping &
Ledger** is a real accounting ledger - it must be append-only, corrected only
via reversing entries, and fully reconstructable for an audit. Those are
different invariants and different hydration strategies (see
hydration-and-projection.md), so DDD says: different bounded context, even
though In4Suite sells them as one module.

Why Travel Management folds into Expense & Reimbursement rather than standing
alone: a travel request is, structurally, a pre-approval workflow feeding the
same `ExpenseClaim` aggregate and the same approval/ledger-posting path. It
doesn't have its own consistency boundary distinct from expenses, so giving it
a separate bounded context would be over-splitting.

## Context map

```
                         ┌─────────────────────┐
                         │ Identity & Access    │  Shared Kernel / Open Host Service
                         │ Tenancy & Platform   │  (every context depends on these two)
                         └──────────┬───────────┘
                                    │
   ┌────────────────┐   Customer/  ▼   ┌───────────────────┐   Customer/    ┌────────────────────────┐
   │ Land            │───Supplier───▶│ Legal &            │───Supplier────▶│ Project Delivery        │
   │ Acquisition      │              │ Compliance          │  (clearances    │ (Project, Unit,        │
   └────────────────┘              └───────────────────┘   gate project    │  PricingPlan)           │
                                                             start)          └──────────┬─────────────┘
                                                                                          │ published unit
                                                              ┌───────────────────────────┼──────────────────────────┐
                                                              ▼                           ▼                          ▼
                                                    ┌───────────────────┐        ┌──────────────────┐       ┌──────────────────┐
                                                    │ Construction       │        │ Sales             │       │ Leasing           │
                                                    │ Engineering        │        │ (Booking)         │       │ (Lease)           │
                                                    └─────────┬─────────┘        └────────┬──────────┘       └────────┬─────────┘
                                                              │ raises PO                  │ ACL                       │ ACL
                                                              ▼                             ▼                          ▼
                                                    ┌───────────────────┐        ┌──────────────────────────────────────────┐
                                                    │ Procurement        │        │ Cost & Invoicing                          │
                                                    └─────────┬─────────┘        └────────────────┬──────────────────────────┘
                                                              │ Conformist                          │ ACL
                                                              ▼                                     ▼
                                                    ┌───────────────────┐               ┌──────────────────────────┐
                                                    │ Partner Directory  │               │ Bookkeeping & Ledger      │──▶ external:
                                                    │ (suppliers,        │               │ (append-only journal)     │   Tally / SAP FICO /
                                                    │  contractors,      │               └────────────┬──────────────┘   QuickBooks / Oracle
                                                    │  agents)           │                             │                 Financials (ACL)
                                                    └─────────┬─────────┘               ┌───────────────┴───────────────┐
                                                              │ Conformist               │  HR & Payroll, Expense &      │
                                                              ▼                          │  Reimbursement, Asset Mgmt    │
                                                    ┌───────────────────┐                │  all post journal entries     │
                                                    │ Facility           │                └───────────────────────────────┘
                                                    │ Management         │
                                                    └───────────────────┘

   ┌───────────────────┐   Open Host Service (attachments)   consumed by every context above
   │ Document           │◀─────────────────────────────────────────────────────────────────
   │ Management         │
   └───────────────────┘

   ┌───────────────────┐   Published Language (domain events, no context knows this exists)
   │ Reporting &        │◀─────────────────────────────────────────────────────────────────  every context above
   │ Analytics          │   pure projection consumer - see hydration-and-projection.md
   └───────────────────┘

   ┌───────────────────┐
   │ CRM & Leads        │───Customer/Supplier (LeadConvertedToBooking)───▶ Sales
   └───────────────────┘
```

## DDD strategic relationship patterns used above

| Pattern | Where it applies | Why |
|---|---|---|
| **Shared Kernel** | Identity & Access, Tenancy & Platform | Every other context is *literally unable to run* without a resolved tenant + authenticated principal - these aren't "integrations," they're a shared foundation, consistent with how `CoreModule.forRoot()` already wires `TenancyModule`/`AuthModule` as `global: true`. |
| **Customer-Supplier** | Land Acquisition → Legal & Compliance → Project Delivery; CRM & Leads → Sales | The upstream context's output (a cleared parcel, a qualified lead) is a precondition the downstream context depends on, but the downstream doesn't dictate the upstream's internal model. |
| **Anticorruption Layer (ACL)** | Sales/Leasing/Cost & Invoicing → Bookkeeping & Ledger; Bookkeeping & Ledger → external accounting systems | A "collection" or "invoice" concept must be translated into the ledger's strict debit/credit journal-entry concept, which has different invariants (immutability). Translating at the boundary keeps the ledger's model uncorrupted by every upstream context's looser vocabulary. Same reasoning applies outbound, translating cerp's ledger into Tally's/SAP FICO's own format. |
| **Conformist** | Procurement, Facility Management → Partner Directory | These contexts don't need their own supplier/contractor model - they accept Partner Directory's model as-is rather than translating, since there's no real conflict of vocabulary worth the translation cost. |
| **Open Host Service** | Document Management (attachments), Partner Directory (via `services/partner-system`'s HTTP API) | A published, stable API any context can integrate against without a bespoke integration per consumer. |
| **Published Language** | Every context's domain events, consumed by Reporting & Analytics | Domain events are the versioned, public contract a context exposes; Reporting & Analytics (and any future consumer) depends only on that published event schema, never on another context's tables. |

## Relationship to the current codebase

`Property` (today) is a deliberately simplified stand-in that will eventually
split into `Project` + `Unit` under Project Delivery once land, phases, and
pricing plans are modeled for real. `apps/api/src/partners` +
`services/partner-system` is already exactly what the catalog calls Partner
Directory: a separate deployable integrated through an outbound gateway port,
consumed as a Conformist by whatever context needs a supplier/contractor/agent
lookup. Nothing in this document requires changing that code today - it's the
map for what gets built next, and where.
