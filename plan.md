# Plan: Phase 1 Implementation – Separated Services

Note: Extracted from docs/ConstructHub_Concept_vNext.md Section 21 and augmented with an initial execution plan.

## Repository Strategy: Monorepo-first with clear split criteria

Current state

- Monorepo with pnpm workspaces (apps/_, packages/_, services/_, examples/_) and Turborepo; shared packages such as packages/shared-types and core-*; infra/k8s lives in-repo and is deployable via GitOps tools later.

Principles

- Monorepo now for speed and coherence; design for independent deployability of each app/service.
- Keep shared code only in packages/*; enforce boundaries so teams don’t couple across deployables.

Immediate actions (make the monorepo team-ready)

- Ownership and access
  - Add CODEOWNERS per path:
    - apps/web/ → @team-web
    - apps/api/ → @team-api
    - services/core-api-java/ → @team-java
    - services/partner-system/ → @team-partner
    - packages/shared-types/ → @team-platform
    - packages/core-*/ → @team-platform
    - infra/ → @team-infra
  - Use labels and required reviews per path; branch protections on main/default.
- Module boundaries
  - Shared code may only be imported from packages/* public barrels. Disallow cross-app/service imports.
  - ESLint: add no-restricted-imports and import path aliases to force public barrels; lint failure on deep relative imports across packages.
- CI/CD per deployable
  - One pipeline per deployable (web, api, each service). Trigger with path filters and use Turborepo filters:
    - Examples: pnpm turbo run lint,test,build --filter=apps/api... and similarly for apps/web, services/*.
  - Build/test only what changed; publish artifacts per deployable; independent release cadence.
  - Enable Turborepo remote cache (Vercel/S3/self-hosted) to speed builds/CI.
- Versioning and publishing of shared packages
  - Introduce Changesets; version packages/* independently and publish to an internal registry (e.g., GitHub Packages).
  - Consumers depend via semver ranges; keep workspace linking for local dev until split is needed.
- Contract-first interfaces
  - Maintain OpenAPI/AsyncAPI specs; generate SDKs for TypeScript/Java; keep DTOs minimal and stable to reduce churn.

Split triggers (when to move to multiple repos)

- 3+ independent teams that need truly independent release cadence with minimal cross-repo coordination.
- Security/compliance or vendor constraints requiring strict repo-level access isolation or open-sourcing a subset.
- Tooling divergence (e.g., heavy Java/Gradle flows vs Node) that makes shared CI/pipelines unwieldy.
- CI friction that remains high even with affected-only builds and remote caching.
- Distribution needs: public packages or external consumption that benefits from standalone repos.

Suggested repo boundaries (if/when splitting)

- cerp-web: apps/web + packages/core-web
- cerp-api: apps/api + packages/core-api
- cerp-services-core-api-java: services/core-api-java
- cerp-services-partner-system: services/partner-system
- cerp-infra: infra/k8s (dedicated GitOps repo for Argo/Flux)
- cerp-platform (optional): packages/shared-types, packages/eslint-config, packages/typescript-config
- cerp-examples: examples/demo-overlay

Incremental migration plan (to execute when triggers are met)

1. Prepare shared artifacts
   - Add Changesets; start publishing packages/* to the registry.
   - Replace workspace:* with ^x.y.z semver ranges in apps/services for packages/shared-types and core-*.
2. Isolate infra
   - Move infra/k8s to a cerp-infra GitOps repo watched by Argo/Flux; deploy via image tags/automation.
3. Carve out services/apps
   - Use git filter-repo (or subtree) to split with history per path; set up per-repo CI (lint/typecheck/test/build, container build, SBOM, scan, deploy).
   - Repoint imports to semver packages; ensure each repo builds from clean clone.
4. Harden CI/CD and releases
   - Require status checks; sign images, add provenance; tag releases; keep independent versioning.
5. Decommission workspace links
   - Validate contract tests across repos; ensure compatible API/SDK versions and events.

## 21. Phase 1 Implementation – Separated Services

Overview

- Goal: Deliver Phase 1 (MVP) by standing up a small set of independently deployable services that establish multi-tenancy, identity/access, organizations and projects, a basic DMS, initial projections, API gateway, and audit baseline.
- Principle: Each capability is its own service with clear contracts (REST commands/queries, domain events). Read models are built as separate projections consuming events over Kafka. Identity for auth is delegated to Keycloak; our Identity service owns tenant/user/membership domain data and emits canonical events.

Phase 1 services (independently deployable)

1. API Gateway/BFF (edge)

- Scope (Phase 1): Single ingress for all clients; OIDC authentication via Keycloak; JWT validation; tenant scoping and header propagation; simple route mapping to backend services; basic rate limiting.
- Interfaces:
  - Routes: /identity/* → Identity Service; /orgs/* → Organization Service; /projects/* → Project Service; /documents/* → DMS; /reports/* → Projections; /audit/* → Audit.
  - Security: OAuth2/OIDC; mTLS for service-to-service; correlation IDs injected (x-correlation-id).
- Dependencies: Keycloak, backend services.
- SLO (Phase 1): P95 latency < 150 ms passthrough; availability ≥ 99.9% (non-strict in MVP, monitor only).

2. Identity & Access Service

- Purpose/Scope: Tenant (organization) identity boundary, users and memberships registry, role assignments. Delegates authentication to Keycloak. Emits canonical identity events.
- Commands/Queries (REST):
  - POST /tenants {name, legalName, region, ...} → 201 TenantCreated
  - POST /users {email, name, ...} → 201 UserCreated
  - POST /memberships {tenantId, userId, roles[], scopes} → 201 RoleAssigned
  - GET /tenants/:id, GET /users/:id, GET /memberships?tenantId=...
- Domain Events (Kafka topics):
  - identity.tenant.v1: TenantCreated, TenantUpdated
  - identity.user.v1: UserCreated, UserUpdated, UserDeactivated
  - identity.membership.v1: RoleAssigned, RoleRevoked
- Write Model (PostgreSQL; RLS default):
  - tenants(id, name, region, created_at, updated_at)
  - users(id, email, name, status, created_at, updated_at)
  - memberships(id, tenant_id, user_id, roles jsonb, scopes jsonb, created_at)
  - RLS policy: tenant_id on memberships; tenant_id scoped queries enforced at service.
- Projections (Phase 1):
  - authz_projection: flattens memberships to per-tenant role claims for fast authorization checks.
- Notes: Sync minimal user info with Keycloak on user creation (via admin API); keep Keycloak as source of authentication, service as SoR for memberships and tenant scoping.

3. Organization Service

- Purpose/Scope: Business organization profile (distinct from tenant auth construct) and org metadata required by downstream services; links to tenant id.
- Commands/Queries (REST):
  - POST /orgs {tenantId, displayName, taxIds[], addresses[], contacts[]} → OrganizationCreated
  - PATCH /orgs/:id {...} → OrganizationUpdated
  - GET /orgs/:id, GET /orgs?tenantId=...
- Domain Events (Kafka topics):
  - projectmgmt.organization.v1: OrganizationCreated, OrganizationUpdated
- Write Model:
  - organizations(id, tenant_id, display_name, tax_profile jsonb, meta jsonb, created_at, updated_at)
- Projections:
  - org_directory (by tenant): id, display_name, contact summaries.
- Dependencies: Identity (tenant existence, membership for authorization).

4. Project Service

- Purpose/Scope: Projects and SubProjects lifecycle, team assignments (by user and role) and scoping information for downstream modules.
- Commands/Queries (REST):
  - POST /projects {tenantId, name, code, startDate, ...} → ProjectCreated
  - POST /projects/:id/subprojects {name, code} → SubProjectCreated
  - POST /projects/:id/assignments {userId, role, scope(subprojectId?)} → UserAssignedToProject
  - GET /projects/:id, GET /projects?tenantId=..., GET /projects/:id/assignments
- Domain Events (Kafka topics):
  - projectmgmt.project.v1: ProjectCreated, ProjectUpdated, SubProjectCreated
  - projectmgmt.assignment.v1: UserAssignedToProject, AssignmentRevoked
- Write Model:
  - projects(id, tenant_id, name, code, status, dates, meta)
  - subprojects(id, project_id, name, code, status)
  - project_assignments(id, project_id, user_id, role, subproject_id?, created_at)
- Projections:
  - project_roster: denormalized roster with user names and roles
  - project_list: list with status/counters per tenant
- Dependencies: Identity (user exists), Organization (optional link to legal entity).

5. Document Management Service (DMS – Basic)

- Purpose/Scope: Minimal document storage with versioning metadata; presigned URLs for upload/download; links to entities (org/project).
- Commands/Queries (REST):
  - POST /documents {tenantId, projectId?, entityRef?, filename, contentType, size} → returns {documentId, uploadUrl}
  - POST /documents/:id/complete {hash, versionNote?} → DocumentUploaded
  - GET /documents/:id (metadata), GET /documents?entityRef=...
  - POST /documents/:id/link {entityRef} → DocumentLinked
- Domain Events (Kafka topics):
  - collaboration.document.v1: DocumentUploaded, DocumentVersioned, DocumentLinked
- Write Model:
  - documents(id, tenant_id, project_id?, entity_ref?, filename, content_type, size, hash, version, storage_uri, created_at)
- Storage: S3-compatible (MinIO) with tenant/project prefixing and server-side encryption; presigned URLs.
- Projections:
  - document_index: queryable metadata by entityRef and project for UI lists.
- Dependencies: API Gateway for auth; Identity for authorization context.

6. Audit/Event Catalog Service

- Purpose/Scope: Centralized append-only audit log for all domain events (envelope + payload), queriable by aggregate; governs event catalog and schemas; emits AuditRecorded for observability.
- Commands/Queries (REST):
  - GET /audit/:aggregateType/:aggregateId → event stream (paged)
  - GET /audit/search?tenantId=...&type=... → search by envelope fields
- Event Processing:
  - Subscribes to all domain topics via Kafka consumer group; persists envelopes/payloads idempotently (outbox schema for internal write paths).
- Write Model:
  - audit_log(id, tenant_id, aggregate_type, aggregate_id, event_type, version, occurred_at, correlation_id, causation_id, actor, payload jsonb, topic, partition, offset)
  - Indexes on (tenant_id, aggregate_type, aggregate_id, occurred_at), (event_type)
- Notes: DLQ and replay procedures established; envelope schema stored in Schema Registry (JSON Schema).

7. Projections/Reporting Service (Initial)

- Purpose/Scope: Build and serve initial read models for dashboards (tenants, users, projects, assignments, documents). Exposes simple GET endpoints for UI.
- Queries (REST):
  - GET /reports/tenants/:tenantId/overview → {users, projects, documents, latestEvents}
  - GET /reports/projects/:projectId/overview → {teamCount, docsCount, createdAt, status}
- Consumption:
  - Subscribes to `identity.*`, `projectmgmt.*`, `collaboration.document.v1` topics; idempotent upserts.
- Read Models:
  - tenant_overview(tenant_id, users_count, projects_count, documents_count, last_event_at)
  - project_overview(project_id, team_count, documents_count, created_at, last_event_at)
- SLA (Phase 1): Projection lag P95 < 60s.

Cross-cutting: Observability and Platform (Phase 1)

- Keycloak (Identity Provider): OIDC/OAuth2; realms configured per environment; short-lived access tokens; MFA optional in MVP.
- Kafka + Schema Registry: Topics per context as listed; additive schema evolution; DLQ topics configured per consumer; partition by aggregateId.
- PostgreSQL: Single cluster with RLS-based isolation in Phase 1 (schema-per-tenant option evaluated later). Row policies enforced on multi-tenant tables keyed by tenant_id.
- Object Storage: MinIO for S3-compatible storage; per-tenant/project prefixes; lifecycle rules not enforced in MVP (documented).
- Observability: OpenTelemetry SDK/auto-instrumentation on all services; OTel Collector; Prometheus metrics; Grafana dashboards; structured logs with correlationId/tenantId (PII safe).
- Security Baseline: mTLS between services; network policies at cluster level; no PII in logs; secrets via dev-safe mechanisms (local .env) with rotation documented.

Event Catalog (Phase 1 scope)

- Envelope (v1): id, type, version, occurredAt, tenantId, aggregateType, aggregateId, aggregateVersion, correlationId, causationId, actor.
- Canonical Events (initial):
  - Identity: TenantCreated v1, UserCreated v1, RoleAssigned v1
  - Project Mgmt: OrganizationCreated v1, ProjectCreated v1, SubProjectCreated v1, UserAssignedToProject v1
  - DMS: DocumentUploaded v1, DocumentLinked v1
- Topic naming: <context>.<aggregate>.v1 (e.g., identity.user.v1).

Service Contracts Summary (Phase 1)

- Identity & Access:
  - Commands: createTenant, createUser, assignRole
  - Queries: getTenant, getUser, listMemberships
  - Events: TenantCreated, UserCreated, RoleAssigned
- Organization:
  - Commands: createOrganization, updateOrganization
  - Queries: getOrganization
  - Events: OrganizationCreated, OrganizationUpdated
- Project:
  - Commands: createProject, createSubProject, assignUser
  - Queries: getProject, listAssignments
  - Events: ProjectCreated, SubProjectCreated, UserAssignedToProject
- DMS:
  - Commands: createDocument (issue upload URL), completeUpload, linkDocument
  - Queries: getDocument, listDocuments
  - Events: DocumentUploaded, DocumentLinked
- Projections/Reporting:
  - Queries: tenantOverview, projectOverview
  - Events consumed: `identity.*`, `projectmgmt.*`, `collaboration.document.v1`
- Audit:
  - Queries: listAuditEventsByAggregate
  - Events consumed: all domain topics

Data Isolation (Phase 1)

- Default: RLS on multi-tenant tables with tenant_id scoping and policies bound to JWT claims; service-level guardrails (tenant in request context).
- Storage partitioning: Object storage by tenant/project prefix; Kafka topics not partitioned by tenant (partition by aggregateId); consumer enforces tenant scoping at projection time.

Exit Criteria Validation (mapped to Phase 1)

- 3 pilot tenants live: createTenant flow plus organization profile created; users and memberships assigned; JWT tokens validated end-to-end.
- First end-to-end lifecycle demo: create tenant → create project → assign users → upload/link document → projections reflect counts within 60s; audit log retrievable for all actions.
- Performance: P95 reads < 400 ms on projections and simple GETs; gateway P95 passthrough < 150 ms baseline.
- Projection lag P95 < 60s for initial read models; DLQ empty after smoke tests.

Operational Notes

- CI/CD: Independent pipelines per service; semantic versioning; image signing and SBOM included; dev/staging/prod environments with GitOps manifests (Helm/Kustomize).
- Testing: Unit + contract tests for REST; consumer-driven contracts for topics; testcontainers for Postgres/Kafka; basic E2E smoke for end-to-end flow.
- Runbooks (Phase 1): Topic replay for projections; DLQ drain; rotating keys in Keycloak; onboarding new tenant checklist.
- Backward Compatibility: Event schemas additive; services tolerate unknown fields; APIs versioned under /v1.

Service Ownership (Phase 1)

- Platform Team: API Gateway, Identity & Access, Audit/Event Catalog, Observability.
- Core PM Team: Organization Service, Project Service, Projections (initial).
- Collaboration Team: DMS (basic).

Roadmap Linkage

- Sets foundation for Phase 2 additions (Vendors, Materials, Procurement, Inventory, Marketplace primitives) without changing Phase 1 contracts. New services will subscribe to identity/project events and emit their own domains without retrofitting Phase 1 boundaries.

---

## Repository and Governance Tasks (Phase 0)

- [ ] Add CODEOWNERS with path ownership for apps/, services/, packages/, infra/ and configure labels + required reviews.
- [ ] Add ESLint rules to enforce import boundaries and public API barrels for packages/*; prohibit cross-app/service imports.
- [ ] Introduce Changesets; configure publishing of packages/* to an internal registry (GitHub Packages or alternative).
- [ ] Configure Turborepo remote cache (Vercel/S3/self-hosted) and enable affected-only pipelines in CI using --filter.
- [ ] Create per-deployable CI pipelines (web, api, each service) with path-based triggers; artifacts and releases per deployable.
- [ ] Establish OpenAPI/AsyncAPI generation in CI; publish versioned SDKs; add consumer-driven contract tests between services and clients.
- [ ] Prepare infra for GitOps split (cerp-infra repo plan, image automation, environment overlays).
- [ ] Plan transition from workspace:* to semver ranges for packages/shared-types and core-* once publishing is enabled.
- [ ] Add CONTRIBUTING.md covering boundaries, release/versioning, and ownership runbooks.

## Initial Execution Checklist (to start planning)

- [ ] Local platform baseline
  - [ ] Stand up dev Keycloak with realm, client, and test users
  - [ ] Start Kafka + Schema Registry (topics bootstrap script)
  - [ ] Start PostgreSQL (dev) with migration tool (Flyway/Liquibase)
  - [ ] Start MinIO (S3-compatible) and create bucket/prefix policy
- [ ] Contracts and schemas
  - [ ] Define v1 event envelope and canonical events in Schema Registry
  - [ ] Document topic naming, partitions, and DLQ strategy
- [ ] API Gateway/BFF
  - [ ] JWT validation, tenant scoping, correlation ID middleware
  - [ ] Route mapping to backend services, basic rate limiting
- [ ] Identity & Access Service
  - [ ] Implement tenants, users, memberships endpoints
  - [ ] Integrate Keycloak admin for minimal user sync
  - [ ] Emit identity.* events; enforce RLS policies
  - [ ] Build authz_projection
- [ ] Organization Service
  - [ ] CRUD endpoints, events, and org_directory projection
- [ ] Project Service
  - [ ] Projects, subprojects, assignments endpoints and events
  - [ ] Projections: project_list, project_roster
- [ ] DMS (basic)
  - [ ] Presigned upload flow, metadata persistence, link API
  - [ ] Emit document events; document_index projection
- [ ] Audit/Event Catalog
  - [ ] Kafka consumer of all topics; append-only audit_log
  - [ ] Search/query endpoints; replay/DLQ runbooks
- [ ] Projections/Reporting
  - [ ] Build tenant_overview and project_overview; expose GET endpoints
  - [ ] Ensure idempotent upserts and checkpointing
- [ ] Observability and security baseline
  - [ ] OpenTelemetry, Prometheus metrics, Grafana dashboards
  - [ ] mTLS between services; secrets management for dev
- [ ] CI/CD and environments
  - [ ] Independent pipelines, image signing/SBOM
  - [ ] GitOps manifests for dev/staging
- [ ] E2E validation against exit criteria
  - [ ] “Create tenant → create project → assign user → upload/link doc” happy path
  - [ ] Verify projection lag < 60s and P95 read latencies
- [ ] Documentation and runbooks
  - [ ] Service READMEs, API specs, event catalog pages
  - [ ] DLQ handling and replay procedures

Assumptions/Risks (early)

- Apply RLS (not schema-per-tenant) in Phase 1 for speed; revisit at Phase 2 scale.
- Event Sourcing is limited to audit and projections; CRUD with outbox for writers to reduce complexity.
- Dedicated effort for projection robustness to avoid early DLQ accumulation.

Owners (indicative)

- Platform: Gateway, Identity, Audit/Observability
- Core PM: Organization, Project, Projections
- Collaboration: DMS

## 22. Phase 2 Implementation – Procurement, Inventory, Marketplace Foundations

Overview

- Goals (from docs/ConstructHub_Concept_vNext.md Section 16 Phase 2):
  - Enable real procurement and inventory flows; seed early marketplace participation.
  - Deliver mobile delivery receipt for field confirmation with evidence.
- Deliverables:
  - New services: Vendor, Materials/Catalog, Procurement, Inventory.
  - Mobile Delivery Receipt minimal flow (DeliveryConfirmed with evidence) to update Inventory.
  - Marketplace foundations: Tender/Bidding (open/invited) and Credits (debit/refund rules).
- Exit Criteria (Phase 2):
  - Procurement cycle time -30% across pilots.
  - ≥50 tenders created.
  - ≥70% mobile receipt adoption on pilot sites.

Scope Boundaries and Approach

- Event Sourcing used in Procurement and Inventory write paths for auditability; CRUD + outbox in Vendor and Materials. Marketplace foundations use CRUD + outbox, with events as contract.
- Keep Delivery Management minimal in Phase 2: capture DeliveryConfirmed (with photos/signature, geotag/time), optional DeliveryScheduled ingestion later. Full Delivery orchestration deferred to Phase 3.
- RBAC extensions for Vendor Rep persona; multi-tenant enforcement via RLS as in Phase 1.

New/Updated Services (independently deployable)

1. Vendor Service

- Purpose/Scope: Onboard vendors/suppliers, maintain compliance docs, ratings, legal entity profiles.
- Commands/Queries (REST):
  - POST /vendors {tenantId, legalName, taxIds[], documents[], categories[]} → VendorCreated
  - PATCH /vendors/:id {status?=approved|rejected, documents[], categories[]} → VendorUpdated/VendorApproved
  - POST /vendors/:id/ratings {score, rationale} → RatingUpdated
  - GET /vendors/:id, GET /vendors?tenantId=...&category=...
- Domain Events (Kafka topics):
  - supply.vendor.v1: VendorCreated, VendorUpdated, VendorApproved, RatingUpdated
- Write Model (PostgreSQL; RLS on tenant_id):
  - vendors(id, tenant_id, legal_name, status, tax_profile jsonb, categories text[], created_at, updated_at)
  - vendor_documents(id, vendor_id, doc_type, doc_ref, status, expires_at, created_at)
  - vendor_ratings(id, vendor_id, score, rationale, created_at)
- Projections:
  - vendor_directory(tenant_id, id, legal_name, status, rating_avg, categories)

2. Materials/Catalog Service

- Purpose/Scope: Central catalog and price lists; units of measure; optional vendor-specific items.
- Commands/Queries (REST):
  - POST /catalog/items {tenantId, sku?, name, uom, specs, category} → CatalogItemCreated
  - PATCH /catalog/items/:id {name?, uom?, specs?, category?} → CatalogItemUpdated
  - POST /catalog/prices {itemId, vendorId?, currency, price, minQty?, leadTimeDays?} → PriceListed
  - GET /catalog/items?tenantId=...&q=...&category=...
- Domain Events (Kafka topics):
  - supply.catalog.v1: CatalogItemCreated, CatalogItemUpdated, PriceListed
- Write Model:
  - catalog_items(id, tenant_id, sku, name, uom, category, specs jsonb, created_at, updated_at)
  - catalog_prices(id, item_id, vendor_id?, currency, price, min_qty, lead_time_days, valid_from, valid_to)
- Projections:
  - catalog_search index; vendor_price_matrix(item_id, vendor_id, current_price, lead_time_days)

3. Procurement Service

- Purpose/Scope: Procurement requests (RFQ/Tender link), approvals (basic), supplier selection, POs/contracts.
- Commands/Queries (REST):
  - POST /procurement/requests {tenantId, projectId, items[{catalogItemId, qty, uom}], needBy, budgetCode?} → ProcurementRequestCreated
  - POST /procurement/requests/:id/approve {actor} → RequestApproved
  - POST /procurement/requests/:id/issue-po {supplierId, terms, deliveryWindow, shipTo, items[]} → POIssued
  - POST /procurement/requests/:id/link-award {tenderId, bidId} → AwardedBidLinked
  - GET /procurement/requests/:id, GET /procurement/requests?projectId=...&status=...
  - GET /purchase-orders/:id
- Domain Events (Kafka topics):
  - supply.procurement.v1: ProcurementRequestCreated, RequestApproved, POIssued, AwardedBidLinked
- Write Model (ES + outbox):
  - procurement_requests(aggregate with items, status, approvals); events stored with envelopes
  - purchase_orders(id, tenant_id, request_id, supplier_id, terms jsonb, status, created_at)
- Projections:
  - procurement_list(tenant_id, project_id, status, created_at, approved_at, po_issued_at)
  - procurement_cycle_time(tenant_id, project_id, median, p95) built from events for KPI dashboards

4. Inventory Service

- Purpose/Scope: Site inventory, stock ledger, adjustments; integrates with DeliveryConfirmed.
- Commands/Queries (REST):
  - POST /inventory/receive {tenantId, projectId, poId?, items[{catalogItemId, qty, uom}], evidenceRefs[], receivedAt} → StockReceived
  - POST /inventory/adjust {tenantId, projectId, reason, items[{catalogItemId, delta, uom}], evidenceRefs?} → StockAdjusted
  - POST /inventory/reserve {projectId, items[]} → StockReserved; POST /inventory/issue {projectId, items[]} → StockIssued
  - GET /inventory/balances?projectId=...&itemId=...
- Domain Events (Kafka topics):
  - supply.inventory.v1: StockReceived, StockAdjusted, StockReserved, StockIssued
- Write Model (ES for ledger; projection for balances):
  - stock_ledger(event log per item/site); inventory_balances(project_id, item_id, on_hand, reserved, updated_at)
- Projections:
  - inventory_balances view; discrepancy_queue for negative or abnormal events

5. Delivery Receipt (Minimal Delivery Surface)

- Purpose/Scope: Mobile-first capture of delivery confirmation with evidence; emits DeliveryConfirmed to correlate with PO and update inventory.
- Commands/Queries (REST):
  - POST /deliveries/confirm {tenantId, projectId, poId?, supplierId?, items[{catalogItemId, qty, uom}], photos[], signatureRef?, geo?, eta?} → DeliveryConfirmed
  - GET /deliveries/:id
- Domain Events (Kafka topics):
  - supply.delivery.v1: DeliveryConfirmed
- Storage and Linkage:
  - Links evidence to DMS (Document IDs); correlates to PO if provided.
- Notes:
  - Optionally accept DeliveryScheduled ingestion via webhook placeholder; full scheduling/ASN moved to Phase 3.

6. Marketplace Foundations: Tender/Bidding and Credits

- Tender/Bidding Service
  - Commands/Queries (REST):
    - POST /tenders {tenantId, projectId, type=open|invited, category, region, items/specs, openUntil} → TenderOpened
    - POST /tenders/:id/invite {supplierIds[]} → SuppliersInvited
    - POST /tenders/:id/bids {supplierId, offerLines[], validity, attachments[]} → BidSubmitted (requires credit debit)
    - POST /tenders/:id/close {} → TenderClosed; POST /tenders/:id/award {bidId} → TenderAwarded
    - GET /tenders/:id, GET /tenders?projectId=...&status=...
  - Events (Kafka topics):
    - marketplace.tender.v1: TenderOpened, SuppliersInvited, TenderClosed, TenderAwarded
    - marketplace.bid.v1: BidSubmitted
- Credits Service
  - Commands/Queries (REST):
    - POST /credits/purchase {supplierId, packageId} → CreditsPurchased
    - POST /credits/debit {supplierId, tenderId, reason=bid} → CreditLedgerDebited
    - POST /credits/refund {supplierId, tenderId, reason=constructor_cancel} → CreditsRefunded
    - GET /credits/ledger?supplierId=...
  - Events (Kafka topics):
    - marketplace.credits.v1: CreditsPurchased, CreditLedgerDebited, CreditsRefunded
- Notes:
  - Bid submission first calls Credits debit; enforce configurable free credits per month.
  - No commission accrual in Phase 2; that begins Phase 3.

Event Catalog Additions (envelope unchanged v1)

- New event types and topics as listed above; schemas registered in Schema Registry with additive evolution policy.
- Correlation: AwardedBidLinked → POIssued → DeliveryConfirmed → StockReceived chain uses correlationId/causationId for traceability.

API Gateway/BFF Updates

- New routes: /vendors/_, /catalog/_, /procurement/_, /purchase-orders/_, /inventory/_, /deliveries/_, /tenders/_, /credits/_.
- Security:
  - Extend RBAC with VendorRep role; scope by tenant and optional project.
  - Step-up auth optional for PO issue endpoints.

Projections and Reporting (Phase 2)

- Procurement KPIs:
  - procurement_cycle_time per project/tenant (median, p95) from ProcurementRequestCreated → POIssued (and later → DeliveryConfirmed in Phase 3).
- Inventory:
  - inventory_balances view and low-stock alerts (event-driven).
- Marketplace:
  - tender_metrics(tenant_id, open_count, bidders_avg, tenders_created_rolling_30d)
- Mobile Adoption:
  - delivery_receipt_adoption(project_id, total_deliveries, mobile_confirmations, adoption_pct)

Non-Functional Targets (Phase 2)

- Availability: ≥99.9% per critical service and gateway.
- Performance: P95 reads <300 ms; P95 writes <500 ms typical; projection lag P95 <30s.
- Security: MFA recommended for PO issuance; PII safe logs; secrets rotation maintained.
- Observability: Golden path traces for request→approve→issue PO and mobile delivery confirm; SLO dashboards per service.

Testing and Quality

- Contract-first APIs with OpenAPI/AsyncAPI; consumer-driven contract tests.
- Testcontainers-based integration tests for Postgres/Kafka; idempotency tests on projections.
- Load tests (k6) for procurement and tender endpoints to 5x expected pilot load.

Data Model and Isolation

- RLS continues as default; inventory tables partitioned by tenant/project where needed.
- Evidence stored in DMS with envelope encryption; references stored in delivery/procurement events.

Service Contracts Summary (Phase 2)

- Vendor:
  - Commands: createVendor, updateVendor, approveVendor, addRating
  - Queries: getVendor, listVendors
  - Events: VendorCreated, VendorUpdated, VendorApproved, RatingUpdated
- Materials/Catalog:
  - Commands: createCatalogItem, updateCatalogItem, listPrice
  - Queries: getCatalogItem, searchCatalog
  - Events: CatalogItemCreated, CatalogItemUpdated, PriceListed
- Procurement:
  - Commands: createRequest, approveRequest, issuePO, linkAward
  - Queries: getRequest, listRequests, getPO
  - Events: ProcurementRequestCreated, RequestApproved, POIssued, AwardedBidLinked
- Inventory:
  - Commands: receiveStock, adjustStock, reserve, issue
  - Queries: getBalances
  - Events: StockReceived, StockAdjusted, StockReserved, StockIssued
- Delivery (minimal):
  - Commands: confirmDelivery
  - Queries: getDelivery
  - Events: DeliveryConfirmed
- Marketplace:
  - Commands: openTender, inviteSuppliers, submitBid, closeTender, awardTender; purchaseCredits, debitCredits, refundCredits
  - Queries: getTender, listTenders, getCreditsLedger
  - Events: TenderOpened, SuppliersInvited, BidSubmitted, TenderClosed, TenderAwarded, CreditsPurchased, CreditLedgerDebited, CreditsRefunded

Dependencies and Integrations

- Depends on Phase 1 Identity/Project context for tenant/project and RBAC checks.
- DMS used for evidence attachments; API returns documentIds for linkage.

Phase 2 Execution Checklist

- Platform and Contracts
  - [ ] Register new event schemas in Schema Registry; create topics with partitions keyed by aggregateId.
  - [ ] Extend RBAC model with VendorRep role and scopes; update gateway policy.
- Vendor Service
  - [ ] Implement CRUD + approval and events; vendor_directory projection and search endpoints.
- Materials/Catalog Service
  - [ ] Implement item and price endpoints; publish Catalog events; catalog_search projection.
- Procurement Service
  - [ ] Implement request→approve→issue PO flow with ES; emit procurement events; cycle time projection.
- Inventory Service
  - [ ] Implement stock ledger (ES) and balances projection; receive/adjust/reserve/issue endpoints.
- Delivery Receipt
  - [ ] Implement confirmDelivery endpoint; evidence via DMS; emit DeliveryConfirmed; correlate to PO and update Inventory.
- Marketplace Foundations
  - [ ] Implement Tender/Bidding endpoints and events; invite flow; award linkage to Procurement.
  - [ ] Implement Credits debit/refund/purchase and enforce on bid submission.
- Projections/Reporting
  - [ ] Build procurement_cycle_time, inventory_balances, tender_metrics, delivery_receipt_adoption read models.
- Observability and SLOs
  - [ ] Add traces, metrics, alerts for new services; SLO dashboards; synthetic tests for mobile receipt and PO issuance.
- CI/CD and Security
  - [ ] Pipelines per new service; SBOM/image signing; secret management; security scans.
- E2E Validation Against Exit Criteria
  - [ ] Simulate pilot flows to measure cycle time reduction baseline vs Phase 2.
  - [ ] Create 50+ tenders in test; verify credit debits/refunds; adoption telemetry for delivery confirms ≥70%.
- Documentation and Runbooks
  - [ ] Service READMEs, OpenAPI/AsyncAPI; DLQ/replay procedures per new consumer.

Risks and Mitigations (Phase 2 Specific)

- Marketplace liquidity risk (too few bidders):
  - Mitigation: Seed supply, credit promos, invited tenders for pilots; monitor bidders/tender KPI.
- ES complexity in Procurement/Inventory:
  - Mitigation: Keep aggregates small; rigorous idempotency tests; replayable projections with checkpoints.
- Mobile receipt adoption below target:
  - Mitigation: UX optimization, offline-first behaviors, role-based training, nudges/notifications.

Owners (indicative)

- Supply Team: Vendor, Materials/Catalog, Procurement, Inventory, Delivery (minimal).
- Marketplace Team: Tender/Bidding, Credits.
- Platform Team: Gateway updates, Schema Registry, Observability expansion, RBAC updates.

Exit Criteria Validation (mapped to Phase 2)

- -30% procurement cycle time: computed from ProcurementRequestCreated → POIssued median vs baseline per tenant.
- ≥50 tenders: tender_metrics projection shows created count and bidders/tender ≥3 on average for pilots.
- ≥70% mobile receipt adoption: delivery_receipt_adoption shows adoption_pct ≥0.7 across pilot projects.

Implementation Notes

- Keep commission logic and Delivery/Financials orchestration for Phase 3 to minimize risk.
- Ensure AwardedBidLinked is optional: Procurement flows without marketplace must still work.
- Maintain backward compatibility on events and APIs; additive schema evolution only.

### Phase 2 Delivery Timeline and Milestones

- Sprint 1 (Weeks 1–2)
  - Platform and Contracts
    - Schema Registry: register supply.vendor.v1, supply.catalog.v1, supply.procurement.v1, supply.inventory.v1, supply.delivery.v1, marketplace.tender.v1, marketplace.bid.v1, marketplace.credits.v1.
    - Kafka topics bootstrapped with partitions keyed by aggregateId; DLQs per consumer.
    - RBAC update in Gateway: add VendorRep role and route policy skeletons.
  - Vendor Service (foundations)
    - Service scaffold, security (OAuth2 resource server), TenantContext + RLS aspect, application.yml.
    - Flyway V1: vendors, vendor_documents, vendor_ratings tables with RLS.
    - CRUD: createVendor, get/list; publish VendorCreated.
  - Projections
    - vendor_directory consumer and read model; minimal GET for listing.
- Sprint 2 (Weeks 3–4)
  - Materials/Catalog Service
    - Entities/migrations: catalog_items, catalog_prices; CRUD endpoints; events CatalogItemCreated/Updated, PriceListed.
    - Projection: catalog_search and vendor_price_matrix.
  - Procurement Service (ES skeleton)
    - Aggregate: ProcurementRequest with items and approvals; events ProcurementRequestCreated, RequestApproved.
    - Read model: procurement_list; cycle time projection scaffolding (timestamps).
- Sprint 3 (Weeks 5–6)
  - Procurement Service (PO issuance)
    - Commands: issuePO, linkAward; event POIssued, AwardedBidLinked; outbox publishing.
    - Read model: purchase_orders; enrich procurement_list with PO timestamps.
  - Inventory Service
    - ES stock ledger; commands receiveStock/adjust/reserve/issue; events StockReceived/StockAdjusted/StockReserved/StockIssued.
    - Projection: inventory_balances with idempotent upserts.
  - Delivery Receipt (minimal)
    - confirmDelivery endpoint; DeliveryConfirmed event; DMS evidence linkage.
- Sprint 4 (Weeks 7–8)
  - Marketplace Foundations
    - Tender/Bidding endpoints and events; invited suppliers flow; bid submission requires credit debit.
    - Credits ledger endpoints and events; monthly free credits config.
  - Observability and SLOs
    - Golden paths tracing: request→approve→issue PO; confirm delivery→stock receive.
    - SLO dashboards; synthetic journeys; DLQ alerting.
  - E2E and Exit Criteria Validation
    - Simulate pilot flows; generate ≥50 tenders; measure adoption telemetry for delivery confirms.

### Dependencies and Sequencing

- Must-have before service go-live: Schema Registry entries, Kafka topics, RBAC updates in gateway, DMS available for evidence.
- Procurement depends on Vendor and Catalog data; Inventory depends on DeliveryConfirmed and/or receiveStock.
- Tender/Bidding can operate without Procurement integration initially; AwardedBidLinked optional.

### Definition of Ready (DoR) and Definition of Done (DoD)

- DoR per story
  - OpenAPI/AsyncAPI sketch attached; tenancy and roles defined; success metrics identified; test data described.
- DoD per service change
  - API: OpenAPI updated and validated in CI; backward-compatible changes only.
  - Events: Schema registered; version pinned; sample payloads added to event catalog; outbox publishing with idempotency.
  - Data: Flyway migrations reviewed for RLS and indexes; rollback plan documented.
  - Observability: Traces and metrics added; logs with correlationId/tenantId (no PII).
  - Security: Endpoint access rules covered; JWT scopes mapped; secrets from env/ Vault provider.
  - Testing: Unit + integration (Testcontainers) pass; CDC tests for consumers; k6 baseline for hot endpoints.
  - Docs: Service README, runbooks, API and event pages updated.

### Test Strategy Details

- Unit tests for aggregates, services, and validators.
- Integration tests with Testcontainers (Postgres+Kafka) covering:
  - Tenant isolation with RLS policies.
  - Outbox→Kafka end-to-end publishing and idempotent consumption.
  - Projections replay from earliest with checkpointing.
- Contract tests (PACT/WireMock) between gateway and services; CDC for event consumers.
- Load tests (k6) targeting:
  - Procurement request creation/approval/PO issue at 5x pilot baseline.
  - Delivery confirmation burst uploads with media references.
- Chaos experiments: Inject consumer lag and transient DB outages; verify recovery and DLQ handling.

### Operational Readiness Checklist

- Runbooks: DLQ triage, replay procedures, backfill steps, partition rebalancing.
- Alerts: Consumer lag thresholds, DLQ count > 0, P95 read/write SLO breaches, projection lag >30s, auth error spikes.
- Dashboards: Golden paths, per-service SLOs, topic lag, projection freshness, vendor/catalog coverage, tender KPIs.
- Backups/DR: DB backups tested; event retention policies verified; snapshot and restore drills scheduled.
- Security: Secrets rotation cadence; MFA policy for PO issue; access reviews; no PII in logs checks.

### Rollout Plan

- Environments: Dev → Staging → Pilot → Prod.
- Feature flags: Marketplace endpoints behind flags for staged pilots.
- Data seeding: Sample vendors, catalog items, and tender templates for demos and pilot onboarding.
- Backout strategy: Toggle flags; rollback to previous image; replay projections from snapshot; migration down scripts where safe (or expand/contract pattern).

### KPI Measurement Plan

- Procurement cycle time: median from ProcurementRequestCreated → POIssued per tenant/project; rolling 30-day windows; compared to baseline.
- Tenders: Count, bidders/tender average; time-to-award; refund ratios; tracked via tender_metrics.
- Mobile delivery receipt adoption: delivery_receipt_adoption projection with per-project adoption_pct; alerts on projects <70%.
- Inventory accuracy proxy: exception rates from discrepancy_queue; cycle count events in Phase 3+.

### Phase 2 Engineering Backlog (Top Items)

- Platform/Gateway
  - [ ] Extend RBAC (VendorRep) and route policies; propagate X-Tenant-Id header.
  - [ ] Topic bootstrap scripts and schema registration automation in CI.
- Vendor Service
  - [ ] CRUD + approval; events; vendor_directory projection and GET endpoints.
  - [ ] Compliance docs linkage to DMS; rating aggregation.
- Materials/Catalog
  - [ ] Item CRUD; price listing; projections and search endpoints.
- Procurement
  - [ ] Request→approve→issue PO with ES; cycle time projection; award linkage.
- Inventory
  - [ ] Stock ledger (ES) and balances projection; low-stock alerts.
- Delivery Receipt
  - [ ] confirmDelivery; evidence capture; correlation to inventory receive.
- Marketplace Foundations
  - [ ] Open/Invited tenders; bid submission with credit debit; award flow; credits purchase/refund.
- Observability/SLOs
  - [ ] Traces/metrics; DLQ alerting; synthetic tests; golden path dashboards.
