# ConstructHub – Updated Concept and Architecture (vNext)

## 1. Executive Summary

What

- ConstructHub is a multi-tenant construction operations platform that unifies planning, procurement, logistics/delivery, field execution, financial control, collaboration, and compliance in a single system of record.
- It serves construction companies, general contractors, developers, project managers, site engineers/constructors, vendors/suppliers, and service subcontractors across the full project lifecycle.
- Scope includes core modules: Identity & Access, Project Management, Vendor/Subcontractor Management, Materials & Inventory, Procurement, Delivery, Financials, Workflow/Approvals, Notifications/Collaboration, DMS, Reporting & Analytics, and a Marketplace & Bidding system with AI-assisted estimating.

Why

- Projects suffer from siloed information, manual workflows, poor visibility, and fragmented supplier engagement—driving delays, cost overruns, and disputes.
- ConstructHub provides a single source of truth and automates critical flows to reduce procurement cycle time, increase delivery accuracy, and keep transactions on-platform with full audibility.
- Business impact: faster decisions, fewer errors and claims, improved supplier competition, and more predictable cashflow through progress-linked payments.

How

- Product: digital procurement and delivery confirmations, mobile-first field updates with photos/signatures, catalog-driven materials, configurable approval workflows, and progress-to-payment linkage for subcontractors.
- Marketplace: open/invited tenders with credit-based bidding and premium visibility; fair, category-specific commissions with project-based caps to encourage loyalty; commissions triggered only upon site-confirmed delivery or verified milestone.
- Architecture: Domain-Driven Design with bounded contexts, CQRS and targeted Event Sourcing for high-audit domains, Kafka-backed event backbone with replayable projections for low-latency reads.
- Security and multi-tenancy: OAuth2/OIDC via Keycloak with tenant-scoped claims, RBAC, strong data isolation (RLS or schema-per-tenant), encrypted storage, and comprehensive audit trails via event streams.
- Operations: microservices on Kubernetes, PostgreSQL for OLTP, S3 for documents, optional Elasticsearch for search, Redis for caching, and end-to-end observability (logs, metrics, traces).

## 2. Problem Statement and Market Context

What problems exist

- Siloed information across email, spreadsheets, messaging apps, and paper leads to lost context, duplicate data entry, and poor traceability.
- Manual procurement processes (quotations, approvals, POs) create bottlenecks, inconsistent pricing, and weak audit trails.
- Paper-based or ad-hoc delivery reporting delays inventory updates and hides discrepancies until it is costly to fix.
- Limited real-time visibility into progress, inventory, and commitments prevents proactive management and accurate forecasting.
- Communication gaps between contractors, subcontractors, and vendors result in misaligned expectations and rework.
- Decentralized document management causes versioning issues and increases contractual and safety risk.
- Supplier engagement is fragmented, with no standard mechanism to run fair, scalable tenders across materials and services.
- Off-platform leakage: after initial contact, deals move offline to avoid fees, undermining marketplace sustainability.
- Compliance and claims: insufficient auditable history complicates dispute resolution and regulatory obligations.

Why it matters

- Delays and cost overruns erode thin construction margins and tie up working capital.
- Lack of auditability increases dispute costs and insurance exposure.
- Poor inventory accuracy leads to urgent purchases, waste, and schedule slips.
- Fragmented supplier processes reduce competition and raise prices.
- Market trends (supply chain volatility, labor shortages, digital mandates) require data-driven operations and resilient sourcing.

How ConstructHub addresses these

- Unify data and context: a shared, tenant-isolated system of record with linked documents, comments, and events across modules.
- Automate workflows: guided procurement, configurable approvals, and mobile delivery confirmations with evidence to eliminate manual steps.
- Real-time visibility: event-driven updates feed projections for up-to-date dashboards on progress, stock, and financials.
- Standardize supplier engagement: integrated marketplace with open/invited tenders, reputation, and flexible quoting units for services.
- Prevent off-platform leakage: commission recognized only upon confirmed delivery/verified milestone, with project-level commission caps that reward volume and promote on-platform retention.
- Strengthen compliance: immutable event streams, versioned documents, and fine-grained RBAC support audits and legal defensibility.
- Integration-ready: APIs and webhooks connect ERP, BIM, and logistics partners for end-to-end data flow.

## 3. Business Objectives (Measurable)

What

- A concise, outcome-driven set of targets that define success for our customers and the business. Objectives guide prioritization, pricing, and GTM, and are reported per tenant, per project, and globally.

Why

- Aligns stakeholders around value, not features; proves ROI for buyers; ensures engineering focuses on cycle time, cost control, auditability, and marketplace liquidity rather than vanity metrics.

How

- Instrument end-to-end events with timestamps for all core flows (procurement, delivery, inventory, bidding, approvals).
- Define formulas, owners, and review cadence (monthly QBRs), with per-tenant baselines captured during pilot onboarding.
- Build productized dashboards on projections; use cohort analysis to track improvement after enablement.

Targets (definitions, baselines, and data sources)

- Procurement Cycle Time: median time from ProcurementRequest.created to PurchaseOrder.approved.
  - Target: -40% by month 6, -60% by month 12 vs. tenant baseline.
  - Data: ProcurementRequestCreated, POApproved events; filtered by project/tenant.
- On-time Delivery Confirmation Rate: deliveries confirmed within 24h of scheduled ETA.
  - Target: +30% by month 6.
  - Data: DeliveryScheduled, DeliveryConfirmed events; mobile receipt adoption tracked.
- Inventory Accuracy: 1 - |system_qty - verified_qty| / verified_qty across cycle counts.
  - Target: >98% accuracy.
  - Data: InventoryAdjusted, CycleCountCompleted events; exception queue for discrepancies.
- Document Handling Effort: minutes per document from upload to approval (or docs processed per FTE/week).
  - Target: -50% within 6 months.
  - Data: DocumentUploaded, DocumentReviewed/Approved, workflow task metrics.
- On-Platform Retention: % of awarded tenders that result in on-platform confirmed deliveries/invoices.
  - Target: ≥95%.
  - Data: TenderAwarded, DeliveryConfirmed, InvoiceIssued events; leakage alerting if missing links.
- Performance and Availability SLOs: P95 read <300 ms; P95 write <500 ms; availability ≥99.9% (Phase 2+).
  - Data: APM/observability metrics; synthetic tests.
- Adoption: ≥100 paying tenants and ≥10k MAUs by end of Phase 3; DAU/MAU >0.35 in active projects; ≥3 active tenders per procurement phase per project.
  - Data: Auth/session logs, marketplace events.

Guardrails

- Event capture completeness ≥99%; projection lag P95 <30s; 0 critical security incidents; data retention and privacy policies enforced per region.

## 4. Customer Segments and Personas

What

- The primary customer types and role-based personas who execute core jobs in ConstructHub. These drive permission models, workflows, and UX defaults.

Why

- Ensures the product solves the right problems for each role, reduces change management, and accelerates adoption across office and field.

How

- Define JTBD for each persona; map journeys and SLAs; ship role-based templates (dashboards, checklists, approvals); mobile-first for field roles, desktop-first for office; provide APIs/integrations for vendor/subcontractor systems.

Segments

- Organizations: Construction companies, general contractors, developers (buyers/operators).
- Vendors: Material suppliers and logistics providers (supply side for goods and deliveries).
- Subcontractors: Service trades (e.g., concrete, MEP, finishing) bidding and executing work.
- Partners: ERP, BIM, and finance system integrators.

Personas

- Organization Administrator
  - Goals: Secure, compliant tenant operations; fast onboarding; least-privilege access.
  - Pains: Permission sprawl, audit gaps, SSO complexity.
  - Top Workflows: Create org/projects; assign roles; configure SSO; manage policies; review audit logs.
  - Success: Time-to-onboarding <1 day; 0 critical access incidents; audit readiness.
- Project Manager
  - Goals: Deliver on-time/on-budget with quality; minimize risk and rework.
  - Pains: Disconnected data, slow approvals, unclear commitments.
  - Top Workflows: Create/approve procurement; manage budgets; track deliveries; oversee progress and change orders.
  - Success: Schedule variance <5%; procurement cycle time reduced; budget adherence within tolerance.
- Site Engineer / Constructor
  - Goals: Keep crews productive; ensure materials and instructions are available on time.
  - Pains: Waiting for materials, paper-based receipts, unclear priorities.
  - Top Workflows: Request materials; confirm deliveries with photos/signatures; submit progress claims; log defects/issues.
  - Success: >90% delivery confirmations within 24h; fewer stockouts; faster issue resolution.
- Vendor Representative
  - Goals: Win profitable bids; fulfill accurately and on time; minimize disputes.
  - Pains: Scattered tenders, opaque evaluations, delayed confirmations.
  - Top Workflows: Discover/invite tenders; spend credits and submit bids; confirm shipments (ASN); upload invoices.
  - Success: Bid-to-win >20%; on-time delivery >95%; dispute rate <2%.
- Subcontractor PM
  - Goals: Predictable cash flow; clear scope and measurements; timely approvals.
  - Pains: Ambiguity on quantities, milestone verification delays.
  - Top Workflows: Submit service bids with units (sqm, m3, per-point, lump-sum); request milestone verification; generate invoices.
  - Success: Approval lead time <3 days; variation orders processed <5 days; payment on agreed terms.
- Finance / Controller
  - Goals: Spend control, clean audits, timely close.
  - Pains: Poor audit trails, mismatched commitments vs. actuals.
  - Top Workflows: Set budgets; map to chart of accounts; approve invoices; reconcile accruals; export to ERP.
  - Success: Monthly close <5 days; 0 material audit findings; variance thresholds maintained.

Access Model Notes

- Default RBAC roles map to personas with per-project overrides; vendor/subcontractor tenants are isolated; legal entity and tax profiles support invoicing and compliance.

## 5. Core Value Propositions

What

- Role-based, measurable outcomes that reduce time, cost, and risk while improving predictability, collaboration, and compliance.

Why

- Clarifies why each persona buys and adopts; informs packaging, pricing metrics, and onboarding priorities; ties to Section 3 objectives.

How

- Delivered via a unified system of record, guided workflows with approvals, event-driven updates, mobile evidence capture, and an integrated marketplace linked to logistics and payments.

Value by persona (with indicative KPIs)

- Project Managers
  - Value: Single source of truth for scope, costs, inventory, and progress; faster decisions with fewer surprises.
  - KPIs: -40–60% procurement cycle time; schedule variance <5%; change order lead time ↓.
- Site Teams (Site Engineer / Constructor)
  - Value: Materials arrive when needed; quick delivery confirmations with photos/signatures; less admin.
  - KPIs: +30% on-time confirmations; stockouts ↓; rework incidents ↓.
- Vendors / Suppliers
  - Value: Access to qualified demand, transparent tenders, and faster dispute-free fulfillment.
  - KPIs: Bid-to-win rate ↑; dispute rate <2%; DSO ↓ via cleaner confirmations.
- Subcontractors
  - Value: Clear scopes and units, verified milestones, predictable cashflow through progress-linked invoicing.
  - KPIs: Approval lead time <3 days; variation order turnaround <5 days.
- Finance / Controllers
  - Value: Real-time spend and commitments, audit-ready trails, faster close.
  - KPIs: Monthly close <5 days; variance thresholds maintained; 0 material audit findings.
- Executives / Owners
  - Value: Portfolio visibility, risk signals, and savings from competitive sourcing and waste reduction.
  - KPIs: Cost savings vs. baseline; on-time delivery rate; on-platform retention ≥95%.

## 6. Functional Modules (Scope)

What

- Capability map and minimum viable scope per module to ensure clear bounded contexts and scalable teams/APIs.

Why

- Reduces cross-context coupling, improves delivery velocity, and enables independent scaling and compliance.

How

- Each module exposes commands, events, and queries; uses shared identity and tenancy; and contributes projections for reporting.

Modules (scope and key artifacts)

- Identity & Access
  - Scope: Users, Organizations/Tenants, Memberships, RBAC/permissions, SSO/OIDC.
  - Entities/Events: User, Membership, Role; UserCreated, RoleAssigned, TenantCreated.
  - Notes: Tenant isolation (RLS/schema), policy packs, audit logs.
- Project Management
  - Scope: Organizations, Projects, SubProjects, team assignments, project metadata.
  - Entities/Events: Project, SubProject; ProjectCreated, UserAssignedToProject.
  - Notes: Drives scoping for permissions and reporting.
- Vendor & Subcontractor Management
  - Scope: Onboarding, compliance docs, catalogs/price lists, portfolios, ratings.
  - Entities/Events: Vendor, Subcontractor, CatalogItem; VendorApproved, RatingUpdated.
  - Notes: Reputation feeds marketplace ranking; legal entity profiles for invoicing.
- Materials & Inventory
  - Scope: Central catalog, site inventory, units of measure, stock ledger.
  - Entities/Events: Material, InventoryItem; StockReceived, StockReserved, StockIssued, StockAdjusted.
  - Notes: Supports BOMs/kitting (Phase 3+), cycle counts, and discrepancy workflows.
- Procurement
  - Scope: Procurement requests (RFQ/Tender), approvals, supplier selection, POs/contracts.
  - Entities/Events: ProcurementRequest, PurchaseOrder; RequestApproved, POIssued, AwardedBidLinked.
  - Notes: Integrates with marketplace for competitive sourcing; templates per category.
- Delivery Management
  - Scope: ASN, scheduling, arrival, site receipt, discrepancies/shortages/returns.
  - Entities/Events: Delivery, Shipment, Discrepancy; DeliveryScheduled, DeliveryConfirmed, DiscrepancyRaised.
  - Notes: Mobile-first confirmations with photos/signatures; updates inventory and financials.
- Financials
  - Scope: Budgets, commitments, accruals, invoice matching, tax settings; ERP integration.
  - Entities/Events: Budget, Commitment, Invoice; BudgetSet, CommitmentRecorded, InvoiceApproved.
  - Notes: Mapping to chart of accounts; export APIs/webhooks.
- Workflow & Approvals
  - Scope: Configurable chains, SLAs, escalations, delegation, holidays.
  - Entities/Events: Workflow, Task; TaskCreated, TaskApproved, SLADeadlineBreached.
  - Notes: Used by procurement, financials, documents, and progress claims.
- Notifications & Collaboration
  - Scope: Alerts, inbox, mentions, comments, task assignments.
  - Entities/Events: Notification, Comment; NotificationSent, CommentAdded.
  - Notes: Channel preferences (email, push, in-app), digesting, rate limits.
- Subcontractor Progress
  - Scope: Work packages, measurements, progress claims, verification.
  - Entities/Events: WorkItem, ProgressClaim; ProgressClaimSubmitted, MilestoneVerified.
  - Notes: Payment linkage to verified work; integrates with Financials.
- Reviews & Sign-offs
  - Scope: Formal reviews (drawings, method statements), sign-offs, revision control.
  - Entities/Events: Review, Approval; ReviewRequested, ReviewApproved.
  - Notes: Tied to DMS and project governance.
- Document Management (DMS)
  - Scope: Versioned storage, metadata, OCR/AI extraction, retention policies.
  - Entities/Events: Document; DocumentUploaded, DocumentVersioned, RetentionApplied.
  - Notes: S3-backed storage; content hashing; access control per project/entity.
- Reporting & Analytics
  - Scope: KPI dashboards, exports, ad-hoc queries, data products.
  - Entities/Events: Projection, Report; ProjectionUpdated, ReportGenerated.
  - Notes: Built on projections; supports data warehouse feeds (Phase 4+).
- Marketplace & Bidding
  - Scope: Tenders (open/invited), credits, bids, awards, ranking, commission tracking.
  - Entities/Events: Tender, Bid, CreditLedger, Commission; BidSubmitted, TenderAwarded, CommissionAccrued.
  - Notes: Anti-disintermediation via delivery/milestone linkage and commission caps.
- AI Material Estimator
  - Scope: Early-stage quantity/cost ranges; learning from historical project data.
  - Entities/Events: EstimationRun; EstimationRequested, EstimationCompleted.
  - Notes: Guardrails and confidence intervals; not a substitute for engineering takeoff.

## 7. High-Level Business Workflows

What

- End-to-end processes that span multiple modules and stakeholders. Each workflow is modeled as commands that mutate aggregates, domain events that record facts, and projections that power real-time visibility and KPIs.

Why

- Standardizing workflows reduces cycle times, improves quality, and ensures auditability. Clear SLAs, evidence capture, and compensations minimize disputes and rework.

How

- Orchestrate via sagas (process managers) and event-driven interactions. Each step emits events with timestamps for measurement. Mobile-first steps include offline support and media evidence.

Key workflows (actors, steps, events, SLAs)

1. Procurement → Delivery → Financials

- Actors: Project Manager, Approvers, Vendor, Site Engineer, Finance.
- Preconditions: Budget available; materials defined in catalog; vendor is onboarded.
- Steps
  1. Create Request: Command ProcurementRequest.Create -> Event ProcurementRequestCreated.
  2. Approve: Workflow task -> RequestApproved.
  3. Tender/Bid (optional): TenderOpened -> BidSubmitted* -> TenderAwarded (links AwardedBid to request).
  4. PO Issue: PurchaseOrder.Issue -> POIssued.
  5. Shipment/ASN: Delivery.Prepare -> ShipmentDispatched/ASNCreated.
  6. Site Receipt: Delivery.Confirm (mobile, photos/signature) -> DeliveryConfirmed; discrepancies -> DiscrepancyRaised.
  7. Inventory Update: StockReceived/StockAdjusted.
  8. Financial Update: CommitmentRecorded; InvoiceIssued -> InvoiceApproved.
  9. Commission: CommissionAccrued upon DeliveryConfirmed (if marketplace category and pre-cap); cap logic applied per project/category.
- Postconditions: Inventory and commitments up to date; audit trail complete; exceptions queued.
- SLAs/Controls: Approvals within 48h; delivery confirmation within 24h of ETA; auto-escalations; tamper-evident evidence hashes.

2. Material Delivery and Discrepancy Resolution

- Actors: Vendor, Site Engineer, PM, Finance.
- Steps
  1. Schedule: DeliveryScheduled.
  2. Confirm: DeliveryConfirmed (capturing quantities, photos, geotag/time).
  3. Discrepancy: DiscrepancyRaised (short/over/damage) -> TaskCreated for resolution.
  4. Resolve: DiscrepancyResolved -> StockAdjusted; optional CreditNoteIssued.
- SLAs: Discrepancies triaged <24h; resolved <5 business days.

3. Marketplace Bidding (Materials/Services)

- Actors: PM, Vendors/Subcontractors.
- Steps
  1. Tender Creation: TenderOpened (open/invited; category/region; bid units for services).
  2. Participation: CreditLedgerDebited when supplier places BidSubmitted; GoldenBid flag for premium visibility.
  3. Evaluation/Award: TenderEvaluated -> TenderAwarded; non-awarded credit refunds if constructor cancels.
  4. Logistics Link: AwardedBidLinked to PO; commission only on DeliveryConfirmed or MilestoneVerified.
- SLAs: Tender open period configurable; evaluation target <7 days.

4. Subcontractor Progress → Payment

- Actors: Subcontractor PM, Site Engineer, Finance.
- Steps
  1. Claim: ProgressClaimSubmitted with measurements/evidence.
  2. Verify: MilestoneVerified by site; discrepancies -> ReworkRequested.
  3. Invoice: InvoiceIssued based on verified quantities and agreed rates -> InvoiceApproved.
- SLAs: Verification <3 days; payment per contract terms; variation orders <5 days.

5. User Assignment and Access Provisioning

- Actors: Org Admin, Project Admin.
- Steps
  1. Membership: MembershipCreated; assign RoleAssigned with project/subproject scope.
  2. Permissions: Authorization projections updated; session tokens reflect new claims.
- Controls: Least privilege by default; periodic access reviews.

6. Document Lifecycle

- Actors: Any role, Reviewers.
- Steps
  1. Upload: DocumentUploaded with metadata; virus scan and content hashing.
  2. Review/Approve: ReviewRequested -> ReviewApproved/Rejected; new versions -> DocumentVersioned.
  3. Linkage: Documents linked to entities (request, PO, delivery, claim) for full traceability.
  4. Retention: RetentionApplied per policy; legal hold supported.
- SLAs: Review within configured windows; immutable audit of all actions.

## 8. Domain-Driven Design Overview

What

- A domain-centric architecture that divides the platform into bounded contexts owned by autonomous teams. Each context defines its model, invariants, and language, exposing APIs and events.

Why

- Manages complexity, preserves model integrity, and enables independent evolution and scaling. Prevents concept drift and tight coupling that slow delivery and increase risk.

How

- Bounded Contexts and Ownership
  - Identity & Access: Authentication, RBAC, tenancy. Owner: Platform team.
  - Organization & Project Management: Org/Project/SubProject lifecycle. Owner: Core PM team.
  - Supply Chain: Vendors, Materials, Procurement, Delivery. Owner: Supply team.
  - Financials: Budgets, commitments, invoices. Owner: Finance team.
  - Collaboration: Workflow, Notifications, DMS. Owner: Collaboration team.
  - Execution: Subcontractors, Progress, Reviews. Owner: Execution team.
  - Marketplace & Bidding: Tenders, credits, commissions. Owner: Marketplace team.
  - Reporting & Analytics: Projections, reports, data products. Owner: Data team.

- Aggregate Design (examples and rules)
  - Examples: Organization, Project, SubProject, Membership, User, Role, Permission, Material, InventoryItem, ProcurementRequest, PurchaseOrder, Delivery, Tender, Bid, ProgressClaim, Invoice, Document.
  - Rules: Keep aggregates small with clear invariants; commands validate and emit events; version events for evolution; prefer eventual consistency across contexts.

- Consistency and Coordination
  - Within aggregate: strong consistency via command handlers and invariants.
  - Across aggregates/contexts: sagas/process managers and the transactional outbox pattern; retries/idempotency to handle at-least-once delivery.
  - Read models: purpose-built projections per tenant/project for low-latency queries; replayable from Kafka with schema registry.

- Event Sourcing and CQRS Strategy
  - Apply Event Sourcing in high-audit domains (Procurement, Delivery, Financials, Marketplace); use CRUD with outbox in simpler contexts (e.g., Identity, basic catalogs) to reduce complexity.
  - Commands, Events, and Projections form the contract; queries hit denormalized stores for performance.

- Integration and Boundaries
  - Anti-Corruption Layer (ACL) for ERP/BIM/logistics integrations to protect core models; mapping objects and translation layers at the boundary.
  - API styles: REST for commands/queries; async events for state changes; webhooks for external consumers.

- Ubiquitous Language and Governance
  - Maintain a living glossary per context; ensure naming consistency across APIs, events, and UI.
  - Change management: proposal → review → versioning → migration guides; backward compatibility for events and APIs.

- Multi-Tenancy in the Domain
  - Tenant context carried in commands and events; data isolation enforced in write models and projections; document storage partitioned per tenant and project.

## 9. Software Architecture

What

- A domain-aligned, event-driven microservices architecture that balances auditability, performance, and operability. It uses DDD with CQRS and targeted Event Sourcing, Kafka for event distribution, and purpose-built read models for low-latency queries.

Why

- Ensures strong audit trails and traceability for disputes and compliance, scales independently per capability, supports multi-tenancy with isolation, and provides resilient, observable operations across office and field use cases.

How

- Logical Components
  - API Gateway/BFF: Single ingress with auth, rate limiting, request shaping; optional BFFs per client (web/mobile) to reduce chatty calls.
  - Microservices per Bounded Context: Independent deployables for Identity & Access, Project Management, Supply Chain (Vendors, Materials, Procurement, Delivery), Financials, Collaboration, Execution, Marketplace, Reporting.
  - Event Backbone: Apache Kafka with topics per aggregate type; partition keys chosen for ordering (e.g., aggregateId). Schema Registry (Avro/JSON Schema) for evolution.
  - Read Models/Projections: Services consume events to build denormalized stores for queries and dashboards; fully replayable for rebuilds.
  - Data Stores: PostgreSQL for OLTP write models; S3-compatible object storage for documents; Elasticsearch optional for search; Redis for caching and ephemeral coordination.
  - Identity: Keycloak for OAuth2/OIDC, SSO; JWT includes tenant, roles, and scopes; service-to-service uses mTLS and short-lived tokens.
  - Observability: OpenTelemetry traces, centralized logs, Prometheus metrics, alerting; audit from event store and secured logs.
  - Delivery Infrastructure: Kubernetes for orchestration; GitOps for deployments; feature flags and canary/blue-green rollouts.

- Data and Event Design
  - Events are immutable facts with IDs, timestamps, tenant, correlation/causation IDs, and version. Backward-compatible schemas; deprecate via additive changes.
  - Idempotency: Deduplication keys on consumers; idempotent producers; outbox/inbox pattern for exactly-once effects in write paths.
  - Dead Letter and Retry: Per-consumer retry with exponential backoff; DLQ topics for poisoning messages; operational playbooks for reprocessing.
  - Projections: Built with at-least-once consumption and idempotent upserts; can be dropped and replayed; snapshotting where rebuild time is long.
  - Event Retention: Long-lived or tiered storage (e.g., Kafka tiered storage/S3 sinks) to enable history replay and audits.

- Consistency and Coordination
  - Within an aggregate: Strong consistency via command handlers enforcing invariants.
  - Across services: Sagas/process managers; transactional outbox for publishing domain events atomically with state changes (CDC optional).
  - Example Saga: Procurement → PO → Delivery → Inventory → Financials; compensations handle cancellations, returns, and discrepancies.

- Multi-Tenancy and Isolation
  - Tenant carried in all commands/events and enforced in read/write models.
  - Isolation choices: schema-per-tenant for stricter isolation at lower tenant counts; RLS for high tenant density; storage partitioning per tenant/project; per-tenant encryption keys (optional) for documents.
  - Rate limiting and quotas per tenant; noisy-neighbor protections in cache and read models.

- Security-by-Design
  - AuthN/Z: OAuth2/OIDC via Keycloak; RBAC with project/subproject scoping; service accounts with least privilege; mTLS within cluster; network policies/ingress WAF.
  - Data Protection: TLS in transit; encryption at rest for DB, Kafka, and object storage; secret management via KMS/Vault; key rotation.
  - Auditing: Every state change emits an event; append-only audit log secured and tamper-evident.

- Reliability and Resilience
  - Patterns: Timeouts, retries with jittered backoff, circuit breakers, bulkheads.
  - Degradation: Read models remain available if some writers lag; graceful feature degradation under load.
  - Backups/DR: Regular DB backups; event log retention and snapshots; RPO ≤ 5 min via log-based recovery; multi-AZ by default, optional multi-region.

- Performance and Scalability
  - Horizontal scaling of stateless services; autoscaling on CPU/RPS/lag.
  - Kafka partitions sized by throughput per aggregate; hot partition detection and sharding strategies.
  - Caching: Redis for session/inbox locks and hot entity projections; cache invalidation via events.

- Deployment and Quality
  - CI: Build, test, security scan, SBOM generation; CD via GitOps (Argo/Flux) with canary analysis.
  - Testing: Contract tests (consumer-driven), integration testbeds with ephemeral environments, performance and chaos tests.
  - Versioning: Semantic versioning for services and event schemas; migration guides for breaking changes.

## 10. Capability-to-Service Mapping (Indicative)

What

- Clear mapping from business capabilities to independently deployable services, with stable contracts and owned event schemas.

Why

- Establishes ownership, isolates failures, enables targeted scaling, and clarifies API boundaries and data stewardship.

How

- Guiding principles: one capability → one primary service; aggregates define write models; projections are separate read stores; events are the cross-context contract.

Mappings (scope, APIs, data, events)

- Identity & Access
  - Services: User, Membership, Authorization.
  - APIs: REST (user/membership mgmt), OIDC for auth; SCIM (phase 3+) for provisioning.
  - Data: PostgreSQL (tenant, users, roles); cache for tokens/permissions.
  - Events: UserCreated, RoleAssigned, TenantCreated.
- Project Management
  - Services: Organization, Project.
  - APIs: REST for org/project CRUD and assignments.
  - Data: PostgreSQL; projections for team rosters.
  - Events: ProjectCreated, SubProjectCreated, UserAssignedToProject.
- Supply Chain
  - Services: Vendor, Materials, Procurement, Delivery.
  - APIs: REST for catalogs, RFQs/requests, POs, ASN/delivery; webhook callbacks.
  - Data: PostgreSQL write models; projections for availability and lead times.
  - Events: VendorApproved, CatalogItemUpdated, ProcurementRequestCreated, POIssued, DeliveryScheduled, DeliveryConfirmed, DiscrepancyRaised.
- Financials
  - Services: Financials (budgets, commitments, invoices).
  - APIs: REST for budgets, invoices; connectors for ERP; webhooks for approvals.
  - Data: PostgreSQL; ledger-like projections; secure export feeds.
  - Events: BudgetSet, CommitmentRecorded, InvoiceIssued, InvoiceApproved.
- Execution
  - Services: Subcontractor, Progress, Review.
  - APIs: REST for work packages and progress claims; mobile-friendly endpoints.
  - Data: PostgreSQL; projections for site dashboards.
  - Events: WorkItemCreated, ProgressClaimSubmitted, MilestoneVerified, ReviewApproved.
- Collaboration
  - Services: Workflow, Notification, DMS.
  - APIs: REST for workflow templates and tasks; upload APIs for documents; push/poll for notifications.
  - Data: PostgreSQL (workflow/tasks), S3 (documents), Redis (queues).
  - Events: TaskCreated, TaskApproved, NotificationSent, DocumentUploaded, DocumentVersioned.
- Marketplace
  - Services: Tender/Bidding, Credits, Commission.
  - APIs: REST for tenders/bids; credit purchase via payment gateway; admin rate cards.
  - Data: PostgreSQL (tenders, bids, credit ledger, commission ledger).
  - Events: TenderOpened, BidSubmitted, TenderAwarded, CreditLedgerDebited, CommissionAccrued, CommissionCapReached.
- Reporting & Analytics
  - Services: Reporting/Analytics (+ ETL/warehouse later).
  - APIs: Query/report endpoints; export jobs; data-sharing agreements.
  - Data: Projections in Postgres/Elastic; optional lake/warehouse sinks.
  - Events: ProjectionUpdated, ReportGenerated.

Operational boundaries

- Each service owns its schema and event types; backward-compatible schema evolution policy.
- Rate limits and SLOs defined per service; circuit breakers between gateway and services.
- Failure isolation: crash-only design; DLQs per consumer; graceful degradation for non-critical projections.

## 11. Marketplace & Bidding Details

What

- A two-sided marketplace connecting constructors to material suppliers and service subcontractors, with structured tenders, a credit economy, and a fair commission model tied to confirmed delivery or verified milestones.

Why

- Improves price discovery and reliability, increases competition, and creates sustainable monetization that aligns with project outcomes rather than clicks or impressions.

How

- Tender Types and Participation
  - Open Bidding: Visible to all qualified suppliers in region/category; prequalification filters (certifications, capacity, SLA).
  - Invited Bidding: Restricted to a curated list by the PM; used for sensitive or specialized scopes.
  - Credit Economy: Monthly free credits per supplier; additional purchase via gateway. BidSubmitted debits credits; refunds occur if tender is system-cancelled or materially altered before evaluation; no refunds for losing bids.
  - Golden Bids: Premium placement flag purchased per bid for visibility; limited per tender to avoid crowding.

- Bid Structure and Evaluation
  - Materials: Price/unit, lead time, incoterms, min order qty, warranty, validity period, attachments (specs).
  - Services: Pricing unit (sqm, m3, per-point, lump-sum), crew size, availability window, method statement, portfolio links.
  - Scoring: Weighted score combining price, lead time, supplier rating, on-time delivery history, and bid completeness. Weights configurable per category.
  - Q&A and Revisions: Clarification forum; revision windows logged with versioned bids; audit-preserved.

- Commission Model and Caps
  - Accrual: CommissionAccrued only after DeliveryConfirmed (materials) or MilestoneVerified (services).
  - Rate Cards: Tiered, category-specific rates reflecting margin structures (e.g., lower tonnage-based for steel, higher percentage for finishes).
  - Project-Based Cap: Per project and category, once CommissionLedger reaches CapAmount, further sales in that project/category are commission-free for that supplier. CommissionCapReached event emitted.
  - Leakage Prevention: Cap incentives, on-platform invoicing, and confirmation linkage reduce off-platform moves.

- Logistics and Financial Integration
  - Correlation: AwardedBidLinked to PO, which links to Delivery and Invoice for end-to-end traceability.
  - Discrepancies: If DiscrepancyRaised leads to partial returns, commission is pro-rated on accepted quantities only.
  - Payments: Optional escrow or milestone-based release; invoices auto-generated from verified quantities and rates.

- Fairness, Safety, and Anti-Abuse
  - Verification: KYC/KYB for suppliers; duplicate-account detection; rate limiting; anomaly detection for collusion (suspicious bidding patterns).
  - Privacy: Sealed bids until closing; optional two-stage auctions for services.
  - Policy: Clear terms for cancellations, SLA breaches, and dispute windows; automatic credit refunds on constructor-initiated cancellation post-bidding.

- Compliance and Tax
  - Tax Handling: VAT/GST configured by jurisdiction; tax IDs stored per supplier; compliant invoicing fields.
  - Recordkeeping: Immutable event log for tenders/bids/awards; retention policies by region.

- Notifications and UX
  - Alerts: Invite received, bid outbid (if permitted), Q&A responses, award decision, delivery confirmation required.
  - Mobile-first participation for suppliers; offline draft/save; localized units and currency.

- KPIs and SLAs
  - KPIs: Tender fill rate, average bidders per tender, time-to-award, price delta vs. baseline, on-platform retention post-award, dispute rate.
  - SLAs: Tender open window (configurable), evaluation target <7 days, credit refunds processed within 24h of cancellation, delivery confirmation within 24h of ETA.

## 12. Security and Compliance

- Authentication: OAuth2 + OIDC (Keycloak); PKCE for mobile/web.
- Authorization: RBAC; optional ABAC for project/subproject constraints.
- Tenant Isolation: App-layer checks + DB isolation (RLS or schema split) + storage partitioning.
- Data Protection: At-rest encryption (DB, S3), TLS in transit, secret management (KMS).
- Auditability: Event-sourced history for all state changes; immutable logs.
- Compliance Roadmap: GDPR readiness (DSARs, data retention), SOC 2 controls (change mgmt, access, logging).

## 13. Data Model and Storage Notes

- ERDs: Provided per bounded context (to be generated); naming conventions and key strategies defined.
- Event Catalog: Canonical events per aggregate (created/updated/approved/received/etc.).
- Projections: Tenant- and project-scoped read models; idempotent consumers; replayable.

## 14. Non-Functional Requirements (Targets)

- Availability: ≥99.9% (Phase 2), target 99.95% (Phase 3+).
- Performance: Reads P95 <300 ms; writes P95 <500 ms for typical commands.
- Scalability: Horizontal scaling of stateless services; Kafka partitions sized per aggregate throughput.
- Reliability: Exactly-once semantics in projections where feasible; at-least-once with idempotency otherwise.
- RTO/RPO: RTO ≤ 2 hours, RPO ≤ 5 minutes (event/log-based restore).

## 15. Technology Stack

- Frontend: React, TypeScript.
- Backend: Java, Spring Boot.
- Messaging: Apache Kafka (+ schema registry).
- Database: PostgreSQL.
- Search/Cache: Elasticsearch (optional), Redis.
- Storage: S3-compatible object storage.
- AuthN/Z: Keycloak (OAuth2/OIDC, JWT).
- Deployment: Docker, Kubernetes; GitOps pipelines.
- Observability: OpenTelemetry, centralized logging, metrics.

## 16. Roadmap (Phased)

- Phase 1 (MVP): Identity & Access, Organizations, Projects, basic DMS, initial projections, core observability.
- Phase 2: Vendors, Materials, Procurement, Inventory, mobile delivery receipt, marketplace foundations (credits, tenders).
- Phase 3: Deliveries, Financials (budgets/commitments), Workflow & Approvals, commission logic, subcontractor progress.
- Phase 4: Reporting & Analytics, AI Document Analysis (OCR), AI Material Estimator v1.
- Phase 5: Advanced Marketplace (Golden Bids, reputation), AI Planning, mobile apps GA, external ERP integrations.

## 17. Business Model and Pricing

- Primary Customers: Constructors/GCs; Secondary: Material suppliers and service subcontractors.
- Revenue Streams:
  - Transaction Commissions: Tiered, category-specific rates; project-specific commission cap per category—post-cap sales become commission-free within that project/category.
  - Sale of Bidding Credits: Packages for increased participation.
  - Premium Placement: Golden Bids and featured listings.
  - SaaS Subscriptions: Tiered (Starter/Professional/Enterprise) with limits on projects, users, OCR/AI usage.
- Anti-Disintermediation: Commission cap, logistics linkage, and in-app invoicing reduce off-platform leakage.

## 18. Risks and Mitigations

- Adoption Risk: Provide fast time-to-value templates and integrations; strong onboarding.
- Data Quality: Enforce structured inputs and validations; reconcile via events.
- Marketplace Liquidity: Seed supply in key categories/regions; incentives and credit promos.
- Technical Complexity (ES/CQRS): Gradual rollout; apply ES where high audit value; use outbox for simpler domains.
- Security/Privacy: Least-privilege, regular audits, incident response runbooks.

## 19. Diagrams and Artifacts (To Be Added)

- UML: Use Case, Component, Sequence, Class.
- ERDs: Per context (Identity, Supply Chain, Financials, etc.).
- Event Catalog: Topic schemas and event versioning policy.
- API Examples: REST/async patterns with auth scopes.
- Glossary: Ubiquitous language across contexts.

## 20. Open Questions

- Tenant isolation model final choice (schema-per-tenant vs RLS) per scale and ops constraints.
- Degree of Event Sourcing vs CRUD per context.
- Commission/rate card governance and regional variations.
- ERP integrations priority (SAP, Oracle, others).
