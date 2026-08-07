# API Gateway / BFF

Spring Cloud Gateway-based API Gateway and Backend-for-Frontend for CERP.

## Purpose

`api-gateway-java` exists to give CERP's Java backend a single, centralized edge instead of exposing every microservice's host:port directly to clients. Concretely, per what its code actually implements:

- **Centralized authentication** — `SecurityConfig` configures the service as an OAuth2 resource server that validates every incoming JWT against the Keycloak issuer once, at the edge, instead of each backend service independently verifying tokens.
- **Centralized, declarative authorization** — the same `SecurityConfig` maps path prefixes to required authorities (e.g. `/api/vendors/**` → `vendor:manage`/`vendor:read`, `/api/reports/**` → `reports:read`), so authorization policy lives in one place rather than scattered across services.
- **Tenant header injection** — `TenantContextFilter` resolves the tenant (from an `X-Tenant-Id` header or a JWT claim, falling back to a seeded default tenant) and stamps `X-Tenant-Id` onto every downstream request, so backend services can enforce tenant isolation by trusting a header instead of each re-implementing JWT-claim parsing.
- **Correlation/tracing** — `CorrelationIdFilter` guarantees every request carries an `X-Correlation-Id`/`X-Request-Id` end-to-end.
- **Edge protection** — `RateLimiterConfig` applies a per-client-IP `RequestRateLimiter` before traffic reaches any backend service.
- **Routing indirection** — `GatewayRouteConfig` maps stable public path prefixes (`/api/identity/**`, `/api/projects/**`, …) to whichever internal host:port each service runs on, so clients don't need to know (or track changes to) individual service addresses.

This is the standard BFF/API-gateway rationale: push cross-cutting concerns (authn, authz, tenant scoping, rate limiting, correlation IDs, CORS) to one edge component so domain services behind it stay focused on business logic. **See "Current status" below** for how much of this is actually wired into the live request path today — it's less than the rest of this document might imply.

## Architecture

### Designed request flow

```
Client (browser)
      │  HTTPS, Authorization: Bearer <JWT>
      ▼
api-gateway-java  (Spring Cloud Gateway, :8080)
      │  1. SecurityConfig       – validate JWT against the Keycloak issuer,
      │                            enforce per-route required authorities
      │  2. TenantContextFilter  – resolve tenant (header or JWT claim,
      │                            else a seeded default) → set X-Tenant-Id
      │  3. CorrelationIdFilter  – set X-Correlation-Id / X-Request-Id
      │  4. RateLimiterConfig    – per-client-IP RequestRateLimiter
      │  5. GatewayRouteConfig   – route by path prefix (stripPrefix(1))
      ▼
identity-service-java / project-service-java / workflow-service-java /
vendor-service-java / catalog-service-java
      (each trusts the forwarded X-Tenant-Id + Authorization headers
       instead of revalidating JWTs or re-resolving tenant itself)
```

### Actual current request flow

```
Client (browser)
      │  HTTPS
      ▼
apps/web (Next.js)  – Server Components/Actions + app/api/** route
                       handlers are the de facto BFF today
      │  plain HTTP, one fetch() per downstream call; target resolved
      │  from a per-service *_SERVICE_URL env var, defaulting to
      │  docker-compose DNS names (e.g. http://workflow-service-java:8089)
      ▼
identity-service-java / project-service-java / workflow-service-java /
vendor-service-java / catalog-service-java / notification-service-java
      (called directly — api-gateway-java is not part of this hop)
```

### Current status

This service is **not currently in the live request path**, in either local dev or as the Kubernetes manifests are actually wired today. Verified directly against the code (not just inferred):

**Local dev / `docker-compose.yml`**
- `api-gateway-java` has no entry in `docker-compose.yml`, unlike every other backend service (identity, catalog, vendor, project, workflow, notification).
- `packages/core-web/src/lib/env.ts` hardcodes direct per-service defaults — `http://workflow-service-java:8089`, `http://project-service-java:8085`, `http://vendor-service-java:8085`, `http://identity-service-java:8090` — i.e. docker-compose service DNS names, not a gateway URL.
- Every handler under `apps/web/app/api/**` (workflows, sagas, bidding, open-tenders, vendors, auth/register-\*, notifications, projects) `fetch()`es its target service's `*_SERVICE_URL` directly.
- `packages/core-web/src/lib/api-client.ts` says so explicitly in its own source comment: *"the API gateway currently does not proxy project endpoints"* — and routes both workflow and project calls straight past this service.
- The `@cerp/core-web/proxy` module (`packages/core-web/src/proxy/create-auth-proxy.ts`) referenced in the root `CLAUDE.md` is Next.js middleware for session/JWT-refresh gating — it does not proxy to this gateway either.
- Net effect: **`apps/web` is the de facto BFF today**, calling each Java service directly — matching CLAUDE.md's "Frontend API pattern" section, which describes this exact direct-call behavior. This gateway is not yet part of that picture.

**Kubernetes**
- `infra/k8s/base/kustomization.yaml` does include `api-gateway-java-deployment.yaml` as a resource, but `infra/k8s/base/ingress.yaml` routes 100% of external traffic (`path: /`) straight to the `web` Service — there is no ingress rule targeting `api-gateway` at all.
- The `web` Deployment's only ConfigMap (`web-config`) sets just `NODE_ENV`/`PORT`; no backend or gateway URL is injected, so `web` pods fall back to the same docker-compose-style hostnames above, which don't resolve in-cluster (`infra/k8s/base` has no Service manifests for `workflow-service-java`, `project-service-java`, or `notification-service-java` at all — only identity, catalog, vendor, web, and this gateway have Service objects).
- `api-gateway-java-deployment.yaml` itself references `configMapKeyRef: cerp-config` and `secretKeyRef: cerp-secrets`, neither of which is defined anywhere under `infra/k8s/` (only `api-config`, `web-config`, `api-secrets`, and `postgres-secrets` exist) — applying this manifest as committed would fail to start on the missing, non-optional `cerp-secrets` key.
- It also references container image `cerp/api-gateway:latest`, but `.github/workflows/docker-build.yml`'s publish matrix only builds/pushes `web`, `identity-service-java`, `catalog-service-java`, `vendor-service-java`, `project-service-java`, and `workflow-service-java` — no image is currently published for this service.

**CI**
- `.github/workflows/ci-java.yml` does build and `mvn verify` `services/api-gateway-java` on changes to it (job `build-gateway`), so the module at least compiles and its (currently nonexistent) tests would run — that's the extent of today's automated coverage.

**Bottom line:** treat this service as scaffolded and partially wired into the k8s base manifests, not as a live hop. If/when it's activated, the ingress, `web-config`, the gateway's ConfigMap/Secret names, and the image-publishing matrix all need to be updated together, or requests will 404 or the pod will fail to start.

## Responsibilities (Phase 1)

- Single ingress for all clients
- OIDC authentication via Keycloak
- JWT validation
- Tenant scoping and header propagation
- Route mapping to backend services
- Basic rate limiting
- Correlation ID injection

## Route Mapping

Defined in `GatewayRouteConfig.java` (all routes apply `stripPrefix(1)`):

| Path Prefix                                                     | Backend Service               | Default URL (env var)                            |
| ---------------------------------------------------------------- | ------------------------------ | ------------------------------------------------- |
| `/api/identity/**`, `/api/user/**`                                | Identity Service                | `http://localhost:8083` (`CERP_IDENTITY_SERVICE_URL`) |
| `/api/projects/**`                                                | Project Service                 | `http://localhost:8085` (`CERP_PROJECT_SERVICE_URL`) |
| `/api/documents/**`                                               | Document Management Service     | `http://localhost:8086` (`CERP_DMS_SERVICE_URL`) |
| `/api/reports/**`                                                 | Projections/Reporting Service   | `http://localhost:8087` (`CERP_PROJECTIONS_SERVICE_URL`) |
| `/api/audit/**`                                                   | Audit/Event Catalog Service     | `http://localhost:8088` (`CERP_AUDIT_SERVICE_URL`) |
| `/api/vendors/**`                                                 | Vendor Service                  | `http://localhost:9092` (`CERP_VENDOR_SERVICE_URL`) |
| `/api/catalog/**`                                                 | Catalog Service                 | `http://localhost:9093` (`CERP_CATALOG_SERVICE_URL`) |
| `/api/v1/**`, `/api/workflows/**`, `/api/workflow-templates/**`    | Workflow Service                | `http://localhost:8089` (`CERP_WORKFLOW_SERVICE_URL`) |

The workflow route additionally applies `rewritePath` (`/v1/(?<segment>.*)` → `/api/v1/${segment}`) after stripping `/api`, so `workflow-service-java` still receives its expected `/api/v1/...` path.

> **Discrepancy note (found verifying this table against the code):** `SecurityConfig` also defines authorization rules for `/api/orgs/**` (requires `org:manage`) and `/api/procurement/**` (requires `procurement:manage`), but `GatewayRouteConfig` has no route for either prefix — such a request is authorized (or rejected) by Spring Security and then 404s, since no `Route` bean matches it. More broadly, only `identity`, `project`, `vendor`, `catalog`, `workflow`, and `notification` services actually exist under `services/` in this repo today; an "Organization Service," "Document Management Service," "Projections/Reporting," and "Audit/Event Catalog" (all listed above per the gateway's own route config and Javadoc) do not exist yet, and `notification-service-java` — which does exist and is called directly by `apps/web` — has no route here at all.

## Configuration

Read from `application.yml` (`cerp.gateway.*` and `cerp.services.*`):

| Environment Variable        | Default                                                                            | Description                                                                                     |
| ---------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| `KEYCLOAK_ISSUER_URI`        | `http://localhost:8081/realms/cerp`                                                   | Keycloak issuer URI (OAuth2 resource-server JWT validation)                                        |
| `KEYCLOAK_CLIENT_ID`         | `cerp-gateway`                                                                        | Declared but not currently read by any code path — no OAuth2 client registration is configured (see "Known gaps" below) |
| `KEYCLOAK_CLIENT_SECRET`     | _(empty)_                                                                             | Same as above — declared, not currently consumed                                                   |
| `CORS_ALLOWED_ORIGINS`       | `http://localhost:3000,http://localhost:3001,https://localhost:3000,https://localhost:3001` | Allowed CORS origins for the `SecurityConfig` `CorsWebFilter` bean (see "Known gaps" re: a second, separate CORS config) |
| `CERP_IDENTITY_SERVICE_URL`  | `http://localhost:8083`                                                               | Identity Service URL                                                                                |
| `CERP_PROJECT_SERVICE_URL`   | `http://localhost:8085`                                                               | Project Service URL                                                                                 |
| `CERP_DMS_SERVICE_URL`       | `http://localhost:8086`                                                               | DMS Service URL — no such service currently exists under `services/`                                |
| `CERP_PROJECTIONS_SERVICE_URL` | `http://localhost:8087`                                                             | Projections Service URL — no such service currently exists under `services/`                        |
| `CERP_AUDIT_SERVICE_URL`     | `http://localhost:8088`                                                               | Audit Service URL — no such service currently exists under `services/`                              |
| `CERP_VENDOR_SERVICE_URL`    | `http://localhost:9092`                                                               | Vendor Service URL                                                                                   |
| `CERP_CATALOG_SERVICE_URL`   | `http://localhost:9093`                                                               | Catalog Service URL                                                                                  |
| `CERP_WORKFLOW_SERVICE_URL`  | `http://localhost:8089`                                                               | Workflow Service URL                                                                                 |

**Known gaps in this module** (found while verifying this README against the code; not yet fixed):

- `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET` and the `spring-boot-starter-oauth2-client` dependency are present but unused — no `spring.security.oauth2.client.registration.*` is configured anywhere, so only the OAuth2 **resource-server** (JWT validation) path is actually active.
- Two independent CORS configurations exist: `SecurityConfig.corsConfigurationSource()` (respects `CORS_ALLOWED_ORIGINS`) and Spring Cloud Gateway's own `spring.cloud.gateway.global-cors` block in `application.yml` (hardcoded to the same localhost origins, ignoring that env var). They happen to agree today but can silently drift.
- `application.yml`'s default rate-limit filter is configured for `redis-rate-limiter.*`, but neither `pom.xml` nor `docker-compose.yml` declares a Redis client or service — worth confirming this actually starts correctly wherever it's run.
- `TenantContextFilter`'s `cerp.gateway.tenant.jwt-claim` property is bound but never read; the JWT-claim extraction logic hardcodes its own claim-name list (`tenant_id`, `organization_id`, `tenantId`, `orgId`) instead.

## Build

```bash
mvn clean package -DskipTests
```

## Run

```bash
java -jar target/api-gateway-java-0.1.0-SNAPSHOT.jar
```

## Docker

```bash
docker build -t cerp/api-gateway .
docker run -p 8080:8080 \
  -e KEYCLOAK_ISSUER_URI=http://keycloak:8081/realms/cerp \
  cerp/api-gateway
```

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Observability

- Prometheus metrics at `/actuator/prometheus`
- OpenTelemetry auto-instrumentation supported
- Correlation ID in request/response headers: `X-Correlation-Id`
- Tenant ID propagated via `X-Tenant-Id` header
