# Catalog Service

Spring Boot 3 service that owns the **material/product catalog master data** for CERP's supply-chain domain: catalog items (SKU, name, description, extensible attributes, category tags) and their time-versioned prices. Phase 2 foundation service (see `pom.xml` description) — it exposes a working REST API and publishes domain events, but as of this writing no frontend or other Java service calls it yet.

## Purpose

- **What it owns**: the "what" of procurement — materials/products that can be priced, categorized, and referenced by procurement or construction workflows (e.g. `docs/workflows/state-machines/procurement-request.md` validates that "materials [are] in catalog" before a procurement request can proceed). It does **not** own vendors/suppliers, purchase orders, or goods receipts.
- **Why it's separate from `vendor-service-java`**: the two services share the same physical database (`supply_db`) and the same tenant-isolation mechanism (Postgres RLS), but they own different aggregates. `vendor-service-java` owns the "who" (vendor identity, vendor categories, vendor favorites); `catalog-service-java` owns the "what" (items and prices). Splitting them keeps each service's schema and deploy lifecycle independent even though they're colocated for now — each has its own Flyway history table (`flyway_schema_history_catalog` here vs. `vendor`'s own table) precisely so migrations don't collide inside the shared `supply_db` instance.
- **Who consumes it today**: nobody yet, in code. There is no `CATALOG_SERVICE_URL` wired into `apps/web`, and no other Java service calls its REST API. It's reachable only directly via its own endpoints (`/api/catalog/items`, `/api/catalog/prices`). It publishes Kafka events (see below) so that future consumers (a catalog browsing UI, procurement/bidding workflows in `workflow-service-java`) can react without a synchronous dependency being added now.
- This matches CLAUDE.md's one-line description ("Material catalog") — no correction needed there.

## Responsibilities

- Create and partially update catalog items: SKU, name, description, arbitrary JSONB `attributes`, and category tags (`text[]`)
- List/fetch catalog items scoped to the caller's tenant, with optional category filtering
- Record time-versioned prices per item/currency; creating a new price automatically closes out the previously-open price (sets its `valid_to`) so there's a clean price history instead of overwriting
- Resolve and expose each item's "latest" price (active price if one is open, else the most recent by `valid_from`)
- Enforce tenant isolation **at the database layer** via PostgreSQL Row-Level Security, not just application-level `WHERE tenant_id = …` filtering
- Authenticate all API calls as OAuth2/JWT (Keycloak-issued bearer tokens)
- Publish catalog domain events to Kafka on item/price create/update (outbound only — this service does not consume any Kafka topics)
- Own its schema exclusively through Flyway migrations; Hibernate never auto-generates DDL
- Expose OpenAPI/Swagger docs and Actuator health/metrics endpoints

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│ Caller (API Gateway or direct)                                  │
│   Authorization: Bearer <JWT>   X-Tenant-Id: <uuid>              │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌────────────────────────────────────────────────────────────────┐
│ SecurityFilterChain                                              │
│  OAuth2 Resource Server (validates JWT against issuer-uri)       │
│  → TenantRequestFilter (X-Tenant-Id header, else JWT claim       │
│     tenantId/tenant_id/tid) → TenantContext (ThreadLocal)        │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌────────────────────────────────────────────────────────────────┐
│ REST Controllers                                                 │
│  CatalogController (/api/catalog/items)                          │
│  CatalogPriceController (/api/catalog/prices)                    │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌────────────────────────────────────────────────────────────────┐
│ RlsTenantAspect (@Around, AOP, wraps io.cerp.catalog.service..*) │
│  binds TenantContext → SELECT set_config('app.tenant_id', …, true)│
│  inside the active @Transactional, so Postgres RLS policies see  │
│  the tenant for this session/transaction                          │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌────────────────────────────────────────────────────────────────┐
│ CatalogService / CatalogServiceImpl                               │
│  - re-checks tenant ownership in application code (defense in     │
│    depth alongside RLS)                                           │
│  - orchestrates item + price writes                                │
│  - maps entities → response DTOs                                   │
└───────────────┬───────────────────────────────┬───────────────────┘
                │                               │
                ▼                               ▼
┌───────────────────────────────┐   ┌───────────────────────────────┐
│ JPA Repositories                │   │ CatalogEventsPublisher          │
│ CatalogItemRepository            │   │ → Kafka topic supply.catalog.v1 │
│ CatalogPriceRepository           │   │   (catalog.item.created/updated,│
└───────────────┬───────────────────┘   │    catalog.price.created)      │
                │                       └───────────────────────────────┘
                ▼
┌────────────────────────────────────────────────────────────────┐
│ PostgreSQL supply_db, schema "supply"                             │
│  catalog_items, catalog_prices                                    │
│  Row-Level Security policies keyed on app.tenant_id                │
└────────────────────────────────────────────────────────────────┘
```

### Design decisions

**Separate `supply_db` / `supply-postgres` instance.** Unlike identity/project/workflow — which share one Postgres instance (`postgres`/`cerp` database) and isolate tenants with schema-per-tenant — `catalog-service-java` and `vendor-service-java` are pointed at a dedicated Postgres instance (`supply-postgres`, its own container, port 5433 on the host in docker-compose) and database (`supply_db`). This is a deliberate split: the supply-chain domain's data shape (catalog items, prices, vendors) is uniform across tenants and comparatively simple, so it's modeled as flat shared tables with RLS instead of one schema per tenant — schema-per-tenant would mean re-running the same migrations once per tenant schema, which doesn't scale operationally for reference/master data like this. Running it on a separate Postgres instance also isolates the supply-chain domain's operational footprint (backups, scaling, migrations) from the core `cerp` database.

**RLS vs. schema-per-tenant — confirmed, with a caveat.** CLAUDE.md's claim that supply-chain services "use RLS policies on a shared schema instead" is accurate for this service: `V006__catalog_rls_and_policies.sql` runs `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` on both `supply.catalog_items` and `supply.catalog_prices`, with policies keyed on `current_setting('app.tenant_id', true)`. `TenantRequestFilter` + `TenantContext` + `RlsTenantAspect` bind the resolved tenant into the Postgres session via `set_config('app.tenant_id', ..., true)` before each service call. One nuance worth knowing: both RLS policies explicitly allow the row through when `current_setting('app.tenant_id', true) IS NULL` — i.e., RLS here **fails open** if no tenant was bound to the session, rather than denying by default. In practice the service layer always resolves a tenant before touching the repositories (`requireTenant()` throws `IllegalStateException` otherwise) and also re-filters by `tenant_id` in application code, so RLS acts as defense-in-depth rather than the sole enforcement boundary. Also note: CLAUDE.md's sentence lists "vendor, catalog, procurement" as the RLS-based services, but there is no `procurement-service-java` in this repository today — only `catalog-service-java` and `vendor-service-java` exist under `services/`.

**`OAUTH_ISSUER_URI` pointed at `realms/master`, not `realms/cerp`.** In docker-compose, `identity-service-java` (which bootstraps a tenant-facing `cerp` realm in Keycloak on startup, per `KeycloakBootstrap`/`KeycloakAdminClient`) and `project-service-java` are configured against `http://keycloak:8081/realms/cerp`. `catalog-service-java`, `vendor-service-java`, and `workflow-service-java` are instead configured against `http://keycloak:8081/realms/master` — Keycloak's built-in administrative realm, which exists the instant the Keycloak container starts, with no bootstrap step required. `catalog-service-java`'s `depends_on` in docker-compose only waits on `supply-postgres` and `kafka` (not `keycloak` or `identity-service-java`), so pointing at the always-available `master` realm sidesteps a startup-order race against the `cerp` realm being created — Spring resolves OIDC discovery metadata from the issuer URI eagerly at boot. That said, this looks like **unreconciled configuration drift rather than an intentional security decision**: `vendor-service-java` has a `DevJwtDecoder` that bypasses real Keycloak validation entirely when `DEV_MODE=true` (which docker-compose sets for it), making its issuer value largely moot locally — but `catalog-service-java` has no such bypass, so it genuinely validates bearer tokens against `realms/master`. Any real JWT actually minted by identity-service-java against the `cerp` realm carries issuer `.../realms/cerp` and would be **rejected** by catalog-service-java's resource server as-is. The k8s base manifests (`infra/k8s/base/*-deployment.yaml`) compound this: they set `realms/master` uniformly for identity, vendor, and catalog alike, suggesting that layer is an older/less-maintained scaffold that predates the docker-compose `cerp` realm bootstrap. Net: don't assume catalog-service-java is currently wired into real end-user Keycloak auth end-to-end — this is worth reconciling with whoever owns the Keycloak realm setup rather than treated as a deliberate per-service boundary.

**`SPRING_JPA_HIBERNATE_DDL_AUTO=none`.** The code-level default in `application.yml` is `hibernate.ddl-auto: validate` (Hibernate checks the schema matches the entities but never changes it); docker-compose overrides this to `none` (Hibernate doesn't even validate). Either way, Hibernate never creates or alters tables — Flyway (`V005__catalog_initial_schema.sql`, `V006__catalog_rls_and_policies.sql`) is the sole source of schema truth, matching the pattern used by the other supply-chain service, `vendor-service-java`.

## API Endpoints

| Method | Path                      | Description                                                          | Auth |
| ------ | ------------------------- | ---------------------------------------------------------------------| ---- |
| POST   | `/api/catalog/items`      | Create a catalog item (sku, name, description, attributes, categories)| JWT |
| PATCH  | `/api/catalog/items/{id}` | Partially update a catalog item (name/description/attributes/categories)| JWT |
| GET    | `/api/catalog/items/{id}` | Get a catalog item by id (tenant-scoped), including its latest price  | JWT  |
| GET    | `/api/catalog/items`      | List catalog items for the tenant; optional `?category=` filter       | JWT  |
| POST   | `/api/catalog/prices`     | Add a price for an item (currency, amount, optional `validFrom`); closes the prior open price | JWT |

All endpoints require authentication — `SecurityConfig` permits only `/actuator/**` and the OpenAPI/Swagger paths without a token.

### Headers

| Header          | Required | Description                                                        |
| --------------- | -------- | -------------------------------------------------------------------|
| `Authorization` | Yes      | Bearer JWT token                                                    |
| `X-Tenant-Id`   | Yes*     | Tenant UUID; falls back to JWT claim `tenantId`/`tenant_id`/`tid` if absent. If neither resolves, requests fail with 400 ("Tenant id is required"). |
| `Content-Type`  | Yes      | `application/json` (POST/PATCH)                                     |

### API documentation

OpenAPI docs are generated by springdoc and served at `/v3/api-docs`; a browsable UI is at `/swagger-ui.html` (both permitted without auth).

## Data Model

```
supply.catalog_items
├── id (UUID) - Primary key
├── tenant_id (UUID) - Tenant scope; enforced by RLS, not schema-per-tenant
├── sku (TEXT) - Stock-keeping unit
├── name (TEXT)
├── description (TEXT, nullable)
├── attributes (JSONB, nullable) - arbitrary/extensible material attributes
├── categories (TEXT[], default '{}') - tags/categories
├── created_at (TIMESTAMPTZ)
└── updated_at (TIMESTAMPTZ, nullable)

supply.catalog_prices
├── id (UUID) - Primary key
├── item_id (UUID) - FK -> supply.catalog_items(id) ON DELETE CASCADE
├── currency (CHAR(3)) - ISO 4217
├── amount (NUMERIC(19,2))
├── valid_from (TIMESTAMPTZ, default now()) - start of this price's validity
├── valid_to (TIMESTAMPTZ, nullable) - null = currently active price
└── created_at (TIMESTAMPTZ)
```

Indexes: `idx_catalog_items_tenant_id`, `idx_catalog_items_sku`, `idx_catalog_prices_item_id`, `idx_catalog_prices_item_id_valid_from`. Note there is no DB-level uniqueness constraint on `(tenant_id, sku)` despite `CatalogItemRepository.findBySkuAndTenantId` existing — duplicate SKUs per tenant are not currently prevented at the database layer.

RLS policies (`V006__catalog_rls_and_policies.sql`): `catalog_items_tenant_isolation` (direct `tenant_id` check) and `catalog_prices_tenant_isolation` (checks the tenant of the referenced `catalog_items` row, since prices don't carry `tenant_id` directly).

## Kafka Topics

| Topic                | Direction | Description                                                                 |
| --------------------- | --------- | ---------------------------------------------------------------------------|
| `supply.catalog.v1`   | Publish   | Envelope `{ type, timestamp, payload }` for `catalog.item.created`, `catalog.item.updated`, `catalog.price.created` |

This service has no `@KafkaListener` consumers — it only publishes.

## Configuration

| Environment Variable         | Default (`application.yml`)                  | docker-compose value                          | Description                                   |
| ----------------------------- | ---------------------------------------------- | ------------------------------------------------ | ---------------------------------------------|
| `DB_URL`                      | `jdbc:postgresql://localhost:5432/cerp`        | `jdbc:postgresql://supply-postgres:5432/supply_db` | Full JDBC URL (single-var, unlike the split `DB_HOST`/`DB_PORT`/`DB_NAME` used by `project-service-java`) |
| `DB_USER`                     | `cerp`                                          | `${POSTGRES_USER:-cerp}`                          | Database username                             |
| `DB_PASSWORD`                 | `cerp`                                          | `${POSTGRES_PASSWORD:-cerp_dev_password}`         | Database password                             |
| `OAUTH_ISSUER_URI`            | `http://localhost:8080/realms/cerp`            | `http://keycloak:8081/realms/master`              | Keycloak issuer URI for JWT validation (see Architecture note above on the `master` vs `cerp` realm discrepancy) |
| `KAFKA_BOOTSTRAP`             | `localhost:9092`                                | `kafka:9092`                                      | Kafka bootstrap servers                        |
| `SPRING_FLYWAY_ENABLED`       | `true` (set in yml)                             | `"true"`                                          | Enables Flyway migrations on startup          |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`| `validate` (set in yml as `hibernate.ddl-auto`)| `none`                                             | Hibernate never mutates schema; Flyway is sole source of truth |
| `PORT`                        | n/a — `server.port: 8086` is hardcoded in yml  | `8086`                                            | Set in compose but not actually consumed (no `${PORT}` placeholder in `application.yml`); harmless only because it matches the hardcoded value |

## Build

```bash
mvn clean package -DskipTests
```

## Run

```bash
java -jar target/catalog-service-java-0.1.0-SNAPSHOT.jar
```

## Docker

```bash
docker build -t cerp/catalog-service .
docker run -p 8086:8086 \
  -e DB_URL=jdbc:postgresql://supply-postgres:5432/supply_db \
  -e OAUTH_ISSUER_URI=http://keycloak:8081/realms/master \
  -e KAFKA_BOOTSTRAP=kafka:9092 \
  cerp/catalog-service
```

## Health Check

```bash
curl http://localhost:8086/actuator/health
curl http://localhost:8086/actuator/info
```

`management.endpoints.web.exposure.include` also exposes `metrics`, `env`, and `loggers`. Unlike `project-service-java`, this service has no Micrometer Prometheus registry or OpenTelemetry dependency in `pom.xml`, so `/actuator/prometheus` and distributed tracing are not available here.

## Database Migrations

Flyway manages schema changes under `src/main/resources/db/migration/`, tracked in a dedicated history table (`flyway_schema_history_catalog`) to avoid colliding with other services sharing `supply_db`:

- `V005__catalog_initial_schema.sql` — creates the `supply` schema, `catalog_items`, `catalog_prices` tables and their indexes
- `V006__catalog_rls_and_policies.sql` — enables Row-Level Security and tenant-isolation policies on both tables

(Migration numbering starts at `V005` because it shares the `supply` schema/history convention with `vendor-service-java`, whose own migrations occupy earlier numbers in that lineage.)
