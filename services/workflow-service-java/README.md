# Workflow Service

Spring Boot 3 service that owns process execution for CERP: construction workflow instances, workflow templates, the vendor bidding/tender lifecycle, and a generic guarded state-machine library.

## Purpose

Where `project-service-java` owns the *structural* hierarchy (project → construction site → subproject → building level), this service owns *process on top of that structure* — a subproject exists whether or not any workflow has ever been instantiated against it. Concretely it owns two structurally different kinds of process, kept deliberately separate rather than forced into one engine:

1. **Construction workflows** (`Workflow`/`WorkflowStep`) — a single-aggregate, sequential, guarded status flow scoped to one project → subproject → floor/part triad, auto-instantiated from a `WorkflowTemplate` whenever a subproject is created. No service boundary is crossed executing one of these.
2. **Vendor bidding / tendering** (`Saga`/`SagaHandler`) — a process that genuinely spans multiple aggregates and external actors (create a tender, invite vendors or publish it openly, collect bids, evaluate, award, issue a PO) and needs rollback/compensation semantics if it's aborted partway through.

Every other service that needs "make something happen over several steps, possibly across services" depends on this one rather than re-implementing its own orchestration.

## Responsibilities

- Instantiate and drive `Workflow`/`WorkflowStep` instances from templates, enforcing legal status transitions
- Auto-generate a subproject's workflows on creation, via a Kafka listener
- Manage `WorkflowTemplate`/`WorkflowTemplateStep` definitions, per-organisation workflow config, and template-phase mappings
- Orchestrate the bidding/tender saga end-to-end, including compensation on abort
- Own the bidding domain: vendor bid wallets, bid submissions, bid transactions, channel subscriptions
- Provide a generic, reusable guarded-transition engine (`StateMachineEngine`) — currently backing `Workflow`/`WorkflowStep`; predefined-but-unwired definitions also exist for Procurement/PurchaseOrder/Delivery/Tender/ProgressClaim, ready for those domains to adopt when they're built
- Publish/consume Kafka events for cross-service coordination, with a (partial — see Known gaps) transactional outbox for reliable saga event delivery

## Architecture

```
                         Kafka: project.subproject.created
                                        │
                                        ▼
                          WorkflowEventListener
                                        │
                                        ▼
                     SubProjectWorkflowGenerator ──reads──▶ WorkflowTemplate
                                        │                    (+ TemplateStep)
                                        ▼
                          creates Workflow + WorkflowStep[]

┌─────────────────────────────────────────────────────────────────────┐
│  REST layer (7 controllers, /api/v1/**)                             │
│    WorkflowController · WorkflowTemplateController · SagaController │
│    BiddingController · WorkflowGenerationController                 │
│    OrganisationWorkflowConfigController · TemplatePhaseMappingCtrl  │
└───────────────────────────────┬───────────────────────────────────┘
                                 ▼
        ┌────────────────────────────────────────────┐
        │  Service layer                              │
        │  WorkflowService        SagaOrchestrator     │
        │  (guarded transitions)  (BiddingSagaHandler, │
        │        │                 ProcurementSagaHandler,│
        │        ▼                 DeliverySagaHandler) │
        │  StateMachineEngine            │             │
        │  + WorkflowStateStore(s)       ▼             │
        │        │                 OutboxEvent /       │
        │        ▼                 EventPublisher      │
        │  JPA repositories ──────────────┘            │
        └───────────────────┬──────────────────────────┘
                             ▼
              PostgreSQL: workflow schema + bidding schema
                    (shared `cerp` database)

Outbound HTTP: identity-service-java (8090) · vendor-service-java (8085) ·
               notification-service-java (8091)
```

### Why two orchestration mechanisms, not one

This is the central design decision in the service, and it was made deliberately rather than by accident: **Saga is reserved for process that crosses aggregates/services and may need to roll back; `StateMachineEngine` is used for a single aggregate's own guarded status flow.** Construction workflow steps never cross a service boundary and never need to undo a completed step, so forcing them through `SagaHandler`'s string-keyed dispatch and freeform JSON context would trade a simple guarded enum for machinery built for a different problem. The tender/bidding flow is the opposite: it really does touch vendor invitations, bid collection, evaluation, and award across what are conceptually different actors, and `BiddingSagaHandler` provides (weak, mostly event-emitting) compensation for that. Both mechanisms live in this one service because both are "process," but they are not the same abstraction and are not layered on top of each other.

### Workflow/WorkflowStep now run on `StateMachineEngine`

As of the most recent change to this service, `Workflow` and `WorkflowStep` no longer self-guard their own status transitions. Previously each transition method (`start()`, `block()`, `complete()`, …) called a hand-rolled `requireStatus(...)` allow-list check before mutating `this.status` directly. That guard now lives in `WorkflowStateMachines` (two `StateMachineDefinition` graphs — one per aggregate, since no template varies the *transition rules*, only the step content), and `WorkflowService` calls `StateMachineEngine.executeTransition(...)` before invoking the now side-effect-only entity method. `WorkflowStateStore`/`WorkflowStepStateStore` adapt the engine's generic `StateStore` interface directly onto the existing `workflows.status` / `workflow_steps.status` columns — deliberately **no separate state-tracking table**, since those columns are already the indexed, `CHECK`-constrained source of truth and a parallel table would just create a second one to desync from. This finally gives the previously-dormant `StateMachineEngine` (see Known gaps) a real caller.

### Templates only vary step content, never transition rules

`WorkflowService.instantiate()` builds a `Workflow`'s steps purely from `WorkflowTemplate`/`WorkflowTemplateStep` data (order, code, name, weight, due-offset) — none of the ~20+ construction-phase templates branch on status-transition logic. That's why one `workflowMachine()` and one `workflowStepMachine()` definition is sufficient for every template rather than needing a machine-per-template registry.

## Controllers / API surface

| Base path | Controller | Purpose |
|---|---|---|
| `/api/v1/workflows` | `WorkflowController` | Instantiate workflows, drive status transitions, manage steps and notes |
| `/api/v1/workflow-templates` | `WorkflowTemplateController` | CRUD for reusable workflow templates and their step definitions |
| `/api/v1/sagas` | `SagaController` | Start/advance/pause/resume/compensate sagas (`POST .../steps`, `/compensate`, `/pause`, `/resume`) |
| `/api/v1/bidding` | `BiddingController` | Vendor bid wallet and bid submission operations |
| `/api/v1` | `WorkflowGenerationController` | Manual/administrative workflow generation (mirrors the Kafka-triggered auto-generation) |
| `/api/v1/{tenantId}/organisations/{orgId}/workflow-config` | `OrganisationWorkflowConfigController` | Per-organisation workflow configuration |
| `/api/v1/{tenantId}/template-phase-mappings` | `TemplatePhaseMappingController` | Maps construction phases to template selection |

## Data Model

**`workflow` schema:**
- `workflows` — id, tenant/project/subproject scope, template ref, `status` (`PLANNED/IN_PROGRESS/BLOCKED/ON_HOLD/COMPLETED/APPROVED/REJECTED/CANCELED`), `progress_percent`, scope level (WHOLE/FLOOR/PART), next-step pointer
- `workflow_steps` — belongs to a workflow, `status` (`NOT_STARTED/IN_PROGRESS/BLOCKED/COMPLETED/APPROVED/REJECTED`), assignee, evidence/notes, weight (drives progress-percent calculation)
- `workflow_templates` / `workflow_template_steps` / `workflow_template_step_assignee_roles` — reusable template definitions
- `organisation_workflow_config`, `template_phase_mapping` — per-tenant/organisation template selection rules
- `saga_events` — full saga context/history as JSON, keyed by saga id
- `outbox_events` — transactional outbox rows for reliable event publishing (see Known gaps)
- `workflow_notes` — free-text audit/annotation trail

**`bidding` schema:**
- `bid_wallets` — per-entity bid balance (`entityId` is distinct from `userId`, intended as a future company-billing attribution hook)
- `vendor_bid_submissions`, `bid_transactions`, channel subscriptions

## Kafka

| Topic | Direction | Description |
|---|---|---|
| `project.subproject.created` | Consume | From project-service-java; triggers `SubProjectWorkflowGenerator` to instantiate that subproject's workflows. Not currently declared in project-service-java's own topic config — a loose, name-only contract between the two services worth tightening. |
| `TOPIC_WORKFLOW_STARTED` / `BLOCKED` / `COMPLETED` / etc. | Declared, unused | `KafkaTopicConfig` declares a full set of workflow-lifecycle topics; nothing currently publishes to them (see Known gaps). |

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8089` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/cerp` | Shared Postgres instance; this service owns the `workflow` schema within it (`spring.jpa.properties.hibernate.default-schema`), with its own Flyway history table (`workflow_flyway_history`) to avoid clashing with other services' migrations |
| `DB_USER` / `DB_PASSWORD` | `cerp` / `cerp_dev_password` | Database credentials |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `OAUTH_ISSUER_URI` | `http://keycloak:8081/realms/master` | Keycloak issuer for JWT validation |
| `IDENTITY_SERVICE_URL` | `http://identity-service-java:8090` | Resolves users/roles (e.g. vendor invitation targets) |
| `VENDOR_SERVICE_URL` | `http://vendor-service-java:8085` | Vendor lookups during bidding |
| `NOTIFICATION_SERVICE_URL` | `http://notification-service-java:8091` | Notifies vendors/approvers of workflow and bidding events |

`spring.jpa.hibernate.ddl-auto` is `validate` — Flyway is the sole owner of schema changes; Hibernate only checks the entities match at startup.

## Database Migrations

Flyway migrations under `src/main/resources/db/migration/`:

- `V1__create_workflow_schema.sql` — workflows, workflow_steps, templates, outbox
- `V2__create_bidding_schema.sql` — bid wallets, submissions, transactions
- `V3__seed_reference_data.sql`, `V4__seed_template_steps.sql` — seed data for templates/phases
- `V5__seed_bid_wallets.sql`, `V6__fix_bid_wallet_user_ids.sql` — bid wallet seed/fix-up

## Build

```bash
mvn clean package -DskipTests
```

## Run

```bash
mvn spring-boot:run
```

## Docker

```bash
docker build -t cerp/workflow-service .
docker run -p 8089:8089 \
  -e DB_URL=jdbc:postgresql://postgres:5432/cerp \
  -e OAUTH_ISSUER_URI=http://keycloak:8081/realms/master \
  cerp/workflow-service
```

## Health Check

```bash
curl http://localhost:8089/actuator/health
```

## Known gaps

Documented here deliberately rather than glossed over, since they're relevant to anyone extending this service:

- **`BiddingSagaHandlerTest` currently has 4 failing tests** (`testExecuteCollectBids`, `testExecuteCollectBidsEmptyThrows`, `testExecuteEvaluateBids`, `testGetNextStep`), confirmed present on the current `main` branch independent of any other work — looks like test/implementation drift in the tender step sequence (the test expects `Tender.CreateAndOpen → Tender.InviteVendors`, the handler's `getNextStepCode` currently returns `Bidding.CollectBids`). Needs a decision on which side is correct before fixing.
- **Outbox events are written but nothing drains them to Kafka.** `OutboxEvent` rows are persisted by `InMemoryEventPublisher`, but no `@Scheduled` processor publishes them onward — a separate `KafkaEventPublisher` exists and publishes directly to Kafka, but isn't wired through the same `EventPublisher` interface the rest of the code depends on via DI.
- **`StateMachineEngine`/`PredefinedStateMachines`'s five other aggregate definitions (Procurement, PurchaseOrder, Delivery, Tender, ProgressClaim) have no corresponding JPA entities or call sites yet.** They're ready to use once those domains are actually built here or elsewhere; `Workflow`/`WorkflowStep` (via `WorkflowStateMachines`) are the only current consumers of the engine.
