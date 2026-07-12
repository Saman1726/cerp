# Generic subdomain & platform modules

Generic contexts are commodity capabilities - buy or reuse an existing
library/service before building one, since they aren't where cerp
differentiates. Platform contexts aren't from In4Suite at all; they're the
multi-tenancy and auth infrastructure every other context in the catalog
depends on. See [../bounded-context-map.md](../bounded-context-map.md) for
the full classification and [../README.md](../README.md#how-to-read-the-catalog-entries)
for the template these entries follow.

---

## Document Management

| | |
|---|---|
| **Subdomain** | Generic |
| **Purpose** | Centralized, indexed, versioned, tagged document storage - title deeds, contracts, invoices, BOQ drawings - used as an attachment capability by nearly every other context. Corresponds to In4Suite's "Document Management System." |
| **Status** | planned |
| **Aggregate root** | `DocumentRecord` |
| **Entities & value objects** | `DocumentVersion`, `DocumentTag` (entities); `DocumentIndex`, `AccessLevel` (value objects) |
| **Domain events** | `DocumentUploaded`, `DocumentVersioned` |
| **Inbound ports** | `UploadDocumentPort` |
| **Outbound ports** | `DocumentRepositoryPort`, `BlobStorageGatewayPort` (S3/MinIO - genuinely generic, buy/use a managed object-storage service rather than build one) |
| **Hydration strategy** | State-based |
| **Relationships** | Open Host Service consumed by every context in both other catalogs that needs attachments (Land Acquisition's title documents, Cost & Invoicing's invoices, Construction Engineering's BOQ drawings, and more). |

---

## Document Intelligence

| | |
|---|---|
| **Subdomain** | Generic |
| **Purpose** | Extracts structured data (vendor, line items, amounts, dates, parties, case numbers) from unstructured documents - invoices, receipts, BOQ quotes, legal filings - using a self-hosted, fine-tunable document-understanding model. Not an In4Suite module; this is an AI capability layered on top of the module scope, decided during architecture chat rather than derived from the research. |
| **Status** | planned. Decision: self-hosted document-understanding models (LayoutLM/Donut family), not a general-purpose vision-LLM API call, specifically so the model can be **fine-tuned on this tenant base's actual document formats** over time (an option a closed API doesn't offer), and so documents never leave infrastructure this company controls. Chosen over the alternatives compared in chat: plain OCR + a small LLM (cheaper to stand up, but bounded accuracy ceiling and no fine-tuning lever), and calling a hosted vision-LLM API directly (fastest to ship, but recurring per-document cost and no path to specialize on this tenant base's documents). |
| **Aggregate root** | `ExtractionJob` |
| **Entities & value objects** | `ExtractedField` (entity, carries a `ConfidenceScore` value object per field); `DocumentReference` (value object, points back to Document Management's `DocumentRecord` rather than duplicating it) |
| **Domain events** | `ExtractionRequested`, `ExtractionCompleted`, `ExtractionFailed` |
| **Inbound ports** | `RequestExtractionPort` |
| **Outbound ports** | `ExtractionJobRepositoryPort`, `DocumentUnderstandingModelGatewayPort` (the self-hosted LayoutLM/Donut inference endpoint - this is the one port where "which model, and whether it's fine-tuned" is an adapter-level decision, swappable the same way Party Registry's `PartyRegistryGatewayPort` adapter is) |
| **Hydration strategy** | State-based |
| **Relationships** | Open Host Service consumed by Cost & Invoicing, Expense & Reimbursement, Construction Engineering, and Legal & Compliance (each a Customer-Supplier relationship). Critically, this context never writes into any of those contexts' aggregates directly - a completed extraction is handed back as a *draft* (e.g., a proposed `RaiseInvoiceCommand`), and the requesting context's own port and aggregate still validate it exactly as if a person had typed it in. Document Intelligence proposes; it never bypasses domain invariants. |

---

## Reporting & Analytics

| | |
|---|---|
| **Subdomain** | Generic |
| **Purpose** | Cross-context dashboards and reports. Corresponds to In4Suite's Business Intelligence layer (built on Qlik). Unlike every other context in this catalog, **it has no aggregates and no write side** - it exists purely to hydrate read models from every other context's published domain events. See hydration-and-projection.md for the full mechanism and why this matters at scale. |
| **Status** | existing, simplified - the Dashboard page's stat cards (`apps/web/app/(app)/dashboard`) call `GET /properties` and `GET /partners` live today, which is correct at current data volumes and is the placeholder this context's real read models (see below) are meant to replace once volume makes live counting expensive. |
| **Aggregate root** | none - query-only context |
| **Read models** | `PortfolioDashboardProjection`, `ProjectCostVsBudgetProjection`, `CollectionsAgingProjection` (each a denormalized table, independent of any other context's write-side schema) |
| **Domain events consumed** | every domain event published anywhere in the catalog is a candidate input - `ProjectCreated`, `UnitBooked`, `LeaseSigned`, `InvoiceRaised`, `JournalEntryPosted`, and so on |
| **Inbound ports** | query ports, e.g. `GetPortfolioDashboardQueryPort` - read-only, no commands |
| **Outbound ports** | one `Projector` per read model, each subscribing to the specific events it maintains |
| **Hydration strategy** | N/A - no aggregates to hydrate; this context only ever reads its own projection tables |
| **Relationships** | Published Language consumer of every other context - depends only on their versioned domain events, never on their tables, and no other context knows Reporting & Analytics exists. |

---

## Identity & Access

| | |
|---|---|
| **Subdomain** | Platform (not from In4Suite) |
| **Purpose** | One authenticated identity per human - a single email/password - that can hold membership in multiple, independent contexts (a tenant-organization, a Party/provider, a generic default) and switch between them without re-authenticating, plus fine-grained per-project role assignment within whichever context is active. |
| **Status** | **planned redesign**, superseding what's built today (`packages/core-api/src/auth`: a `User` scoped inside one tenant's own schema, one email = one disconnected account per tenant, no cross-tenant or cross-context concept at all). The redesign's core change: `Identity` moves out of each tenant's schema into the shared `platform` schema, becoming the one global credential a human authenticates against; tenant/project-level access becomes a separate, revocable grant rather than something baked into where the row lives. This is a real migration, not an additive change - existing tenant `User` rows would need to be reconciled into platform-level `Identity` rows plus per-tenant `Membership` grants. |
| **Aggregate roots** | `Identity` (the credential - email, password hash, status); `Membership` (a separate aggregate, not a child of `Identity` - granting one `Identity` a role inside one context, so granting/revoking access at one tenant never has to lock or reason about that identity's unrelated memberships elsewhere); `Invitation` (its own pending/accepted/expired/revoked lifecycle, since it can target an email with no `Identity` yet) |
| **Entities & value objects** | `ProjectAssignment` (entity, child of `Membership` - scopes a role to one specific project *within* that membership's context, so its own invariant - "this assignment's project must belong to this membership's tenant" - is enforced by the aggregate boundary); `MembershipContext` (value object: `type` of `tenant` \| `party` \| `generic`, plus the referenced `contextId` - a tenant, or a Party Registry record, or unset for generic); `Role` (value object, e.g. Owner/Admin/Staff at the org level, PM/Finance/Investor/Contractor/Viewer at the project level) |
| **Domain events** | `IdentityRegistered`, `MembershipGranted`, `MembershipRevoked`, `ProjectAssignmentGranted`, `ProjectAssignmentRevoked`, `InvitationSent`, `InvitationAccepted`, `InvitationExpired`, `InvitationRevoked` |
| **Inbound ports** | `AuthenticateIdentityPort` (email + password → the list of that identity's memberships, not yet a tenant-scoped session); `SelectMembershipPort` (choose one membership from that list → issues the actual scoped session/token for that context - this is the "switch" action, and it never requires re-entering a password); `InviteUserPort` (new email, no `Identity` yet → creates a pending `Invitation`); `AcceptInvitationPort` (creates the `Identity` if needed, plus the `Membership` and any `ProjectAssignment`s the invitation specified); `AssignUserToProjectPort` (for an identity that already has a `Membership` - directly creates a new `ProjectAssignment`, no invitation step) |
| **Outbound ports** | `IdentityRepositoryPort`, `MembershipRepositoryPort`, `InvitationRepositoryPort`, `InvitationEmailGatewayPort` (sends the actual invite email - an Anticorruption Layer to whatever email-sending service is used) |
| **Hydration strategy** | State-based |
| **Relationships** | Shared Kernel - every context in both other catalogs still depends on an authenticated principal existing, same as today. New relationship: a `Membership` with `MembershipContext.type = 'party'` references a Party Registry record by id (Customer-Supplier - Identity & Access depends on Party Registry's published party ids, never duplicates or owns that data). |

**Open question, not yet resolved**: what a `generic` context membership concretely grants - a demoted, read-only view of whatever the identity is already a member of elsewhere, or a fully separate default access level with no tenant/party tie at all. Worth settling before this gets built, since the two read very differently as an authorization model.

**Enforcement note**: none of this is self-enforcing without a second guard alongside today's `TenancyGuard`. `TenancyGuard` answers "is this a valid request for this tenant" - it has no notion of projects. A planned `ProjectAccessGuard` would need to check a request's `:projectId` against the caller's active membership's `ProjectAssignment`s (and whether the role there permits the action) before per-project scoping is a real access boundary rather than just a data model.

---

## Tenancy & Platform

| | |
|---|---|
| **Subdomain** | Platform (not from In4Suite) |
| **Purpose** | The tenant registry and schema-per-tenant isolation mechanism every other context's data depends on. |
| **Status** | **existing** - `packages/core-api/src/tenancy`, `packages/core-api/src/platform`. |
| **Aggregate root** | `Tenant` (platform schema), resolved per-request into a `TenantContext` |
| **Entities & value objects** | `PlatformAdmin` |
| **Domain events** | none currently published |
| **Inbound ports** | `TenancyGuard` (resolves `X-Tenant-Id` header into tenant context before any other guard/handler runs) |
| **Outbound ports** | `TenancyService.getEntityManager()` (pinned `queryRunner` with `search_path` bound to the resolved tenant's schema) |
| **Hydration strategy** | State-based |
| **Relationships** | Shared Kernel - every bounded context's own repository implementations resolve their `EntityManager` through this context; no context bypasses it to reach Postgres directly. |
