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
| **Purpose** | Authentication (JWT access + rotating refresh tokens), the authenticated-principal concept every other context relies on for authorization decisions. |
| **Status** | **existing** - `packages/core-api/src/auth`. Already documented in detail in the repo's own root architecture notes; not re-derived here. |
| **Aggregate root** | `User` |
| **Entities & value objects** | `RefreshToken` |
| **Domain events** | none currently published |
| **Inbound ports** | `AuthController` (`POST /auth/login`, `POST /auth/refresh`) |
| **Outbound ports** | `UserRepository`, `RefreshTokenRepository` (TypeORM-backed today, not yet extracted as named port interfaces) |
| **Hydration strategy** | State-based |
| **Relationships** | Shared Kernel - every context in both other catalogs depends on an authenticated principal existing; wired as `global: true` inside `CoreModule.forRoot()` today via the `APP_GUARD` providers. |

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
