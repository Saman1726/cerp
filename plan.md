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
