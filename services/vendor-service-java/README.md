# Vendor Service

Spring Boot 3 service that owns the vendor side of the CERP supply-chain/marketplace domain: vendor onboarding, vendor company profiles, the platform's shared category taxonomy, and per-user vendor favorites.

## Purpose

Vendor Service is the system of record for "who is this vendor, and which tenant/user owns it." It is one of three supply-chain services (alongside `catalog-service-java` and a future procurement service) that run against a dedicated `supply-postgres` instance/`supply_db` database instead of the shared `cerp` database used by identity/workflow/project.

It exists to support two distinct usage patterns:

- **Marketplace directory** — any tenant can browse/search the vendor directory (`GET /vendors`) to find vendors worth inviting to a tender, largely without tenant scoping (see [Architecture](#architecture)).
- **Vendor self-service** — a vendor's own tenant manages its profile, categories, and (per-user) favorites of other vendors/listings.

Vendor Service does not know anything about tenders or bids — that domain belongs entirely to `workflow-service-java`. The relationship is one-directional and synchronous: when `workflow-service-java`'s bidding saga invites vendors to a tender, its `VendorServiceClient` calls back into this service (`GET /vendors/{id}`, forwarding `X-Tenant-Id`) purely to resolve each invited vendor's owning `tenantId`/`userId` so it knows who to notify. See `services/workflow-service-java/src/main/java/io/cerp/workflow/service/VendorServiceClient.java` and `event/VendorsInvitedEventConsumer.java`.

**Consumers:**

- `apps/web` — Next.js BFF proxy routes under `app/api/vendors/**` and `app/api/auth/register-vendor` (directory browsing, profile self-service, favoriting, registration orchestration).
- `workflow-service-java` — synchronous vendor-id → user-id resolution for bidding invitation notifications (`BiddingSagaHandler`, `VendorsInvitedEventConsumer`).
- `identity-service-java` is the other direction: an upstream dependency vendor-service calls into (not a consumer of vendor-service) to resolve a user's UUID from an email JWT subject — see below.

## Responsibilities

- Vendor registration/onboarding (`POST /vendors`) and CRUD.
- Self-service vendor profile read/update, scoped to the caller's own tenant (`/vendors/profile`).
- Admin-style vendor status lifecycle (`pending` → `approved`/`rejected`) via `PATCH /vendors/{id}`.
- Vendor directory/marketplace listing and lookup, largely open (not tenant-scoped) so any tenant can discover vendors.
- Predefined construction category taxonomy (`category_groups` / `category_definitions`), seeded via migration, exposed read-only.
- Per-user vendor favorites (bookmarking vendors across tenants), with Kafka-based propagation of favorite create/remove events.
- Publishing/consuming vendor domain events over Kafka (`supply.vendor.v1`, `vendor.registered.v1`, `identity.vendor.v1`).
- Resolving user identity via `identity-service-java` when a JWT's `sub` claim is an email rather than a UUID.
- Enforcing tenant isolation on vendor-owned tables via PostgreSQL Row-Level Security, driven by a request-scoped `TenantContext`.

## Architecture

```
┌─────────────────────────────┐        ┌────────────────────────────────┐
│   workflow-service-java     │        │      apps/web (Next.js BFF)     │
│  BiddingSagaHandler /        │        │  /api/vendors/**,               │
│  VendorsInvitedEventConsumer │        │  /api/auth/register-vendor      │
└──────────────┬───────────────┘        └────────────────┬─────────────────┘
               │ GET /vendors/{id}                        │ HTTP (JWT / X-Tenant-Id)
               │ (X-Tenant-Id forwarded)                  │
               ▼                                          ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       Vendor Service (port 8085)                       │
│                                                                        │
│  TenantRequestFilter → SecurityConfig (NimbusJwtDecoder, or            │
│  DevJwtDecoder when DEV_MODE=true) → TenantContext (ThreadLocal)       │
│                              │                                         │
│                              ▼                                         │
│  Controllers   VendorController · VendorFavoriteController ·           │
│                CategoryController                                      │
│                              │                                         │
│                              ▼                                         │
│  Services      VendorServiceImpl · VendorFavoriteService ·             │
│                CategoryService                                         │
│                        │            ▲                                  │
│                        │            │  RlsTenantAspect (@Around on     │
│                        │            │  service..*): runs              │
│                        │            │  set_config('app.tenant_id',…)   │
│                        ▼                                               │
│  Repositories  (Spring Data JPA)                                       │
│                              │                                         │
│                              ▼                                         │
│  PostgreSQL   supply_db · schema `supply`                              │
│               RLS policies enforce tenant_id isolation on              │
│               vendors / vendor_documents / vendor_ratings              │
│                                                                        │
│  Kafka   VendorEventsPublisher --publish--> supply.vendor.v1 ---┐       │
│                                                                  ▼       │
│          VendorFavoriteCreatedConsumer <--consume (same topic)--┘       │
│          VendorIdentityCreatedConsumer <-- identity.vendor.v1          │
│                                            (inactive unless            │
│                                             EVENTS_ENABLED=true)       │
└──────────────────────────────┬─────────────────────────────────────────┘
                                │ GET /users?email=<jwt-sub>
                                │ (only when sub isn't already a UUID)
                                ▼
                 ┌───────────────────────────────────┐
                 │    identity-service-java (8090)    │
                 └───────────────────────────────────┘
```

### Design decisions

**Separate `supply_db` / `supply-postgres` instance.** Vendor Service and `catalog-service-java` both run against a dedicated Postgres instance (`supply-postgres`, host port `5433`) and database (`supply_db`) — separate from the shared `cerp` database used by identity/workflow/project. This isolates the marketplace/supply-chain domain from the core tenant-scoped ERP data and lets it evolve/scale independently. Within `supply_db`, Vendor Service owns the `supply` schema.

**RLS, not schema-per-tenant — confirmed.** CLAUDE.md states that supply-chain services use RLS on a shared schema instead of schema-per-tenant, and this is genuinely implemented here, not aspirational. `V002__vendor_rls_and_policies.sql` enables `ROW LEVEL SECURITY` on `supply.vendors`, `supply.vendor_documents`, and `supply.vendor_ratings`, with policies keyed on `tenant_id::text = current_setting('app.tenant_id', true)`. `RlsTenantAspect` is a Spring AOP `@Around` advice wrapping every public `io.cerp.vendor.service..*` method; it runs `select set_config('app.tenant_id', :tid, true)` using the tenant id bound into `TenantContext` (a `ThreadLocal` populated per-request by `TenantRequestFilter` from `X-Tenant-Id` or a JWT tenant claim), so the RLS policies see the right session variable. A few important nuances:

- **Fail-open by design.** Every policy reads `current_setting('app.tenant_id', true) IS NULL OR tenant_id::text = current_setting(...)` — if no tenant id was ever bound for the session/transaction, *all* rows pass. `TenantRequestFilter` excludes the exact path `/vendors` from tenant-context extraction, and `SecurityConfig` marks `GET /vendors/**` as `permitAll`, so listing/searching vendors intentionally runs with no tenant filter — this is what makes the cross-tenant marketplace directory possible, not a bug.
- **Not everything is RLS-scoped.** `vendor_favorites` (V007) and the category tables (`category_groups`, `category_definitions`, `vendor_categories`, V004) have no RLS policy at all — favorites are an explicitly cross-tenant per-user concept, and categories are shared platform-wide reference data, not tenant-owned.
- **Today's registration flow funnels everyone into one shared tenant.** The only live registration path (`apps/web/app/api/auth/register-vendor/route.ts`) assigns every self-registered vendor to the same seeded `DEFAULT_TENANT_ID` (`00000000-0000-0000-0000-000000000001`); a private tenant is a separate, manual admin action not yet built. So in practice RLS today mostly separates "the vendor pool" from other tenants' data rather than isolating individual vendor companies from each other.

**`DEV_MODE` gates JWT signature verification, not RLS.** `DevJwtDecoder` (`config/DevJwtDecoder.java`) is only registered `@ConditionalOnProperty(name = "DEV_MODE", havingValue = "true")`. When active it becomes the `@Primary` `JwtDecoder`, manually parsing unsigned (`alg: none`) tokens shaped like the ones identity-service mints locally (`sub`, `roles` csv, `exp`, with a 5-minute expiry grace period) — it does **not** verify any signature. This replaces `SecurityConfig.jwtDecoderProduction`, which otherwise validates real Keycloak-signed RS256 tokens via `NimbusJwtDecoder` against the issuer's JWKS endpoint. docker-compose sets `DEV_MODE: "true"` for the local stack; it must stay unset/false anywhere real Keycloak tokens are expected.

**Why calls to identity-service-java.** Some JWTs presented to this service carry an email in the `sub` claim rather than a UUID (this is exactly what `DevJwtDecoder` produces, and appears true of identity-service-issued tokens generally). The favorites feature needs a real user UUID to key `vendor_favorites.user_id`. `VendorFavoriteController.extractUserId()` tries `UUID.fromString(sub)` first, and only on failure calls `IdentityServiceClient.resolveUserIdByEmail(sub)` — `GET {IDENTITY_SERVICE_URL}/users?email=...` — which is confirmed to exist and return a matching user record on identity-service's `UserController`. This is the only outbound synchronous call this service makes.

**The event-driven registration flow exists in code but is dormant by default.** `VendorServiceImpl`'s javadoc describes a saga: `register(email, password, …)` saves a vendor with no tenant/user yet, publishes `vendor.registered.v1`, `identity-service`'s `VendorRegistrationConsumer` creates the tenant/user/membership/subscription and publishes `identity.vendor.v1` back, and this service's `VendorIdentityCreatedConsumer` calls `linkIdentity()` to complete the link. In this repo, however:
  - `VendorController.create()` — the only thing wired to `POST /vendors` — always calls the *other* `register(...)` overload, the one that takes `tenantId`/`userId` directly and saves synchronously with **no** Kafka publish. The email/password-only overload that publishes `vendor.registered.v1` is exercised only by `VendorServiceImplTest`, not by any live HTTP path.
  - The actual live flow is orchestrated by the web BFF (`apps/web/app/api/auth/register-vendor/route.ts`): it creates the user, membership, and subscription in `identity-service-java` *first*, then calls `POST /vendors` with `tenantId`/`userId` already resolved.
  - Even if something did publish `vendor.registered.v1`, this service's own reply listener (`VendorIdentityCreatedConsumer`) is `@ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")`, i.e. gated by `EVENTS_ENABLED` (default `false`), which docker-compose does **not** set for `vendor-service-java` (it *is* set — `APP_EVENTS_ENABLED: "true"` — on `identity-service-java`, so identity's half is live; vendor-service's half isn't, by default).

  Treat the saga as legacy/aspirational unless you deliberately re-wire the controller and set `EVENTS_ENABLED=true` here.

**Schema drift worth knowing about.** The `Vendor` entity (`entity/Vendor.java`) declares `user_id`, `contact_name`, `phone`, `website`, `address`, and `country` columns on `supply.vendors`. None of these appear in any committed Flyway migration (`V001`/`V003` only create `id, tenant_id, legal_name, status, tax_profile, categories, created_at, updated_at, city`). With `ddl-auto: validate` (the `application.yml` default) this would fail schema validation at boot unless the columns already exist in the target database by some other means; docker-compose sidesteps this by setting `SPRING_JPA_HIBERNATE_DDL_AUTO: none`, which skips validation but not the need for the columns to actually exist for these fields to work at runtime. Verify the live `supply_db` schema before assuming parity with the entity. Separately, `V004`'s `supply.vendor_categories` join table (meant to FK-validate vendor categories against `category_definitions`) is not referenced by any repository or entity — `Vendor.categories` is a free-form `text[]` column with no DB-level validation against the taxonomy. `supply.vendor_documents`, `supply.vendor_ratings`, and the `supply.vendor_directory` view (all from `V001`) similarly exist in the schema with RLS already applied, but have no corresponding JPA entity/controller/service yet — schema headroom for compliance docs and ratings that isn't exposed via the API in this codebase.

## API Endpoints

| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| POST | `/vendors` | Register/create a vendor record | Public |
| GET | `/vendors` | List vendors (optional `?status=`) — cross-tenant marketplace directory† | Public† |
| GET | `/vendors/{id}` | Get a vendor by ID | Public |
| GET | `/vendors/tenant/{tenantId}` | Get the vendor owned by a given tenant | Public |
| PATCH | `/vendors/{id}` | Update vendor status (`pending`/`approved`/`rejected`) | JWT |
| GET | `/vendors/profile` | Get the caller's own vendor profile (tenant resolved from `X-Tenant-Id`/JWT claim) | Public route‡ |
| POST | `/vendors/profile` | Same fetch-or-empty logic as GET (legacy alias) | JWT |
| PATCH | `/vendors/profile` | Create/update the caller's own vendor profile | JWT‡ |
| GET | `/vendors/categories` | List all active categories, with group info | Public |
| GET | `/vendors/categories/group/{groupId}` | List active categories in a group | Public |
| GET | `/vendors/my-vendors` | Favorited vendors (full details) for the current user | JWT§ |
| GET | `/vendors/my-vendor-ids` | Favorited vendor IDs for the current user | JWT§ |
| POST | `/vendors/{vendorId}/favorite` | Favorite a vendor | JWT |
| DELETE | `/vendors/{vendorId}/favorite` | Unfavorite a vendor | JWT |

† `SecurityConfig` marks `GET /vendors/**` `permitAll`, and `TenantRequestFilter` excludes the exact path `/vendors` from tenant-context extraction — so listing runs with no bound tenant, and (via the RLS fail-open clause) returns vendors across all tenants. This is intentional marketplace behavior, not tenant-scoped by design.
‡ Matched by the public `GET /vendors/**` rule at the Spring Security layer, but the controller itself returns `401` if it can't resolve a tenant id from `X-Tenant-Id` or a JWT claim — public to the filter chain, not to unauthenticated callers in practice. The `POST`/`PATCH` variants aren't covered by any `permitAll` rule and require a valid JWT.
§ Also matched by `GET /vendors/**` `permitAll`, but the handler requires a `JwtAuthenticationToken` argument and throws if no bearer token was presented — a JWT is required in practice even though the filter chain doesn't enforce it.

### Headers

| Header | Required | Description |
| --- | --- | --- |
| `Authorization` | For JWT-gated routes | Bearer JWT (Keycloak RS256, or unsigned dev token when `DEV_MODE=true`) |
| `X-Tenant-Id` | For tenant-scoped routes | Tenant context; resolved into `TenantContext` and bound to `app.tenant_id` for RLS |
| `Content-Type` | Yes | `application/json` on POST/PATCH |

## Data Model

```
supply.vendors  (V001, +city in V003; RLS-enabled in V002)
├── id (UUID) - Primary key
├── tenant_id (UUID) - Owning tenant; RLS partition key
├── legal_name (TEXT) - Company legal name
├── status (TEXT) - pending | approved | rejected (free text, not a DB enum)
├── tax_profile (JSONB) - Arbitrary tax profile / tax IDs
├── categories (TEXT[]) - Free-form category codes (not FK-validated, see below)
├── city (TEXT) - Added in V003, trigram-indexed
├── created_at, updated_at (TIMESTAMPTZ)
└── [entity-only, no migration found] user_id, contact_name, phone, website, address, country
    — declared on the Vendor JPA entity; verify against the live DB (see Architecture)

supply.vendor_documents  (V001; RLS-enabled in V002)         — no JPA entity/controller yet
├── id, vendor_id (FK -> vendors, cascade delete)
├── doc_type, doc_ref, status, expires_at, created_at

supply.vendor_ratings  (V001; RLS-enabled in V002)           — no JPA entity/controller yet
├── id, vendor_id (FK -> vendors, cascade delete)
├── score (1-5), rationale, created_at

supply.vendor_directory  (VIEW, V001)                        — not queried by any repository yet
└── tenant_id, id, legal_name, status, categories, rating_avg, created_at, updated_at

supply.category_groups  (V004)                               — no RLS (shared reference data)
├── id (UUID), name, description, display_order, is_active, created_at, updated_at

supply.category_definitions  (V004)                          — no RLS (shared reference data)
├── id (UUID), group_id (FK -> category_groups), code (unique w/ is_active), name,
│   description, icon (lucide-react name), display_order, is_active, created_at, updated_at
└── seeded with ~25 construction trade/material/equipment/service categories

supply.vendor_categories  (V004)                              — unused by application code
└── vendor_id + category_id (composite PK); intended to FK-validate vendor categories,
    but Vendor.categories is a plain text[] with no reference to this table today

supply.vendor_favorites  (V007)                               — no RLS (cross-tenant by design)
├── id (UUID), user_id (UUID), vendor_id (FK -> vendors, cascade delete)
├── created_at
└── UNIQUE (user_id, vendor_id)
```

Flyway history table is `flyway_schema_history_vendor` (a service-specific name, since multiple services share `supply_db`). Migration numbering jumps from `V004` to `V007`; `V005`/`V006` were never present in this repo's history.

## Kafka Topics

| Topic | Config key | Direction | Description |
| --- | --- | --- | --- |
| `supply.vendor.v1` | `app.topics.vendor` | Publish + Consume (self) | `VendorEventsPublisher` emits favorite created/removed events as `{type, timestamp, payload}` envelopes; `VendorFavoriteCreatedConsumer` subscribes to the same topic and idempotently persists/removes the favorite (existence-checked), enabling other future consumers to react to favorite changes. |
| `vendor.registered.v1` | `app.topics.vendor-registration` | Publish (dormant) | Would carry new-vendor email/password/profile data for `identity-service-java`'s `VendorRegistrationConsumer` to provision identity. Not published by any live REST path today — see Architecture. |
| `identity.vendor.v1` | `app.topics.vendor-identity` | Consume (inactive by default) | `VendorIdentityCreatedConsumer` links a vendor to its newly created `tenantId`/`userId`. Gated by `app.events.enabled` (`EVENTS_ENABLED`), default `false`, not set for this service in docker-compose. |

Consumer group id: `vendor-service` (`app.kafka.consumer.group-id`).

## Configuration

| Environment Variable | Default (`application.yml`) | docker-compose value | Description |
| --- | --- | --- | --- |
| `PORT` / `server.port` | `8085` | `8085` | HTTP port. Host and container ports both stay `8085` in docker-compose — unlike `project-service-java`, which is remapped to host `8095` to avoid colliding with this service. |
| `DB_URL` | `jdbc:postgresql://supply-postgres:5432/supply_db` | same | JDBC URL — the dedicated `supply-postgres` instance/`supply_db` database, not the shared `cerp` DB. |
| `DB_USER` | `cerp` | `${POSTGRES_USER:-cerp}` | Database username |
| `DB_PASSWORD` | `cerp_dev_password` | `${POSTGRES_PASSWORD:-cerp_dev_password}` | Database password |
| `OAUTH_ISSUER_URI` | `http://localhost:8080/realms/cerp` | `http://keycloak:8081/realms/master` | Keycloak issuer used for JWKS-based RS256 validation — only takes effect when `DEV_MODE` is not `"true"` |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | `kafka:9092` | Kafka bootstrap servers |
| `SPRING_FLYWAY_ENABLED` | `true` | `"true"` | Enable Flyway migrations |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | `none` | Hibernate schema check mode; compose disables validation entirely (see schema-drift note above) |
| `DEV_MODE` | unset (falsy) | `"true"` | Activates `DevJwtDecoder`, accepting unsigned dev JWTs instead of verifying Keycloak-signed RS256 tokens. Never enable outside local/dev. |
| `JWT_SECRET` | `dev` | not set | Read by `DevJwtDecoder` but not actually used to verify anything — the dev decoder trusts any well-formed unsigned token regardless of this value. |
| `IDENTITY_SERVICE_URL` | `http://localhost:8080` | `http://identity-service-java:8090` | Base URL for `IdentityServiceClient`'s `GET /users?email=` lookup |
| `EVENTS_ENABLED` | `false` | not set (→ `false`) | Gates `VendorIdentityCreatedConsumer` (`identity.vendor.v1`) |

## Build

```bash
mvn clean package -DskipTests
```

## Run

```bash
java -jar target/vendor-service-java-0.1.0-SNAPSHOT.jar
```

## Docker

```bash
docker build -t cerp/vendor-service .
docker run -p 8085:8085 \
  -e DB_URL=jdbc:postgresql://supply-postgres:5432/supply_db \
  -e OAUTH_ISSUER_URI=http://keycloak:8081/realms/master \
  -e KAFKA_BOOTSTRAP=kafka:9092 \
  -e IDENTITY_SERVICE_URL=http://identity-service-java:8090 \
  cerp/vendor-service
```

Or via the monorepo's docker-compose (starts `supply-postgres` and `kafka` as dependencies automatically):

```bash
docker compose up --build vendor-service-java
```

## Health Check

```bash
# Liveness / readiness / info (Spring Boot Actuator)
curl http://localhost:8085/actuator/health
curl http://localhost:8085/actuator/info

# OpenAPI / Swagger UI
curl http://localhost:8085/v3/api-docs
open http://localhost:8085/swagger-ui.html
```

Exposed actuator endpoints: `health`, `info`, `metrics`, `env`, `loggers` (`management.endpoints.web.exposure.include`).
