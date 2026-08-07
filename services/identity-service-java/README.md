# Identity Service

## Purpose

Identity Service is CERP's system of record for "who is allowed to do what." It owns user
accounts, tenants, tenant- and resource-level role assignments, and per-user subscription/plan
quotas, and it is the only service that talks to Keycloak. Authentication and authorization are
cross-cutting concerns every other microservice needs but none of them should own independently —
if each service kept its own copy of "who is this user," tenant isolation would have to be
duplicated (and would drift) across every service instead of being centralized once, verified once,
and trusted everywhere else.

Every other backend service depends on it in two ways:

1. **JWT validation** — all Java services validate the Keycloak-issued JWT on incoming requests via
   `spring-boot-starter-oauth2-resource-server`, trusting the `internal_user_id` claim that
   identity-service is responsible for minting (see [Design decisions](#design-decisions) below).
2. **Direct REST calls** — project-service resolves a user's plan/quota limits before allowing new
   construction sites/projects; workflow-service resolves a user's accessible/writable/readable
   projects for saga listing access control; vendor-service resolves a JWT `sub`/email to an
   identity-service user id. `apps/web`'s BFF calls identity-service directly for login,
   registration, and profile management.

## Responsibilities

- User account lifecycle: create/read/list/delete `UserAccount` records (email, name, bcrypt
  password hash, account **Type**, status), plus self-service profile read/update.
- Tenant lifecycle: create/read/list/delete `Tenant` records.
- Tenant-scoped role assignment: `Membership` (`tenant_id` + `user_id` → `roles[]` + `scopes{}`),
  enforced with PostgreSQL row-level security.
- Resource-scoped access control: `ResourceMembership` (`PROJECT`/`CONSTRUCTION_SITE` + resource id
  + user id → `roles[]` + granular read/write/delete/assign permissions), plus service-to-service
  permission-check endpoints other services call directly.
- Subscriptions and plan quotas: exactly one `Subscription` per user (Freemium/Professional/Premium)
  and a `PlanLimits` table of resource quotas per plan, exposed for other services to enforce.
- Keycloak integration: bootstraps the `cerp` realm/client at startup, dual-writes every
  `UserAccount` into Keycloak, and issues/refreshes real tokens via Direct Access Grant.
- Event publishing: emits tenant/user/membership/vendor-identity/subscription-created events to
  Kafka for other services to react to asynchronously.
- Event consumption: consumes vendor-registration events from vendor-service and provisions the
  corresponding identity records (user, membership, subscription).

## Architecture

```
                    ┌─────────────────────────────────────────────┐
                    │   Other services / apps/web (BFF)            │
                    │   project- · workflow- · vendor-service,     │
                    │   api-gateway, apps/web                      │
                    └───────────────────┬───────────────────────────┘
                                        │
        validate JWT, trust the             REST calls: /memberships,
        "internal_user_id" claim             /api/v1/users/*/subscription,
        (spring-oauth2-resource-server)      /api/v1/resources/**,
                                        │     /api/v1/users/*/can-*-project, ...
                                        ▼
┌───────────────────────────────────────────────────────────────────────┐
│                     identity-service-java   (:8090)                   │
│                                                                        │
│  Controller   AuthController · UserController · TenantController ·    │
│               MembershipController · ResourceMembershipController ·   │
│               SubscriptionController                                  │
│       │                                                                │
│       ▼                                                                │
│  Service      UserService · TenantService · MembershipService ·       │
│               ResourceAccessService · SubscriptionService             │
│       │                                              │                │
│       ▼                                              ▼                │
│  Repository   Spring Data JPA (repo/*)      IdentityEventPublisher    │
│       │                                              │                │
│       ▼                                              ▼                │
│  PostgreSQL "identity" schema                Kafka (identity.*.v1)    │
│  tenants · user_accounts · memberships ·                              │
│  resource_members · subscriptions · plan_limits                       │
└──────────────────────────┬──────────────────────────────────────────┘
                           │  KeycloakAdminClient / KeycloakBootstrap
                           │  (Keycloak Admin REST API over HTTP)
                           ▼
               ┌──────────────────────────────────────────┐
               │  Keycloak — realm "cerp"                   │
               │  client "cerp-identity-service"            │
               │  • every UserAccount dual-written here     │
               │  • login exchanges password for a real,    │
               │    signed token (Direct Access Grant)      │
               │  • realm/client bootstrapped imperatively  │
               │    at identity-service startup             │
               └──────────────────────────────────────────┘
```

### Design decisions

**Real Keycloak, not a hand-rolled JWT.** `AuthController.login()` still verifies the password
locally (bcrypt, against `UserAccount.passwordHash`), but then exchanges the credentials with
Keycloak via a Direct Access Grant to obtain a real, signed token pair instead of minting its own
unsigned JWT. This lets every downstream service validate the signature cryptographically
(`spring-boot-starter-oauth2-resource-server` against Keycloak's issuer/JWK endpoint) instead of
trusting an unverified payload.

**Imperative Keycloak bootstrap, not declarative realm-import.** `KeycloakBootstrap` runs as a
`CommandLineRunner` (`SeedDataConfig`, `@Order(1)`, before any user is synced) and creates the
`cerp` realm, the `cerp-identity-service` client, its service-account `manage-users` role, the
`internal_user_id` protocol mapper, and two User Profile fixes (making `firstName`/`lastName`
optional, and declaring `internal_user_id`/`account_type`/`tenant_id` as recognized custom
attributes) — all via the Admin REST API. The code comments say declarative `--import-realm` was
tried first and hit an apparent bug in this Keycloak version (`ExportImportManager` NPEs with
"directory not found" even with the import directory correctly mounted); this imperative bootstrap
was the workaround, and it is idempotent (safe to run on every startup).

**`internal_user_id`, never `sub`.** This is the most important gotcha in the whole service.
Keycloak does not honor a client-supplied id when creating a user — it silently assigns its own,
so a Keycloak token's `sub` claim is *Keycloak's* internal id, not `UserAccount.id`. To work around
this, `KeycloakAdminClient.upsertUser()` stores identity-service's own id in a Keycloak user
attribute called `internal_user_id`, and `KeycloakBootstrap` exposes that attribute as a same-named
token claim via a protocol mapper. **Every downstream service must read the `internal_user_id`
claim to get the caller's identity-service user id — never `sub`.** `project-service`'s
`RequestIdentity.creatorId(Jwt)` is the reference implementation of this pattern. Within
identity-service itself, `AuthController.refresh()` follows the rule correctly — but see
[Known rough edges](#known-rough-edges) below for two places in this same service that don't.

**One shared tenant, today.** Every registration path observed in the code — `VendorRegistrationConsumer`
and the comments referencing `apps/web`'s registration routes — hardcodes tenant id
`00000000-0000-0000-0000-000000000001` rather than creating a tenant per signup. `Tenant` and
`TenantController` fully support creating arbitrary tenants (and `SeedDataConfig` creates two more,
purely for local dev personas John and Admin), but nothing in this codebase currently exercises a
"new tenant per signup" flow. `identity.memberships` is the only RLS-protected table
(`app.tenant_id` session variable, bound per-request by `RlsTenantAspect` + `TenantRequestFilter`);
`tenants` and `user_accounts` are treated as global, un-scoped tables (V3 migration).

**Creator-based ownership is assembled from parts this service provides, not decided by this
service.** The actual "whoever creates a Project becomes its owner" rule lives in project-service
(`Project.primaryOwnerId`, set at creation from `RequestIdentity.creatorId(jwt)` — i.e., from the
`internal_user_id` claim identity-service mints). What identity-service contributes: (1) the
verified identity the creator is recorded under; (2) `ResourceMembership`, the fine-grained
per-resource RBAC table other collaborators are invited into with `owner`/`manager`/`member`/`viewer`
roles and read/write/delete/assign permissions (seed data shows the creator recorded as their own
`invited_by`); and (3) `Subscription`/`PlanLimits`, keyed by **user, not tenant** — a Company
account's plan does not become a shared entitlement for everyone under its tenant. This is why
quota checks (e.g. project-service's `GET /api/v1/users/{userId}/subscription` call) are always
made against the resource's *owner*, not its tenant or the caller.

### Known rough edges

Discovered while reading the current code — worth an engineer's attention, not necessarily "bugs to
fix today":

1. **`/users/profile` (GET/PATCH) skips Keycloak signature validation entirely.**
   `SecurityConfig.userProfileFilterChain` deliberately omits `oauth2ResourceServer`, and both
   `ProfileBypassFilter` and `UserController`'s own `extractUserIdFromToken` manually
   base64-decode the JWT payload (no signature check) and read `sub`/`userId`/`id` — **not**
   `internal_user_id`. This is inconsistent with every other authenticated endpoint in this service
   and with the rule downstream services are told to follow; it reads like pre-Keycloak legacy code
   that was never migrated.
2. **`ProfileBypassFilter` stores a user id in `TenantContext`.** `TenantContext` is otherwise used
   exclusively to carry a *tenant* id for Postgres RLS (`RlsTenantAspect` binds it to
   `app.tenant_id`). On the `/users/profile` path, `TenantContext.set(userUuid)` is called with the
   *user's* id, which looks like a naming/usage mix-up rather than intentional behavior.
3. **`GET /tenants` and `GET /users` (no path segment) are fully unauthenticated.** Any caller can
   list every tenant, and every user account's id/email/name/type/status, with no token at all —
   these share a `permitAll()` rule with the registration endpoints they sit next to.
4. **The `permitAll()` for `DELETE /tenants/**`/`DELETE /users/**` looks unreachable.**
   `publicApiFilterChain`'s own `securityMatcher(...)` list only contains the literal paths
   `/tenants` and `/users` (no `/**`), so a request like `DELETE /tenants/{id}` doesn't match this
   chain at all and falls through to `defaultFilterChain`'s `anyRequest().authenticated()`. The
   "DELETE endpoints for cleanup" comment suggests these were meant to be open (e.g. for e2e test
   teardown); as configured, they likely require a valid JWT instead. Worth confirming empirically.
5. **The internal permission-check endpoints aren't in the public carve-out, but their callers send
   no token.** `GET /api/v1/users/*/subscription` is explicitly `permitAll()`, but the sibling
   endpoints `/api/v1/users/{userId}/accessible-projects`, `/can-write-project`, `/can-read-project`,
   `/can-delete-project`, and `/is-tenant-admin-or-manager` are not — they fall under the default
   chain's `anyRequest().authenticated()`. Their only known caller, workflow-service's
   `IdentityPermissionClient`, calls them with no `Authorization` header, so today these calls may
   simply fail auth and silently resolve to each call site's fail-open/fail-closed default rather
   than a real permission check.

## API Endpoints

### Auth (`/auth`)

| Method | Path            | Description                                                                     | Auth   |
| ------ | --------------- | -------------------------------------------------------------------------------- | ------ |
| POST   | `/auth/login`   | Verify username/email + password locally, then exchange for a Keycloak-issued token pair (Direct Access Grant) | Public |
| POST   | `/auth/refresh` | Exchange a refresh token for a new Keycloak token pair                          | Public |

### Users (`/users`)

| Method | Path             | Description                                                                 | Auth                              |
| ------ | ---------------- | ---------------------------------------------------------------------------- | ---------------------------------- |
| POST   | `/users`         | Create a user account (email, name, optional password, required `type`); dual-writes to Keycloak | Public |
| GET    | `/users`         | List all users, or look up one by `?email=`                                 | Public                              |
| GET    | `/users/{id}`    | Get a user by id                                                             | JWT (Keycloak)                     |
| DELETE | `/users/{id}`    | Delete a user (cascades to memberships/resource_members/subscriptions)      | JWT (Keycloak) — see rough edge #4 |
| GET    | `/users/profile` | Get the caller's own profile, resolved from the bearer token                | Bearer token present, not signature-verified — see rough edge #1 |
| PATCH  | `/users/profile` | Update the caller's own profile (`name`)                                    | Bearer token present, not signature-verified — see rough edge #1 |

### Tenants (`/tenants`)

| Method | Path            | Description        | Auth                                |
| ------ | --------------- | ------------------- | ------------------------------------ |
| POST   | `/tenants`      | Create a tenant     | Public                                |
| GET    | `/tenants`      | List all tenants    | Public                                |
| GET    | `/tenants/{id}` | Get a tenant by id  | JWT (Keycloak)                       |
| DELETE | `/tenants/{id}` | Delete a tenant     | JWT (Keycloak) — see rough edge #4  |

### Memberships (`/memberships`) — tenant-level roles

| Method | Path                                     | Description                                              | Auth   |
| ------ | ----------------------------------------- | ---------------------------------------------------------- | ------ |
| POST   | `/memberships`                            | Assign/update a user's roles + scopes on a tenant          | Public (registration flow) |
| GET    | `/memberships?tenantId=&userId=`          | Get one membership                                          | Public |
| GET    | `/memberships?tenantId=`                  | List memberships in a tenant                                | Public |
| GET    | `/memberships?userId=`                    | List a user's memberships                                   | Public |
| DELETE | `/memberships/{id}`                       | Remove a membership                                          | JWT (Keycloak) |

### Resource memberships & permission checks (`/api/v1`) — resource-level RBAC

| Method | Path                                                                        | Description                                                                 | Auth |
| ------ | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ---- |
| GET    | `/api/v1/resources/{resourceType}/{resourceId}/members`                    | List all users with access to a resource                                   | JWT (Keycloak) |
| GET    | `/api/v1/resources/{resourceType}/{resourceId}/members/{userId}`           | Get one user's access to a resource                                        | JWT (Keycloak) |
| POST   | `/api/v1/resources/{resourceType}/{resourceId}/members`                    | Grant a user access to a resource                                          | JWT (Keycloak) |
| PATCH  | `/api/v1/resources/memberships/{membershipId}`                             | Update a resource membership's roles/permissions                           | JWT (Keycloak) |
| DELETE | `/api/v1/resources/memberships/{membershipId}`                             | Revoke a resource membership                                                | JWT (Keycloak) |
| POST   | `/api/v1/resources/{resourceType}/{resourceId}/members/{userId}/role/{roleName}` | Quick-assign a standard role (`owner`/`manager`/`member`/`viewer`) and its permission preset | JWT (Keycloak) |
| GET    | `/api/v1/users/{userId}/accessible-projects`                                | Service-to-service: project ids the user can access (empty ⇒ tenant admin/manager = full access) | JWT (Keycloak) — see rough edge #5 |
| GET    | `/api/v1/users/{userId}/can-write-project?projectId=`                      | Service-to-service permission check                                         | JWT (Keycloak) — see rough edge #5 |
| GET    | `/api/v1/users/{userId}/can-read-project?projectId=`                       | Service-to-service permission check                                         | JWT (Keycloak) — see rough edge #5 |
| GET    | `/api/v1/users/{userId}/can-delete-project?projectId=`                     | Service-to-service permission check                                         | JWT (Keycloak) — see rough edge #5 |
| GET    | `/api/v1/users/{userId}/is-tenant-admin-or-manager`                        | Service-to-service: is this user a tenant-level admin/manager (full access)? | JWT (Keycloak) — see rough edge #5 |

### Subscriptions

| Method | Path                                    | Description                                                | Auth   |
| ------ | ---------------------------------------- | ------------------------------------------------------------ | ------ |
| POST   | `/subscriptions`                        | Create a subscription for a user (defaults to `FREEMIUM`)   | Public |
| GET    | `/api/v1/users/{userId}/subscription`   | Get a user's subscription plus its resolved plan limits      | Public (explicit internal carve-out; called by project-service) |

## Data Model

```
identity.tenants
├── id (UUID, PK)
├── name, region
└── created_at, updated_at

identity.user_accounts
├── id (UUID, PK)
├── email (unique), name
├── password_hash            -- bcrypt; also dual-written to Keycloak
├── type                     -- USER | VENDOR | COMPANY, NOT NULL, immutable (CHECK constraint)
├── status                   -- free-text, default 'active'
└── created_at, updated_at

identity.memberships                        -- tenant-scoped roles; RLS-enabled (app.tenant_id)
├── id (UUID, PK)
├── tenant_id, user_id       -- FK to tenants/user_accounts, UNIQUE together
├── roles                    -- JSONB array of free-form strings (admin, manager, owner, user, vendor, viewer, ...)
├── scopes                   -- JSONB object, free-form
└── created_at, updated_at

identity.resource_members                   -- fine-grained, per-resource RBAC
├── id (UUID, PK)
├── resource_type            -- PROJECT | CONSTRUCTION_SITE
├── resource_id, user_id     -- UNIQUE together with resource_type
├── roles                    -- JSONB array, defaults to ["viewer"]
├── permissions              -- JSONB {read, write, delete, assign}: boolean, defaults to read-only
├── invited_by               -- FK to user_accounts, nullable
└── created_at, updated_at

identity.subscriptions                       -- exactly one per user, keyed by user_id (not tenant_id)
├── id (UUID, PK)
├── user_id (FK, UNIQUE)
├── plan                     -- FK -> plan_limits.plan
├── status                   -- default 'active'
└── created_at, updated_at

identity.plan_limits                         -- one row per Plan; the plan itself is the PK
├── plan                     -- PK: FREEMIUM | PROFESSIONAL | PREMIUM
├── max_construction_sites, max_buildings
├── max_owners_per_project, max_managers_per_project, max_members_per_project   -- NULL = uncapped
├── starting_bid_grant       -- NOT NULL; seeds workflow-service's bid wallet at signup
└── updated_at
```

**Enums:** `UserAccount.AccountType` (`USER`, `VENDOR`, `COMPANY` — fixed, exhaustive, distinct from
the free-form "roles" on `Membership`/`ResourceMembership`; `COMPANY` is defined and allowed by the
DB `CHECK` constraint but no flow in this codebase creates one yet). `Plan` (`FREEMIUM`,
`PROFESSIONAL`, `PREMIUM`). `ResourceMembership.ResourceType` (`PROJECT`, `CONSTRUCTION_SITE`).

**Plan limits today** (`V10`/`V12` migrations):

| Plan         | Sites | Buildings | Owners/project | Managers/project | Members/project | Starting bid grant |
| ------------ | ----- | --------- | --------------- | ------------------ | ------------------ | ------------------- |
| FREEMIUM     | 1     | 1         | 1                | 1                   | 0                   | 50                   |
| PROFESSIONAL | 10    | 10        | unlimited        | unlimited           | 5                   | 200                  |
| PREMIUM      | unlimited | unlimited | unlimited     | unlimited           | unlimited           | 400                  |

**Structural rule:** identity-service stores and exposes quotas (`plan_limits`, via
`GET /api/v1/users/{userId}/subscription`) but does not itself enforce them — enforcement is the
resource-owning service's job (today, project-service compares its own counts against the returned
limits before allowing creation). Freemium's `max_owners_per_project = 1` /
`max_managers_per_project = 1` is documented in the migration as an *exact* cardinality rule
("exactly 1 owner + 1 manager"), not just a ceiling — but enforcing that floor is likewise left to
project-service, not encoded here.

## Kafka Topics

Gated by `app.events.enabled` (`APP_EVENTS_ENABLED`) — when disabled, the producer/consumer beans,
topic declarations, and the vendor-registration listener are all conditionally absent.

### Published (`IdentityEventPublisher`)

| Topic                       | Event type                          | Description                                                                 |
| ---------------------------- | ------------------------------------ | ------------------------------------------------------------------------------ |
| `identity.tenant.v1`        | `identity.tenant.created`           | A tenant was created                                                        |
| `identity.user.v1`          | `identity.user.created`             | A user account was created                                                  |
| `identity.membership.v1`    | `identity.membership.role_assigned` | Tenant-level roles were assigned/updated for a user                         |
| `identity.vendor.v1`        | `identity.vendor.identity_created`  | Identity (tenant + user) created for a vendor onboarded elsewhere, so vendor-service can link its record |
| `identity.subscription.v1`  | `identity.subscription.created`     | A subscription was created; includes the denormalized `startingBidGrant` so workflow-service can seed the bid wallet without a callback |

### Consumed

| Topic                                                       | Handler                     | Description                                                                                     |
| -------------------------------------------------------------- | ----------------------------- | ---------------------------------------------------------------------------------------------------- |
| `vendor.registered.v1` (configurable: `app.topics.vendor-registration`) | `VendorRegistrationConsumer` | Creates a `VENDOR` UserAccount, a `Membership` on the default tenant with role `vendor`, and a `FREEMIUM` Subscription, then republishes `identity.vendor.identity_created` |

## Configuration

| Variable                  | `application.yml` default                     | docker-compose value                          | Description |
| --------------------------- | ------------------------------------------------ | ------------------------------------------------- | -------------- |
| `PORT`                     | `8090`                                            | `8090`                                             | HTTP port |
| `DB_URL`                   | `jdbc:postgresql://localhost:5432/cerp`           | `jdbc:postgresql://postgres:5432/cerp`             | Postgres JDBC URL |
| `DB_USERNAME`              | `cerp`                                            | `cerp`                                              | Database user |
| `DB_PASSWORD`              | `cerp`                                            | `cerp_dev_password`                                | Database password |
| `DB_POOL_MAX`              | `10`                                              | (unset → `10`)                                     | Hikari max pool size |
| `OIDC_ISSUER_URI`          | `http://localhost:8081/realms/cerp`               | `http://keycloak:8081/realms/cerp`                 | Issuer the OAuth2 resource server uses to validate incoming JWTs |
| `KEYCLOAK_URL`             | `http://localhost:8081`                           | `http://keycloak:8081`                             | Base URL for the Admin REST API (`KeycloakBootstrap`, `KeycloakAdminClient`) |
| `KEYCLOAK_REALM`           | `cerp`                                            | `cerp`                                              | Realm to bootstrap and issue tokens from |
| `KEYCLOAK_CLIENT_ID`       | `cerp-identity-service`                           | `cerp-identity-service`                            | Confidential client used for Direct Access Grant + service-account admin calls |
| `KEYCLOAK_CLIENT_SECRET`   | `cerp-identity-service-dev-secret`                | `cerp-identity-service-dev-secret`                 | Client secret |
| `KEYCLOAK_ADMIN`           | `admin`                                           | `admin`                                             | Master-realm admin username (bootstrap only) |
| `KEYCLOAK_ADMIN_PASSWORD`  | `admin`                                           | `admin`                                             | Master-realm admin password (bootstrap only) |
| `KAFKA_BOOTSTRAP_SERVERS`  | `localhost:9092`                                  | `kafka:9092`                                        | Kafka brokers |
| `APP_EVENTS_ENABLED`       | `false`                                           | `true`                                              | Toggles all Kafka publisher/consumer/topic beans |
| `SPRING_FLYWAY_ENABLED`    | `true`                                            | `true`                                              | Run Flyway migrations on startup |

In `docker-compose.yml`, `identity-service-java` also gets `JAVA_OPTS` wired for a remote debug
agent on port `5007` (exposed alongside `8090`), and depends on `postgres` (healthy) and `keycloak`
(started).

OpenAPI docs are served at `/api-docs` (JSON) and `/swagger` (UI), both public — see
`springdoc-openapi` in `pom.xml` and the `springdoc` block in `application.yml`.

## Build

```bash
mvn clean verify
```

## Run

```bash
mvn spring-boot:run
```

Requires PostgreSQL and Keycloak reachable at the configured URLs (see Configuration above) — the
simplest way to get both locally is `docker compose up -d postgres keycloak`.

## Docker

```bash
docker build -t cerp/identity-service .
docker run -p 8090:8090 \
  -e DB_URL=jdbc:postgresql://postgres:5432/cerp \
  -e OIDC_ISSUER_URI=http://keycloak:8081/realms/cerp \
  -e KEYCLOAK_URL=http://keycloak:8081 \
  -e KEYCLOAK_CLIENT_SECRET=cerp-identity-service-dev-secret \
  cerp/identity-service
```

## Health Check

```bash
# Liveness
curl http://localhost:8090/actuator/health/liveness

# Readiness
curl http://localhost:8090/actuator/health/readiness

# Info
curl http://localhost:8090/actuator/info
```
