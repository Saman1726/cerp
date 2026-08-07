# Project Service

Spring Boot 3 service owning the structural backbone of CERP: the Project → SubProject hierarchy and the Construction Site → Building → Floor → Unit hierarchy that everything else (workflows, procurement, billing, vendor assignment) is anchored to.

## Purpose

Every construction job in CERP is ultimately anchored to a `Project` and, beneath it, one `SubProject` per building under construction. This service is the single owner of that structure: it decides what a Project is, who permanently owns it, how it decomposes into SubProjects/buildings, and which structural invariants are non-negotiable (one active Project per Construction Site, one SubProject per Building).

It is deliberately a separate service from `workflow-service-java`, even though both sit in the same "construction execution" part of the platform:

- **project-service-java owns structure** — the Project/SubProject/Construction-Site/Building/Floor/Unit hierarchy itself: what nodes exist, who owns them, and the physical/organizational shape of the job.
- **workflow-service-java owns process** — the guarded workflow templates, steps, and bidding/procurement sagas that execute *within* a SubProject once it exists. It reacts to structural events published by this service (`project.subproject.created` triggers workflow auto-generation — see [Kafka Topics](#kafka-topics)) but does not itself decide what a SubProject is.

Keeping these separate means "what exists" (rarely changes shape; mostly CRUD plus a small set of hard invariants) stays independent from "what happens to it" (frequently-changing process/orchestration logic). See `workflow-service-java`'s own README for the process side of that split.

## Responsibilities

- CRUD for Projects, SubProjects (buildings tracked as sub-projects of a Project), Construction Sites, Buildings, Floors, and Units
- Creator-based ownership: whoever calls `POST /api/v1/projects` or `POST /api/v1/construction-sites` becomes that resource's permanent owner (`primaryOwnerId` / `createdBy`) — immutable afterward, and the anchor for that resource's subscription-plan quotas
- Role-based user assignment on Projects (`OWNER`/`MANAGER`/`SUPERVISOR`/`VIEWER`) with per-role membership caps sourced from the owner's identity-service subscription plan
- Guarded SubProject status lifecycle (`PLANNING` → `IN_CONSTRUCTION` → `ON_HOLD`/`COMPLETED`)
- DB-enforced 1:1 structural rules: one active Project per Construction Site, one SubProject per Building
- Project code uniqueness per tenant
- Tenant-scoped data isolation (via `X-Tenant-Id` header)
- Kafka event publishing for downstream consumers (see [Kafka Topics](#kafka-topics) — there are naming inconsistencies here worth knowing about before relying on them)
- OAuth2/JWT authentication (Keycloak), PostgreSQL persistence with Flyway migrations

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│  API Gateway  (X-Tenant-Id propagation, JWT passthrough)      │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│  Project Service                                              │
│                                                               │
│  Controllers --> Services --> JPA Repositories --> Postgres   │
│   (Project/        (ProjectService,      (schema: `project`)  │
│    SubProject/       SubProjectService,                       │
│    ConstructionSite) ConstructionSiteService)                 │
│                                                               │
│  Also calls out to:                                           │
│   - identity-service  (subscription / quota checks)           │
│   - Kafka             (lifecycle events + subproject-created) │
└───────────────────────────────────────────────────────────────┘
```

### The two hierarchies

This service models **two** parallel structural trees that meet at one point:

- **Project → SubProject** (`Project.java`, `SubProject.java`): a `Project` is the top-level unit of work; each `SubProject` represents one building (or a scoped part of one) being worked under that Project, with its own guarded status lifecycle and a `ConstructionPhase` (15 phases, `PRE_CONSTRUCTION` through `FACILITY_MANAGEMENT`) that downstream services use to pick applicable workflow templates.
- **Construction Site → Building → Floor → Unit** (`ConstructionSite.java`, `Building.java`, `Floor.java`, `Unit.java`): the physical/organizational shape of a site. This tree is intentionally plain CRUD with cascade deletes and no status machine of its own.

A `SubProject` optionally points at one `Building` (`building_id`) — that FK is the bridge between the two trees: "this unit of project work corresponds to this physical building."

### Creator-based ownership

Whoever calls `POST /api/v1/projects` becomes that Project's permanent `primaryOwnerId`; whoever calls `POST /api/v1/construction-sites` becomes that site's permanent `createdBy` owner. This is set once, from the caller's identity-service user id — read out of the JWT's `internal_user_id` claim by `RequestIdentity.creatorId()`, deliberately not the JWT's `sub`, which is Keycloak's own internal id, not identity-service's — and is enforced as immutable afterward:

- `ProjectService.assignUsersWithRoles()` rejects changing the primary owner's role away from `OWNER`.
- `ProjectService.removeUser()` rejects removing the primary owner from their own project.

This is independent of the account type (User/Vendor/Company) identity-service assigns — project-service only ever sees a user id.

Ownership is also the billing/quota anchor: `enforceMemberCap()`, `enforceConstructionSiteQuota()`, and `enforceBuildingQuota()` all call identity-service's `GET /api/v1/users/{userId}/subscription` for the **resource's owner**, not the acting caller, and fail closed (`QuotaCheckUnavailableException`) if that lookup fails. A Project's or Construction Site's plan limits travel with whoever created it — consistent with a shared-tenant model where many users can belong to one tenant.

### Role vocabulary

`Project.assignedUserRoles` accepts exactly `OWNER`, `MANAGER`, `SUPERVISOR`, or `VIEWER`, enforced by a `@Pattern` regex on `ProjectUserAssignmentRequest.UserAssignment.role()` and by the role-bucket counting in `enforceMemberCap()`. There is **no `ADMIN` role at this level** — `ADMIN` only exists as an app/tenant-level role in identity-service. (A couple of Javadoc comments elsewhere in this codebase — `Project.assignedUserRoles`'s field comment and `ProjectController#assignUsers`'s method comment — still describe an older `VIEWER`/`EDITOR`/`ADMIN` vocabulary; those comments are stale and don't reflect what's actually validated.)

### Guarded status transitions

`SubProject` only allows moving between statuses via explicit methods — `startConstruction()`, `hold()`, `resume()`, `complete()` — each calling a private `requireStatus(...)` guard that throws `IllegalStateException` if the current status doesn't permit the move (e.g. you cannot `complete()` a SubProject still in `PLANNING`). One gap worth knowing: `PUT /api/v1/projects/{projectId}/subprojects/{subProjectId}` (`SubProjectService.updateSubProject()`) sets `status` directly from the request body through a plain setter, bypassing these guard methods entirely — the guard only protects call sites that go through the named transition methods, not a raw update.

### DB-enforced structural invariants

Two rules are structural, not just application logic, and are backed by Postgres unique indexes (`V2__enforce_one_to_one_site_project_and_building_subproject.sql`), not application checks alone:

- `idx_projects_construction_site_id_active` — a **partial** unique index on `projects.construction_site_id` (`WHERE construction_site_id IS NOT NULL AND is_deleted = false`): a Construction Site can have at most one active Project.
- `idx_subproject_building_id_unique` — a unique index on `sub_projects.building_id` (`WHERE building_id IS NOT NULL`): a Building can have at most one SubProject.

Both services do an application-level pre-check first (for a clean error message/status), then `saveAndFlush()` inside a `try`/`catch (DataIntegrityViolationException)` to translate the rare race-window violation (two concurrent requests both passing the pre-check before either commits) into the same domain exception the pre-check throws. **Caveat:** there is currently no `src/test` directory anywhere in this service, so this race-window handling is not covered by an automated test — treat it as reviewed-by-reading, not verified-by-test.

### `ddl-auto: update` alongside Flyway

`application.yml` sets `spring.jpa.hibernate.ddl-auto: update` (also pinned explicitly via `SPRING_JPA_HIBERNATE_DDL_AUTO: update` in `docker-compose.yml`) **in addition to** Flyway migrations. This is unusual and worth flagging: `vendor-service-java`/`catalog-service-java` use `ddl-auto: none` and treat Flyway as the sole schema authority, which is the safer pattern once Flyway is managing a schema — Hibernate can otherwise auto-alter columns/tables at boot in ways Flyway never records, silently drifting the real schema out of migration history. `V1__create_project_schema.sql`'s own header comment explains the history here: Flyway used to be hardcoded disabled for this service and `ddl-auto=update` failed silently on every boot ("no schema has been selected to create in"), so no tables ever actually existed until that migration shipped. Flyway is now enabled and is the real source of truth, but `ddl-auto: update` appears to have never been turned back off — it looks like leftover scaffolding rather than a deliberate safety net, and is worth revisiting.

## API Endpoints

Project/SubProject routes live under `/api/v1/projects`; Construction Site routes (including nested Buildings/Floors/Units) live under `/api/v1/construction-sites`. (The base path is `/api/v1/...` — not `/api/projects/v1/...`, which an earlier version of this doc had swapped.)

### Projects (`ProjectController`)

| Method | Path                                     | Description                                                            | Auth |
| ------ | ----------------------------------------- | ------------------------------------------------------------------------- | ---- |
| GET    | `/api/v1/projects`                        | List all projects for tenant (optional `?userId=` assignment filter)      | JWT  |
| GET    | `/api/v1/projects/by-org/{orgId}`         | List projects for tenant/org                                              | JWT  |
| GET    | `/api/v1/projects/{id}`                   | Get project by ID                                                          | JWT  |
| POST   | `/api/v1/projects`                        | Create project (caller becomes permanent `primaryOwnerId`)                | JWT  |
| PATCH  | `/api/v1/projects/{id}`                   | Update project (partial)                                                   | JWT  |
| DELETE | `/api/v1/projects/{id}`                   | Soft-delete project; releases its Construction Site link                  | JWT  |
| POST   | `/api/v1/projects/{id}/assign`            | Assign users with roles (`OWNER`/`MANAGER`/`SUPERVISOR`/`VIEWER`)          | JWT  |
| POST   | `/api/v1/projects/{id}/assign-legacy`     | Assign users without roles (legacy; body `{ "userIds": [...] }`)          | JWT  |
| DELETE | `/api/v1/projects/{id}/assign/{userId}`   | Remove a user (rejected for the primary owner)                             | JWT  |

### SubProjects (`SubProjectController`, nested under Projects)

| Method | Path                                                       | Description                                                |
| ------ | ------------------------------------------------------------ | -------------------------------------------------------------- |
| GET    | `/api/v1/projects/{projectId}/subprojects`                   | List SubProjects for a Project                                  |
| GET    | `/api/v1/projects/{projectId}/subprojects/active`            | List active SubProjects for a Project                           |
| GET    | `/api/v1/projects/{projectId}/subprojects/{subProjectId}`    | Get SubProject by ID                                            |
| GET    | `/api/v1/projects/{projectId}/subprojects/code/{code}`       | Get SubProject by code within a Project                         |
| GET    | `/api/v1/projects/subprojects/external/{externalId}`         | Get SubProject by external ID                                   |
| GET    | `/api/v1/projects/{projectId}/subprojects/status?status=`    | Filter by status                                                |
| GET    | `/api/v1/projects/subprojects/by-type?buildingType=`         | Filter by building type                                         |
| GET    | `/api/v1/projects/subprojects/by-phase?constructionPhase=`   | Filter by construction phase                                    |
| GET    | `/api/v1/projects/subprojects/by-level?buildingLevel=`       | Filter by building level (`WHOLE`/`FLOOR`/`PART`)               |
| POST   | `/api/v1/projects/{projectId}/subprojects`                   | Create SubProject; publishes `project.subproject.created`       |
| PUT    | `/api/v1/projects/{projectId}/subprojects/{subProjectId}`    | Update SubProject (also used for status changes — see [Guarded status transitions](#guarded-status-transitions)) |
| DELETE | `/api/v1/projects/{projectId}/subprojects/{subProjectId}`    | Delete a SubProject                                             |
| DELETE | `/api/v1/projects/{projectId}/subprojects`                   | Delete all SubProjects for a Project                            |

Unlike the Project endpoints above, these read the acting user from a plain `X-User-Id` header rather than the JWT — an inconsistency with `ProjectController`'s JWT-based `RequestIdentity`, not an intentional design choice as far as the code shows.

### Construction Sites, Buildings, Floors, Units (`ConstructionSiteController`)

| Method | Path                                                                        | Description                                                    |
| ------ | ------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| GET    | `/api/v1/construction-sites`                                                    | List sites for tenant                                                 |
| GET    | `/api/v1/construction-sites/{id}`                                               | Get site by ID (with nested buildings/floors/units)                   |
| POST   | `/api/v1/construction-sites`                                                    | Create site (caller becomes owner for quota purposes); quota-checked  |
| PUT    | `/api/v1/construction-sites/{id}`                                               | Update site                                                            |
| DELETE | `/api/v1/construction-sites/{id}`                                               | Delete site                                                            |
| POST   | `/api/v1/construction-sites/{siteId}/buildings`                                | Add building; quota-checked against the site's creator                 |
| PUT    | `/api/v1/construction-sites/{siteId}/buildings/{buildingId}`                   | Update building                                                        |
| DELETE | `/api/v1/construction-sites/{siteId}/buildings/{buildingId}`                   | Delete building                                                        |
| POST   | `/api/v1/construction-sites/{siteId}/buildings/{buildingId}/floors`            | Add floor                                                              |
| PUT    | `/api/v1/construction-sites/{siteId}/buildings/{buildingId}/floors/{floorId}`  | Update floor                                                           |
| DELETE | `/api/v1/construction-sites/{siteId}/buildings/{buildingId}/floors/{floorId}`  | Delete floor                                                           |
| POST   | `.../floors/{floorId}/units`                                                    | Add unit (path prefixed as the floor row above)                        |
| PUT    | `.../floors/{floorId}/units/{unitId}`                                           | Update unit                                                            |
| DELETE | `.../floors/{floorId}/units/{unitId}`                                           | Delete unit                                                            |

Note: `Building`/`Floor`/`Unit` rows have no `created_by`/owner column of their own — only the root `ConstructionSite` tracks a creator, which is what building/floor/unit quota checks are evaluated against.

### Headers

| Header             | Required     | Description                                                                                                                                                     |
| ------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Authorization`     | Yes           | Bearer JWT token                                                                                                                                                    |
| `X-Tenant-Id`       | Yes           | Tenant context (injected by gateway); enforced by a filter that returns 401 if missing                                                                             |
| `X-User-Id`         | Conditional   | Required identity source for SubProject endpoints (see note above); ignored by Project/Construction Site endpoints, which derive the user from the JWT's `internal_user_id` claim instead. Defaults to `"anonymous"` in request logging/MDC when absent. |
| `X-Org-Id`          | No            | Org context for SubProject creation; falls back to the parent Project's `orgId` if absent                                                                          |
| `X-Correlation-Id`  | No            | Request tracing ID                                                                                                                                                   |
| `Content-Type`      | Yes           | application/json (POST/PATCH/PUT)                                                                                                                                    |

### Request/Response Examples

**Create Project:**

```json
POST /api/v1/projects
{
  "code": "PRJ-2026-001",
  "name": "Riverside Commercial Complex",
  "orgId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "description": "Multi-story commercial building with retail and office spaces",
  "propertyId": "f1e2d3c4-b5a6-7890-dcba-f01234567890",
  "addressLine1": "123 Riverside Drive",
  "postalCode": "10115",
  "city": "Berlin",
  "countryCode": "DE",
  "projectType": "COMMERCIAL",
  "startDate": "2026-09-01",
  "endDate": "2027-12-31",
  "budget": 15000000.00,
  "currencyCode": "EUR",
  "clientName": "Riverside Developments Ltd.",
  "clientEmail": "projects@riverside-dev.example"
}
```

Note: `status` is intentionally not accepted here — `ProjectCreateRequest` has no `status` field, and every new Project starts as `PLANNING` (set by the `Project` constructor). Since this service has no lenient/unknown-property Jackson config, sending an extra `"status"` field would actually be rejected as an unrecognized property rather than silently ignored.

**Response:**

```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "tenantId": "tenant-123",
  "orgId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "code": "PRJ-2026-001",
  "name": "Riverside Commercial Complex",
  "description": "Multi-story commercial building with retail and office spaces",
  "status": "PLANNING",
  "projectType": "COMMERCIAL",
  "progress": 0.0,
  "budget": 15000000.0,
  "currencyCode": "EUR",
  "syncStatus": "LOCAL",
  "createdAt": "2026-07-16T10:30:00",
  "updatedAt": "2026-07-16T10:30:00"
}
```

## Configuration

Reflects the actual placeholders in `application.yml` — note these differ from the generic `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`KEYCLOAK_ISSUER_URI` names used by some other services in this repo.

| Environment Variable            | Default                                  | Description                                                                 |
| -------------------------------- | ------------------------------------------ | ------------------------------------------------------------------------------ |
| `PORT`                           | `8085`                                     | HTTP port                                                                       |
| `DB_URL`                         | `jdbc:postgresql://localhost:5432/cerp`    | Full JDBC URL (host/port/db name are baked into this single value)             |
| `DB_USER`                        | `cerp`                                     | Database username                                                              |
| `DB_PASSWORD`                    | `cerp_dev_password`                        | Database password                                                              |
| `OAUTH_ISSUER_URI`               | `http://localhost:8081/realms/cerp`        | Keycloak realm issuer URI                                                       |
| `KAFKA_BOOTSTRAP_SERVERS`        | `localhost:9092`                           | Kafka brokers                                                                   |
| `IDENTITY_SERVICE_URL`           | `http://localhost:8090`                    | Base URL for identity-service subscription/quota lookups                       |
| `SPRING_FLYWAY_ENABLED`          | `true`                                     | Toggles Flyway migrations                                                       |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`  | `update`                                   | See [`ddl-auto: update` alongside Flyway](#ddl-auto-update-alongside-flyway) — kept on in addition to Flyway, unusual for this codebase |
| `OTEL_EXPORTER_OTLP_ENDPOINT`    | `http://localhost:4317`                    | OpenTelemetry collector endpoint                                                |

In `docker-compose.yml`, the container listens on `8085` internally but is published to the host as `8095` (`8095:8085`) — unlike most other services, which publish their native port 1:1.

## Build

```bash
mvn clean package -DskipTests
```

## Run

```bash
java -jar target/project-service-java-0.1.0-SNAPSHOT.jar
```

## Docker

```bash
docker build -t cerp/project-service .
docker run -p 8085:8085 \
  -e DB_URL=jdbc:postgresql://postgres:5432/cerp \
  -e OAUTH_ISSUER_URI=http://keycloak:8081/realms/cerp \
  cerp/project-service
```

## Health Check

```bash
# Liveness
curl http://localhost:8085/actuator/health/liveness

# Readiness
curl http://localhost:8085/actuator/health/readiness

# Info
curl http://localhost:8085/actuator/info
```

## Observability

- Prometheus metrics at `/actuator/prometheus`
- OpenTelemetry tracing supported
- Correlation ID in logs: `traceId`, `spanId`, `correlationId`
- Mapped tenant ID in logs: `tenantId` (from `X-Tenant-Id` header)

## Database Migrations

Flyway manages schema changes under `src/main/resources/db/migration/` (schema: `project`):

- `V1__create_project_schema.sql` — Initial schema: `projects`, `construction_sites`, `buildings`, `floors`, `units`, `sub_projects`, with tenant/org isolation indexes. Its header comment notes it replaces an earlier public-schema migration set that, per that same comment, never actually ran: Flyway used to be hardcoded disabled for this service and `ddl-auto=update` failed silently on every boot, so no project tables existed anywhere before this migration shipped.
- `V2__enforce_one_to_one_site_project_and_building_subproject.sql` — Adds the partial/unique indexes backing "one active Project per Construction Site" and "one SubProject per Building" (see [DB-enforced structural invariants](#db-enforced-structural-invariants)).
- `V3__add_primary_owner_to_projects.sql` — Adds `projects.primary_owner_id`.

## Data Model

```
projects (schema: project)
├── id (UUID) - Primary key
├── tenant_id (VARCHAR) - Tenant scope
├── org_id (UUID) - Organization reference
├── external_id (VARCHAR, unique) - External system reference
├── code (VARCHAR 64) - Project code (uniqueness enforced in ProjectService, not a DB constraint)
├── name (VARCHAR 255) - Project name
├── construction_site_id (VARCHAR 64) - at most one active project per site (idx_projects_construction_site_id_active)
├── property_id (UUID) - Property reference (nullable)
├── address_* (VARCHAR) - Address fields
├── status (ENUM) - PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED, SUSPENDED
├── project_type (ENUM) - NEW_CONSTRUCTION, RENOVATION, MAINTENANCE, INFRASTRUCTURE, COMMERCIAL, RESIDENTIAL, INDUSTRIAL
├── start_date, end_date, actual_start_date, actual_end_date - Date fields
├── progress (NUMERIC) - 0.00 to 100.00
├── budget (NUMERIC) - Total project budget
├── currency_code (VARCHAR) - ISO 4217
├── client_name, client_email, client_phone - Client info
├── is_deleted (BOOLEAN) - soft-delete flag
├── sync_status (ENUM) - LOCAL, REMOTE, CONFLICT, PENDING_SYNC
├── primary_owner_id (UUID) - permanent creator/owner, immutable after creation (added in V3)
├── assigned_user_ids (TEXT[]) - users granted access to this project
├── assigned_user_roles (JSONB) - {"<userId>": "OWNER"|"MANAGER"|"SUPERVISOR"|"VIEWER"}
├── created_at, updated_at - Audit timestamps
└── created_by, updated_by - Audit users

sub_projects (schema: project)
├── id (UUID) - Primary key
├── tenant_id, org_id, project_id (FK → projects.id)
├── external_id (VARCHAR, unique), code (VARCHAR 64), name, description
├── building_type (ENUM) - RESIDENTIAL, COMMERCIAL, INDUSTRIAL, MIXED_USE, PUBLIC, PARKING, UTILITY, INFRASTRUCTURE, OTHER
├── construction_phase (ENUM) - 15 phases, PRE_CONSTRUCTION .. FACILITY_MANAGEMENT (drives applicable workflow templates)
├── building_level (ENUM) - WHOLE, FLOOR, PART
├── floor_ids, part_ids (TEXT[]) - populated when building_level = FLOOR / PART respectively
├── usable_area, total_area (NUMERIC), floor_count, unit_count
├── status (ENUM) - PLANNING, IN_CONSTRUCTION, ON_HOLD, COMPLETED, CANCELLED (guarded transitions - see Architecture)
├── start_date, end_date, progress, budget, currency_code
├── building_id (UUID, nullable, unique) - bridges to the Construction Site hierarchy below; at most one SubProject per Building (idx_subproject_building_id_unique)
└── sync_status, created_at, updated_at, created_by, updated_by
```

Construction Sites are a separate, simpler tree: `construction_sites` → `buildings` (FK `construction_site_id`, cascade delete) → `floors` (FK `building_id`, cascade delete) → `units` (FK `floor_id`, cascade delete). Only `construction_sites` has a `created_by` column (used for per-creator quota checks) — `buildings`/`floors`/`units` carry no owner/audit columns of their own.

## Kafka Topics

**The topics declared in code and the topics actually published to don't match.** This table documents what the code actually does, not just what `KafkaTopicConfig` declares:

| Topic                           | Direction | Declared as a `NewTopic` bean? | Publisher / consumer                                                                                     |
| --------------------------------- | --------- | --------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `organization.project.created`    | Publish   | No                                 | `ProjectService.create()` — no consumer found anywhere in this monorepo                                        |
| `organization.project.updated`    | Publish   | No                                 | `ProjectService.update()` — no consumer found anywhere in this monorepo                                        |
| `organization.project.deleted`    | Publish   | No                                 | `ProjectService.delete()` — no consumer found anywhere in this monorepo                                        |
| `project.subproject.created`      | Publish   | No                                 | `SubProjectService.createSubProject()` → consumed by `workflow-service-java`'s `WorkflowEventListener` (`TOPIC_SUBPROJECT_CREATED`) to auto-generate workflows |
| `project.created`                 | —         | Yes (`KafkaTopicConfig`)          | Declared/auto-created; nothing in this service publishes to it                                                 |
| `project.updated`                 | —         | Yes (`KafkaTopicConfig`)          | Declared/auto-created; nothing in this service publishes to it                                                 |
| `project.deleted`                 | —         | Yes (`KafkaTopicConfig`)          | Declared/auto-created; nothing in this service publishes to it                                                 |

Two distinct problems worth fixing rather than just documenting:

1. `KafkaTopicConfig` declares `project.{created,updated,deleted}`, but `ProjectService` actually publishes Project lifecycle events under `organization.project.{created,updated,deleted}` — a different prefix. The declared topics look dead; the real ones are undeclared by this service's own config.
2. `project.subproject.created` is sent via a raw `kafkaTemplate.send("project.subproject.created", ...)` string literal with no corresponding `NewTopic` bean in this service, even though `workflow-service-java` depends on it existing. This service is publishing to a topic it doesn't formally own/declare — a real cross-service coupling smell for whoever next touches either service.

## Project Status Lifecycle

```
PLANNING → ACTIVE → COMPLETED
   │          │
   ├───────► ON_HOLD ────────► ACTIVE
   │
   ├───────► CANCELLED
   │
   └───────► SUSPENDED
```

## SubProject Status Lifecycle

Reachable only via the guarded methods on `SubProject` (see [Guarded status transitions](#guarded-status-transitions)) — **except** the SubProject `PUT` update endpoint, which can set `status` directly and bypass this entirely:

```
PLANNING → IN_CONSTRUCTION ──► COMPLETED
              │      ▲
              ▼      │
           ON_HOLD ──┘

CANCELLED - only reachable via a direct setStatus() call (e.g. through the update
            endpoint), not via any of the named guarded-transition methods
```
