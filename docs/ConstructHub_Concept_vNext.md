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
- Delivery model: Cloud-neutral native cloud (no AWS/Azure dependency) multi-tenant SaaS by default; optional on-prem/private cloud Kubernetes offering targeted for Phase 5 enterprise deals (feature parity constraints and higher TCO). Dev environment runs fully locally.
- High-value exemplar use cases: (a) Urgent material request-to-delivery in <48h; (b) Steel tender attracting 5+ qualified bidders in <7 days; (c) Progress claim verified with photo evidence within 72h and auto-invoiced.
- Success measures (executive): procurement cycle time reduction, on-time delivery confirmations, GMV and take rate (marketplace), on-platform retention, audit findings (zero material issues).

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
- Typical pre-digital baselines (for ROI framing): quote cycles 5–10 days; delivery confirmation >72h after arrival; inventory accuracy 85–92%; paper document handling 10–20 minutes per file.
- Change management: phased pilots with on-site champions, role-based training, and KPI dashboards; integration adapters to minimize double entry during transition.
- Industry context: aligns with ISO 19650 (information management), common FIDIC contract workflows, GDPR for personal data, and VAT/GST compliance for invoicing.

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
- No PII in logs; secrets never logged; periodic access reviews and attestations.
- Error budget policy: if error budget burn exceeds 50% mid-cycle, feature releases pause in favor of reliability work.
- Support SLAs (pilot): P1 <1h response, P2 <4h, P3 <1 business day; incident postmortems within 5 business days with corrective actions.

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
- Role presets (indicative): Org Admin, Project Admin, PM, Site Engineer, Finance Approver, Vendor Rep, Subcontractor Rep; custom roles per tenant.
- Scope model: Global (org) → Project → SubProject; permissions are the intersection of role and scope. Field roles default to mobile-first surfaces and offline capabilities.
- Localization and accessibility: WCAG 2.1 AA targets; right-to-left support (Phase 4); unit/currency/timezone localization per project.

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

Proof of value and attribution

- Feature-to-value examples: delivery confirmations with evidence → lower dispute rate; bidding with credit economy → more bidders and better pricing; progress-to-invoice automation → faster cash cycles.
- Evidence artifacts: before/after KPI dashboards, timeline snapshots of key procurements, anonymized tender outcomes, and audit-ready event trails.

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

Cross-cutting and shared services

- API Gateway/BFF, Identity (Keycloak), Observability (traces/logs/metrics), Feature Flags, Schema Registry, Event Catalog.
- Data governance service for code lists/reference data; cost and usage telemetry; export and integration hub.

Out of scope (vNext)

- Full-fledged scheduling (Primavera/MS Project parity), BIM authoring, heavy equipment telematics; these integrate via APIs.
- Native payments/escrow beyond invoice generation in early phases (evaluated in Phase 3+).

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
- Edge cases and exceptions: superseded drawings auto-archived with cross-links; emergency releases allowed with post-facto review; redaction workflows for sensitive PII.
- Automation hooks: OCR/AI extraction populates metadata; automatic reviewer assignment by discipline; reminders/escalations on SLA breach.

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

Context mapping and boundaries

- Relationships: conformist (Reporting consumes domains), anti-corruption layers to ERPs, customer/supplier separate bounded contexts.
- Examples: Procurement (core) integrates with ERP via ACL for invoices; Marketplace remains separate from Procurement write model and links via events.
- Deprecation: versioned commands/events with sunset policies; compatibility shims during migrations.

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
  - Event Retention: Long-lived or tiered storage (e.g., Kafka tiered storage/object storage sinks) to enable history replay and audits.

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
  - Environments: Dev → Staging → Pre-prod → Prod; ephemeral preview environments per PR where feasible.
  - CI/CD: Build, unit/integration tests, security scan (SAST/SCA), image signing/SBOM; CD via GitOps (Argo/Flux) with canary/blue-green and automated rollbacks.
  - Testing: Consumer-driven contract tests, testcontainers for integration, k6/Gatling for load, chaos experiments (latency/partition), game days.
  - Secrets and Config: Externalized via Vault/KMS; sealed secrets for K8s; runtime reload with config maps.
  - Versioning: Semantic versioning for services and event schemas; backward compatibility policy; migration guides for breaking changes.
  - Topology: Multi-AZ baseline; multi-region DR evaluated Phase 5; edge caching for static assets and large document downloads.

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

- Ownership: Each service owns its schema and event types; clear data stewardship and on-call rotation.
- SLOs: Per-service SLOs (availability, latency) published; error budgets tracked; golden paths defined for core workflows.
- Rate limits: Global and per-tenant throttles; adaptive backpressure to protect shared resources.
- Resilience: Circuit breakers between gateway and services; retries with jitter; bulkheads for critical components.
- Failure isolation: Crash-only design; DLQs per consumer; replay procedures and dashboards; graceful degradation for non-critical projections.
- Scaling: Stateless services autoscale; stateful components use partitioning/sharding and read replicas; hot shard detection and rebalancing.

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

Disputes and governance

- Dispute workflow: DisputeRaised → EvidenceAttached (photos, ASN, signatures) → Mediation → Resolution (credit note/partial payment) with auditable trail.
- Privacy modes: Sealed-bid (default) with reveal on closure; optional two-stage (prequal → price) for services; anonymized bidder identities until award.
- Supplier verification levels: Basic (KYC/KYB), Verified (references + performance history), Premium (audited capacity), impacting ranking and bid visibility.
- Fee transparency: Rate card displayed per category; example—steel (tonnage-based low fee), finishes (higher %); commission shown at award and on confirmed delivery.

## 12. Security and Compliance

What

- A security-by-design and compliance-first approach covering identity, authorization, data protection, operations, and regulatory obligations across multi-tenant workloads.

Why

- Protects sensitive project, financial, and personal data; meets customer and regulatory requirements; reduces legal, operational, and reputational risk while enabling enterprise adoption.

How

- Identity and Authentication
  - OAuth2/OIDC via Keycloak; SSO (SAML/OIDC) with enterprise IdPs; PKCE for public clients.
  - MFA/2FA and step-up authentication for sensitive actions (e.g., PO approval, payment release, rate card changes).
  - Short-lived access tokens, rotating refresh tokens; device binding optional for mobile apps.
- Authorization
  - RBAC as default with optional ABAC constraints (tenant, project, subproject, role, geography).
  - Deny-by-default policies; policy enforcement at gateway and service layers; least-privilege service accounts.
- Tenant Isolation
  - App-layer checks + DB isolation (schema-per-tenant or RLS) + S3 partitioning (bucket/prefix) + Kafka ACLs by service.
  - Optional per-tenant encryption contexts for documents; cross-tenant leakage tests embedded in CI.
- Data Protection
  - TLS 1.2+ in transit; encryption at rest for DB/Kafka/object storage; key management via Vault or HSM/KMS (cloud-neutral); periodic rotation.
  - Envelope encryption for documents; content hashing and integrity checks for evidence files.
  - PII classification and field tagging; log redaction/masking; DLP checks for exports.
- Event and Log Security
  - Tamper-evident, append-only audit logs with actor, tenant, correlation IDs, IP/agent.
  - Restricted log access, time-bound retention, and secure archives; privacy-by-default logging.
- Application Security (SSDLC)
  - SAST/DAST/SCA, dependency pinning, SBOM generation; container/image signing (Sigstore/Cosign) and provenance.
  - IaC scanning, secrets scanning, pre-commit hooks; threat modeling per epic; periodic pen tests.
  - Vulnerability management with severity-based SLAs; emergency patch process.
- Network and Runtime
  - mTLS between services; network policies; WAF/IDS/IPS at ingress; rate limiting and bot detection.
  - Isolation of build vs. runtime; hardened base images; least capabilities.
- Compliance Program
  - SOC 2 (Security, Availability, Confidentiality): change management, access reviews, backups/DR, incident response, vendor risk.
  - GDPR/UK GDPR: lawful basis, consent where applicable, DSAR workflows, data minimization, retention/deletion; DPIAs for high-risk features.
  - Data residency options by region; subprocessors transparency; contractual clauses (SCCs/DTAs) as required.
- Incident Response
  - On-call rotation, detection rules (DLQ spikes, anomaly auth), runbooks, forensics, and postmortems with customer comms SLAs.
- Customer Controls
  - Admin audit trails, configurable retention windows, IP allowlists, SSO enforcement, export controls, and API scopes.
  - Evidence for audits: event logs, access reviews, change tickets, DR test reports, vulnerability scan results.
  - Vendor Risk Management (VRM): documented subprocessors, security questionnaires support, SIG/CAIQ responses on request.

## 13. Data Model and Storage Notes

What

- Principles, conventions, and artifacts for modeling write models, events, and projections safely in a multi-tenant, event-driven system.

Why

- Preserves data integrity and auditability, supports performance and evolution, and prevents coupling and cross-tenant leakage.

How

- Modeling and Naming
  - ERDs per bounded context with normalized write models per aggregate; avoid cross-context foreign keys.
  - Keys: ULIDs/UUIDv7 for time-sortable IDs; human-readable business codes (e.g., PO-YYYY-XXXX) for documents.
  - Conventions: snake_case for storage, camelCase for APIs; consistent units and currency fields.
- Event Catalog
  - Envelope: id, type, version, occurredAt, tenantId, aggregateType, aggregateId, aggregateVersion, correlationId, causationId, actor.
  - Schema governance via registry (Avro/JSON Schema); backward-compatible evolution; additive changes; deprecation policy.
  - Topics: <context>.<aggregate>.<event>; partition by aggregateId for order; compaction where appropriate; retention/tiering to object storage.
- Projections (Read Models)
  - Idempotent consumers with deterministic upserts; track checkpoints per consumer group.
  - Replay support for rebuilds and schema changes; blue/green projection deployment; backfill scripts for historical data.
  - Per-tenant/project scoping; cache invalidation via events; SLA for projection lag P95 <30s.
- Storage Design
  - PostgreSQL for OLTP; RLS or schema-per-tenant; indexes by common filters (tenantId, projectId, status); table partitioning for hot entities.
  - Object Storage (S3-compatible or native): path layout tenant/project/module/objectId; integrity via content hashes; lifecycle policies (archive/delete) per retention rules.
  - Search (Elasticsearch optional): index pipelines from projections; PII redaction; index templates and versioning.
- Data Quality and Governance
  - Constraints and invariants at both command validation and storage (check constraints, unique indexes).
  - Reference data domains with versioned code lists; deduplication jobs; anomaly detection on events (e.g., negative stock).
  - Migrations managed via Flyway/Liquibase; online, backward-compatible patterns (expand/contract); dual writes and shadow reads during cutovers.
- Lifecycle, Retention, and Residency
  - Retention policies per artifact type; RetentionApplied and DataDeleted events; DSAR export/delete flows.
  - Encrypted backups with PITR; periodic restore drills; regional sharding/residency to meet locality requirements.

Data catalog and analytics

- Catalog: Data dictionary per projection and write model; lineage tracked via OpenLineage/Marquez (Phase 4+).
- Warehouse/BI: Optional ETL from projections to a tenant-scoped warehouse or lakehouse; dbt models for standard KPIs; export contracts documented.
- PII inventory: Fields classified by sensitivity; minimization rules; tokenization where feasible for analytics.

## 14. Non-Functional Requirements (Targets)

What

- Service-level objectives and engineering constraints for performance, availability, reliability, security, usability, and cost. These define acceptance criteria for releases and guide capacity and incident management.

Why

- Ensures consistent user experience across office and field; meets enterprise procurement and compliance requirements; prevents uncontrolled cost growth; enables contractual SLAs.

How

- SLIs collected via APM, traces, logs, and synthetic checks; SLOs reviewed quarterly. Error budgets drive release cadence and hardening cycles. Auto-scaling and rate limits protect SLOs. Chaos and load testing validate resilience.

Targets (initial)

- Availability
  - Service availability ≥99.9% Phase 2; target 99.95% Phase 3+ (per critical service and gateway).
  - Mobile offline tolerance: core receipt and claim flows function offline with auto-sync; sync lag P95 <60s after connectivity returns.
- Performance
  - Reads P95 <300 ms; Writes P95 <500 ms for typical commands; cold start P95 <1.5s for web, <2.5s for mobile app relaunch.
  - Projection lag P95 <30s; search queries P95 <800 ms when Elastic enabled.
- Scalability
  - Horizontal scale of stateless services; load-tested to 5x Phase 2 baseline. Kafka partitions provisioned to keep consumer lag <30s P95.
  - Multi-tenant quotas and backpressure to isolate noisy neighbors.
- Reliability and Consistency
  - Exactly-once semantics for critical projections (financials, inventory) or compensating logic with at-least-once + idempotency.
  - Data loss budget: 0 for committed writes; RTO ≤ 2 hours; RPO ≤ 5 minutes via event/log-based restore.
- Security and Privacy
  - Security incident SLO: P1 triage <15 min, containment <4h, customer notice per policy. Secrets rotation ≥90 days.
  - Privacy: DSAR fulfillment <30 days; data deletion SLA <7 days after approval.
- Usability and Accessibility
  - WCAG 2.1 AA for web UI; mobile gesture and large touch targets for field users; localization for currency/units/timezones.
- Interoperability
  - API availability ≥99.9%; backward-compatible changes with ≥2 minor versions supported; deprecation notices ≥90 days.
- Cost and Sustainability
  - Cloud cost budget per tenant/project; scale tests include cost modeling; storage lifecycle policies to manage S3 footprint.

Measurement and Enforcement

- Golden signals and dashboards per service; synthetic journeys for procurement/delivery confirmation; monthly error budget reviews.
- Load tests before major releases; disaster recovery drills semi-annually; chaos experiments quarterly.
- Mobile/offline specifics: local queue for commands with conflict resolution on sync; user prompts on stale data; bandwidth-aware media uploads.
- Internationalization: locale/units/currency per project and user; timezones normalized to UTC in storage with tz-aware display.

## 15. Technology Stack

What

- A pragmatic, enterprise-grade stack optimized for event-driven microservices, strong auth, and multi-tenant SaaS operations.

Why

- Maximizes developer productivity and ecosystem leverage; balances performance, reliability, and cost; aligns with industry-standard tooling to ease hiring and integrations.

How (choices and rationale)

- Frontend: React + TypeScript
  - Rationale: Strong ecosystem, type safety, component reuse; supports responsive web for office and PWA patterns for field.
  - Notes: State management with Redux/RTK Query or React Query; design system and accessibility built-in.
- Backend: Java + Spring Boot
  - Rationale: Mature DI, observability, security, and data libraries; wide enterprise adoption.
  - Notes: Spring Cloud for config/feature-flags; WebFlux for async IO where needed.
- Messaging: Apache Kafka + Schema Registry (Avro/JSON Schema)
  - Rationale: High-throughput, ordered streams per aggregate; replayable history; decoupled services.
  - Notes: Tiered storage or sinks to object storage for long-term retention; RBAC/ACLs on topics.
- Database: PostgreSQL
  - Rationale: Reliable OLTP, strong indexing and JSONB support, RLS for multi-tenancy.
  - Notes: Partitioning for hot tables; read replicas for reporting; prefer cloud-neutral Postgres operators (CloudNativePG/Crunchy) or VM-based clusters; no dependency on AWS/Azure PaaS.
- Search/Cache: Elasticsearch (optional) + Redis
  - Rationale: Full-text and aggregations when needed; low-latency caching and coordination primitives.
  - Notes: Build indices from projections; cache invalidation via events.
- Storage: S3-compatible Object Storage
  - Rationale: Durable, cost-effective document storage and evidence handling.
  - Notes: Lifecycle and retention policies; encryption; presigned URLs for uploads; no dependency on AWS/Azure—supports MinIO, Ceph RGW, and other native object stores.
- AuthN/Z: Keycloak (OAuth2/OIDC, JWT)
  - Rationale: Standards-based identity, SSO, policy enforcement, and admin console.
  - Notes: Multi-realm or tenant-aware config; MFA; group/role mappings; SCIM roadmap.
- Deployment: Docker + Kubernetes with GitOps
  - Rationale: Predictable deployments, autoscaling, isolation; declarative ops.
  - Notes: Argo/Flux for GitOps; Helm/Kustomize; blue-green/canary; HPA/VPA.
- Observability: OpenTelemetry + Prometheus/Grafana + Centralized Logging
  - Rationale: End-to-end tracing, metrics, alerting; rapid incident triage.
  - Notes: Trace context propagation in events; logs with correlation IDs; SLO dashboards.

Extensibility and Integrations

- API styles: REST for commands/queries; async events; webhooks for external consumers.
- Integrations: ERP (SAP/Oracle), BIM, logistics; adapters via anti-corruption layer.
- SDKs and Client Libraries: TypeScript and Java first; others based on demand.

Versioning and Lifecycle

- Semantic versioning for services/APIs/events; compatibility windows; deprecations with ≥90-day notice.
- Security patches tracked via SBOM; image signing and provenance; automated dependency updates with review gates.

Tooling and testing

- Build: Gradle/Maven for backend; pnpm/yarn for frontend.
- Testing: JUnit/Testcontainers, WireMock/PACT for contracts; Cypress/Playwright for E2E; k6 for load; OWASP ZAP for DAST.
- Lint/format: ESLint/Prettier; Checkstyle/SpotBugs; conventional commits and commitlint.

Local development and parity

- Orchestration: Docker Compose for core stack (PostgreSQL, Kafka/ZooKeeper/Schema Registry, Keycloak, MinIO); make targets for up/down/seed.
- Local Kubernetes: kind/k3d/minikube with Helm charts/Kustomize overlays; dev profiles and sample tenants/projects.
- Secrets/config: Dev secrets stored in .env/.secrets (local-only); Vault dev server optional; seeded test data and fixtures.
- Developer UX: one-command bootstrap script; hot reload for services; local mail catcher and mock ERP/logistics webhooks.
- Parity: Same images as prod with dev config; feature flags to toggle integrations; migration/rollback tested locally first.

## 16. Roadmap (Phased)

What

- A staged delivery plan that sequences capabilities to maximize early customer value, de-risk technical decisions, and build marketplace liquidity.

Why

- Phasing reduces complexity, accelerates learning, and ensures each release strengthens core workflows (procure→deliver→pay) before adding advanced features.

How

- Release trains every 6–8 weeks; alpha/beta programs with pilot tenants; exit criteria defined per phase with measurable outcomes from Section 3.

Phases (goals, key deliverables, exit criteria)

- Phase 1 (MVP)
  - Goals: Establish tenant, project, and identity foundations; deliver first end-to-end read/write path; prove projections and observability.
  - Deliverables: Identity & Access, Organizations, Projects, basic DMS, initial projections, core observability, API gateway, audit baseline.
  - Exit Criteria: 3 pilot tenants live; P95 reads <400 ms; projection lag P95 <60s; first procurement request lifecycle demo end-to-end.
- Phase 2
  - Goals: Enable real procurement and inventory flows; seed early marketplace participation.
  - Deliverables: Vendors, Materials, Procurement, Inventory; mobile delivery receipt; marketplace foundations (credits, tenders).
  - Exit Criteria: -30% procurement cycle time in pilots; 50+ tenders created; 70% mobile receipt adoption on pilot sites.
- Phase 3
  - Goals: Close procure-deliver-pay loop; enforce governance; monetize marketplace transactions.
  - Deliverables: Deliveries, Financials (budgets/commitments), Workflow & Approvals, commission logic, subcontractor progress.
  - Exit Criteria: On-time delivery confirmations +25%; first commissions accrued; progress-to-invoice flow live; DAU/MAU >0.35.
- Phase 4
  - Goals: Insight and automation; reduce admin effort; unlock data exports.
  - Deliverables: Reporting & Analytics, AI Document Analysis (OCR), AI Material Estimator v1; export APIs and warehouse sink (optional).
  - Exit Criteria: Document handling time -40%; estimator adoption on ≥30% new projects; 5 standard dashboards used weekly.
- Phase 5
  - Goals: Scale marketplace and intelligence; deepen ecosystem integrations; mobile GA.
  - Deliverables: Advanced Marketplace (Golden Bids, reputation), AI Planning, mobile apps GA, ERP integrations (SAP/Oracle connectors).
  - Exit Criteria: On-platform retention ≥95%; ≥200 paying tenants; GMV and take-rate targets achieved; SOC 2 Type I readiness.

Enablers

- Platform: CI/CD, IaC, cost reporting, feature flags, canary releases; infra as code (Terraform/Pulumi), cluster baselines, cost dashboards.
- Data: Schema registry, event catalog, governance council, data contracts; seeded sample datasets and replay tooling for demos.
- Security: SSO, MFA, audit scopes, incident response playbooks; pen test cadence; secure coding training.

Pilot selection and feedback loops

- Criteria: Active projects, committed sponsor, at least 2 suppliers onboarded, baseline metrics collected.
- Feedback: Weekly steering with pilots; in-product NPS/CSAT; structured discovery and roadmap reviews per phase.
- Risks per phase: MVP scope creep (control via strict exit criteria); Phase 2 liquidity (supplier incentives); Phase 3 finance integration risk (sandbox first).

## 17. Business Model and Pricing

What

- A hybrid SaaS + marketplace monetization model aligned to delivered outcomes: recurring subscriptions for software value and variable fees for transacted value, with caps to encourage loyalty and reduce leakage.

Why

- Aligns incentives across constructors and suppliers; scales with usage and GMV; supports entry-level adoption while monetizing power users and high-volume categories.

How

- Customers and Segments
  - Primary: Constructors/GCs (buyers/operators). Secondary: Suppliers and service subcontractors (sellers).
  - Procurement-led land, marketplace-led expand; partner channel for integrations.

- Revenue Streams
  1. Transaction Commissions (Marketplace)
     - Rates: Tiered, category-specific (e.g., steel tonnage-based low fee; finishes higher %). Regional variations supported.
     - Accrual: Only after DeliveryConfirmed (materials) or MilestoneVerified (services). Pro-rated for partial acceptances.
     - Project-Based Cap: Per project and category, once CapAmount reached for a supplier, subsequent sales in that project/category are commission-free. Events: CommissionAccrued, CommissionCapReached.
     - Controls: On-platform invoicing; escrow/milestone options; anti-collusion monitoring.
  2. Bidding Credits
     - Model: Monthly free credits; paid packages (volume discounts). BidSubmitted debits credits. Refunds if constructor cancels after bids; no refunds for losing bids.
     - Premium: Golden Bids for placement; limited per tender to preserve fairness.
  3. Premium Listings/Promotions
     - Featured suppliers/categories; seasonal campaigns; sponsored tenders in discovery feeds.
  4. SaaS Subscriptions (Constructors)
     - Tiers: Starter, Professional, Enterprise.
       - Starter: Limited projects/users, base workflows, standard support.
       - Professional: Multi-project/portfolio, advanced approvals, reporting, marketplace analytics.
       - Enterprise: SSO/SCIM, data residency options, custom retention, premium support, ERP integrations, AI quotas.
     - Metered Add-ons: OCR pages, AI estimator runs, API call bundles.

- Pricing and Billing
  - Billing cadence monthly/annual; usage-based meters for credits/OCR/AI; overage alerts; invoices with tax compliance per region.
  - Payment methods: Cards, bank transfer; marketplace payouts to suppliers via supported rails; escrow fees if used.

- KPIs and Unit Economics
  - SaaS: ARPA/ARPU, churn, net revenue retention, LTV/CAC.
  - Marketplace: GMV, take rate, active tenders, bidders/tender, win rate, on-platform retention post-award, dispute rate.
  - Targets: Commission share of revenue ≥30% by Phase 4; SaaS gross margin ≥80%; blended gross margin ≥60%.

- Governance and Policies
  - Transparent rate cards; change notices; category councils for rate updates; regional compliance checks (tax, data).
  - Anti-Disintermediation: Commission caps, logistics linkage, and in-app invoicing keep transactions on-platform without punitive fees.

Commercial terms (indicative)

- Examples: Starter $X/user/mo (min Y users), Professional $A/project/mo, Enterprise custom with annual commit; marketplace commission ranges 0.2–5% depending on category/region.
- Discounts: Annual prepay, volume credits for suppliers, founder/pilot discounts Phase 1–2 with sunset dates.
- SLAs and support: Tiered support SLAs by plan; uptime credits for breaches; onboarding packages and training add-ons.
- Enterprise: MSAs, DPAs, security addenda, custom data residency and retention (surcharges may apply).

## 18. Risks and Mitigations

What

- A living register of product, market, and technical risks with owners, early warning indicators, and mitigation plans. Reviewed at each phase gate and quarterly thereafter.

Why

- Proactively reduces the likelihood and impact of adverse events, protects timelines and budgets, and supports enterprise risk assessments (e.g., vendor due diligence, SOC 2).

How

- Process: Identify → Assess (likelihood/impact) → Mitigate → Monitor → Escalate. Map risks to Section 3 objectives and Section 16 phase exits. Track KRIs and playbooks.

Key risks (examples with KRIs and mitigations)

- Adoption and Change Management
  - Risk: Low adoption due to workflow change and integration friction.
  - KRIs: Low DAU/MAU (<0.2), stalled tenders, low mobile receipt usage (<50%).
  - Mitigations: Role-based onboarding, templates, in-app guidance, pilot champions, integration connectors, success plans.
  - Owner: Product + Customer Success.
- Data Quality and Integrity
  - Risk: Inaccurate catalogs/quantities causing stockouts/rework.
  - KRIs: High discrepancy rate (>5%), frequent manual adjustments, negative stock events.
  - Mitigations: Validation rules, required fields, guided forms, anomaly detection, periodic cycle counts, reconciliation tasks.
  - Owner: Supply Chain PM + Eng.
- Marketplace Liquidity
  - Risk: Too few bidders or tenders reduce value and price discovery.
  - KRIs: Bidders/tender <3, tender fill rate <70%.
  - Mitigations: Supply seeding in key categories/regions, credit promos, featured listings, outreach; constructor incentives for open tenders.
  - Owner: Marketplace Lead.
- Technical Complexity (ES/CQRS)
  - Risk: Overuse of ES increases delivery time and operational toil.
  - KRIs: Projection lag >60s P95, frequent consumer DLQs, high incident count.
  - Mitigations: Apply ES selectively; outbox for CRUD domains; reference architectures; platform tooling for projections; chaos and load tests.
  - Owner: Architecture Guild.
- Security and Privacy
  - Risk: Breach or PII leakage; non-compliance with GDPR/SOC 2.
  - KRIs: Unusual auth patterns, failed MFA spikes, data exfil alerts, overdue patches.
  - Mitigations: SSDLC, least privilege, MFA, encryption, monitoring, incident runbooks, regular audits, pen tests.
  - Owner: Security Lead.
- Availability and Performance
  - Risk: SLA/SLO breaches under load or due to hot partitions.
  - KRIs: SLO error budget burn, Kafka lag >30s P95, API P95 >500 ms.
  - Mitigations: Autoscaling, partition/shard planning, backpressure, caching, perf tests, feature flags for graceful degradation.
  - Owner: SRE Lead.
- Regulatory and Tax Complexity
  - Risk: Incorrect tax handling or regional data rules.
  - KRIs: Invoice rejections, audit findings, customer escalations.
  - Mitigations: Tax engine integration, jurisdiction configs, legal review, data residency options.
  - Owner: Finance/Compliance.
- Vendor Lock-in and Cost Overruns
  - Risk: Cloud/messaging costs spike; difficult to migrate.
  - KRIs: COGS/tenant rising >20% QoQ, storage growth without retention controls.
  - Mitigations: Cost dashboards, lifecycle policies, tiered storage, reserved capacity, abstraction layers where prudent.
  - Owner: Eng Ops.

Escalation and Communication

- Severity matrix for incidents; stakeholder comms SLAs; customer advisory board for roadmap risks; postmortems with action tracking.

Residual risk and reporting

- Heatmap per risk with residual ratings after mitigations; quarterly risk reviews with leadership.
- RACI for major risks (e.g., security, liquidity, data quality); backup owners assigned.
- External reporting: SOC 2 readiness status, uptime reports, and privacy transparency updates shared with enterprise customers.

## 19. Diagrams and Artifacts (To Be Added)

What

- A curated, versioned set of engineering and product artifacts that codify the system’s design, behavior, and language. Stored in-repo, linked from the document, and updated via PRs.

Why

- Improves shared understanding, onboarding, and auditability; reduces miscommunication across teams; supports compliance and customer diligence.

How

- Tooling: PlantUML/Mermaid for diagrams in code; dbdocs/DBML for ERDs; AsyncAPI/OpenAPI for APIs; ADR templates for decisions.
- Governance: Docs-as-code with CI linting; owners per context; review checklist includes diagram updates; version tags per phase.

Artifacts

- UML/Mermaid Diagrams
  - Use Case: Key actor-goal interactions (procure→deliver→pay, bidding, progress→payment, document review).
  - Component: Services, gateways, data stores, and external systems; trust boundaries and network flows.
  - Sequence: Critical workflows (procurement approval, delivery confirmation with discrepancy, award→PO→commission accrual, progress claim→invoice).
  - Class/Domain: Aggregates and relationships within contexts (read-only depiction to avoid cross-context coupling).
- ERDs
  - Per bounded context (Identity, Project, Supply Chain, Financials, Collaboration, Execution, Marketplace). Include keys, constraints, and RLS/schema notes.
- Event Catalog
  - Canonical event list with envelopes, schemas, sample payloads, and version history; topic naming and partitioning guidance; retention/compaction notes.
- API Specifications
  - OpenAPI for REST commands/queries; AsyncAPI for events; auth scopes and examples; error models and pagination.
- Architecture Decision Records (ADRs)
  - Decisions on ES scope, tenant isolation model, commission cap algorithm, search usage, and warehouse strategy; status (proposed/accepted/deprecated).
- Security and Privacy Artifacts
  - Threat models (STRIDE/LINDDUN), data flow diagrams, DPIAs, pen test reports (summaries), and incident response runbooks.
- Operations Playbooks
  - On-call runbooks, DLQ handling, replay procedures, backup/restore steps, capacity and cost management guides.
- Glossary
  - Ubiquitous language per context; cross-references; aliases and deprecated terms.

Maintenance

- Quarterly doc days; CI checks for stale diagrams vs. code; template PRs for context changes; changelog entries when artifacts evolve.
- Storage/location: docs/architecture for diagrams, docs/api for OpenAPI/AsyncAPI, docs/adr for decisions, docs/glossary for language.
- Review cadence: Architecture guild reviews monthly; context owners update artifacts with each significant change; DRI list maintained in repo.
- Coverage targets: 100% of services with component diagrams, 100% of public APIs with OpenAPI, 100% of events with schema and samples by Phase 3.

## 20. Open Questions

- Tenant isolation model final choice (schema-per-tenant vs RLS) per scale and ops constraints.
- Degree of Event Sourcing vs CRUD per context.
- Commission/rate card governance and regional variations.
- ERP integrations priority (SAP, Oracle, others).

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
