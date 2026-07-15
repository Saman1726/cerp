# cerp

ERP SaaS for the Properties & Construction industry. Multi-tenant, built to run in Kubernetes.

Migration note: Backend services now default to Java (Spring Boot). The previous Node.js backends are available under a "legacy" profile for optional local use and manual CI. See docs/java-backend-migration.md for details.

The project is split into two parts:

- **Core** (`packages/core-api`, `packages/core-web`) — the reusable ERP engine: tenancy, auth,
  and one example domain module (`Property`). `apps/api`/`apps/web` are thin reference apps that
  consume core with zero customization.
- **Customer overlay** — a way to run core with per-customer code-level customizations (custom
  validation, extra fields, extra workflows, branding) without forking it.
  `examples/demo-overlay/{api,web}` is a worked example proving every extension point actually
  works, end to end. See [`docs/creating-a-new-overlay.md`](docs/creating-a-new-overlay.md) for
  how to build a real one, and when you'd want a genuinely separate deployment/repo instead.

This is still a first scaffold: `Property` is the only real domain module. The rest of the ERP
domain (contracts, budgets, construction projects, accounting, ...) comes later as additional
core modules following the same pattern.

## Architecture

- **Monorepo:** pnpm workspaces + Turborepo — `apps/{api,web}` (reference apps), `packages/{core-api,core-web,shared-types,typescript-config,eslint-config}`, `examples/demo-overlay/{api,web}` (customization example).
- **Multi-tenancy: schema-per-tenant.** One shared PostgreSQL instance; each tenant gets its own Postgres schema (`tenant_<slug>`). A `platform` schema holds the tenant registry. Tenant resolution and dynamic schema binding live in `packages/core-api/src/tenancy` — see the comments there for the invariants that keep it safe under connection pooling.
- **Tenant resolution (v1):** login-time tenant slug + `X-Tenant-Id` header + JWT claim (authoritative once authenticated, via a guard so it never races `ClsModule`'s own middleware for registration order). Subdomain-based routing is a clean v2 upgrade, not built yet.
- **Auth:** JWT access tokens (15 min) + rotating opaque refresh tokens, argon2 password hashing. The Next.js apps are a BFF — the browser never calls the API directly; Next's server holds the session and forwards `Authorization`/`X-Tenant-Id` headers server-to-server.
- **ORM:** TypeORM, chosen specifically for its support for per-request dynamic `search_path` binding (see `TenancyService`), which Prisma/Drizzle don't support well for this shape of multi-tenancy.
- **Extension points** (how a customer overlay customizes core without forking it): DI-token overrides for business rules (`CorePropertiesModule.forRoot({ validatorClass })`), `@nestjs/event-emitter` hooks for side effects (`property.created`), satellite tables (FK back to a core entity's `id`) for extra fields, a render-prop `extraFields` slot on core's form components, and a `BrandingConfig` passed into core's `<AppShell>`. Full detail in `docs/creating-a-new-overlay.md`.

| Layer    | Tech                                                                  |
| -------- | --------------------------------------------------------------------- |
| Backend  | Java (Spring Boot), PostgreSQL • legacy: NestJS + TypeScript, TypeORM |
| Frontend | Next.js (App Router) + React                                          |
| Infra    | Docker, Kubernetes (Kustomize)                                        |
| CI       | GitHub Actions                                                        |

## Local development

Prerequisites: Node 24 (see `.nvmrc`), Docker.

```bash
corepack enable
cp .env.example .env
docker compose up -d postgres

pnpm install
pnpm --filter api run migrate:platform
pnpm --filter api run tenant:provision -- --slug=demo --name="Demo Tenant" --admin-email=owner@demo.test --admin-password=password123

pnpm --filter api run dev    # http://localhost:3000
pnpm --filter web run dev    # http://localhost:3001
```

Or run the whole stack (postgres + api + web, hot-reloading) via:

```bash
docker compose up --build
```

Optional: platform baseline services (Keycloak, Kafka, Schema Registry, MinIO) for integration testing:

```bash
docker compose --profile platform up -d
```

- Keycloak: http://localhost:8081
- Kafka: localhost:9092 (broker), Zookeeper: 2181
- Schema Registry: http://localhost:8082
- MinIO: http://localhost:9001 (console), http://localhost:9000 (S3)

Log in at `/login` with the tenant slug, email and password from the `tenant:provision` step above.

To also run the customization example (uses the same Postgres instance, different ports):

```bash
cp examples/demo-overlay/api/.env.example examples/demo-overlay/api/.env
pnpm --filter demo-overlay-api run migrate:platform   # only if not already run above - same platform schema
pnpm --filter demo-overlay-api run tenant:provision -- --slug=acme --name="Acme" --admin-email=owner@acme.test --admin-password=password123

pnpm --filter demo-overlay-api run dev   # http://localhost:3010
pnpm --filter demo-overlay-web run dev   # http://localhost:3011
```

## Repo structure

```
apps/
  api/                  Reference NestJS app: CoreModule.forRoot({}), zero overrides
  web/                  Reference Next.js app: composes @cerp/core-web building blocks
packages/
  core-api/             The NestJS engine as a library - tenancy, auth, properties, extension points
  core-web/              The Next.js building blocks as a library - session, api-client, components
  shared-types/          DTOs/types shared between core-api and core-web
  typescript-config/
  eslint-config/
examples/demo-overlay/
  api/                   Worked example: DI-override validator, satellite table, event listener
  web/                   Worked example: extra form field, custom branding
infra/k8s/               Kustomize base + overlays (dev/staging/prod) - the reference apps' own deployment
docs/
  creating-a-new-overlay.md   How to build a real customer overlay, and when to reach for one
  architecture/               Target module structure - DDD bounded contexts + Hexagonal + Hydration/Projection
.github/workflows/
```

## Testing & CI

```bash
pnpm turbo run lint typecheck build         # everything, cached/parallelized by Turborepo
pnpm --filter api run test:e2e              # tenancy isolation - two tenants, asserts no cross-visibility
pnpm --filter demo-overlay-api run test:e2e # isolation again (with the custom validator active) + all 3 extension points
```

CI is split by deployable unit and contracts:

- .github/workflows/api-ci.yml — lint/typecheck/build for API, spins up Postgres, runs platform migration and e2e tests
- .github/workflows/web-ci.yml — lint/typecheck/build for Web
- .github/workflows/partner-system-ci.yml — lint/typecheck/build for Partner System, spins up Postgres, runs migrations and tests
- .github/workflows/contracts.yml — placeholder for OpenAPI/AsyncAPI generation and artifact upload
- .github/workflows/docker-build.yml — builds and pushes API and Web images to GHCR on merge to main

Remote caching for Turborepo in CI can be enabled by setting TURBO_TOKEN (secret) and TURBO_TEAM (org/team slug).

## Deployment

Kubernetes manifests are authored with Kustomize under `infra/k8s/` (base + `dev`/`staging`/`prod` overlays) for the **reference apps** — not yet applied anywhere, since no cluster is configured for this project yet. A real customer overlay with its own deployment target is a separate concern; see `docs/creating-a-new-overlay.md`.

```bash
kubectl kustomize infra/k8s/overlays/dev   # render and inspect
kubectl apply -k infra/k8s/overlays/dev    # once a cluster + registry access exist
```

`dev` and `staging` include an in-cluster Postgres `StatefulSet` for convenience. `prod` does not — point it at a managed Postgres instance instead (see `infra/k8s/base/secret.example.yaml`).

## Secrets

Nothing under `infra/k8s` with real credentials is ever committed — `secret.example.yaml` is a template only. Create real secrets out-of-band (`kubectl create secret ...`) or, better, adopt [Bitnami Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) so the encrypted result is safe to commit.

## Conventions

- New ERP modules follow the `packages/core-api/src/properties` pattern: a tenant-scoped entity (no `tenant_id` column, no schema on the `@Entity()` decorator), accessed only via `TenancyService.getEntityManager()`, never `@InjectRepository`.
- DTOs on the NestJS side `implements` the corresponding interface from `@cerp/shared-types` so frontend/backend drift fails at compile time.
- Customer-specific customization never forks core source — see `docs/creating-a-new-overlay.md` for the extension mechanism to reach for.
