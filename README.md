# cerp

ERP SaaS for the Properties & Construction industry. Multi-tenant, built to run in Kubernetes.

This is a first scaffold: a thin, working vertical slice (auth + one example entity, `Property`) proving the multi-tenancy architecture end-to-end. The rest of the ERP domain (contracts, budgets, construction projects, accounting, ...) comes later as additional modules following the same pattern.

## Architecture

- **Monorepo:** pnpm workspaces + Turborepo — `apps/api` (NestJS), `apps/web` (Next.js), `packages/*` (shared TS types, tsconfig, eslint config).
- **Multi-tenancy: schema-per-tenant.** One shared PostgreSQL instance; each tenant gets its own Postgres schema (`tenant_<slug>`). A `platform` schema holds the tenant registry. Tenant resolution and dynamic schema binding live in `apps/api/src/tenancy` — see the comments there for the invariants that keep it safe under connection pooling.
- **Tenant resolution (v1):** login-time tenant slug + `X-Tenant-Id` header + JWT claim (authoritative once authenticated). Subdomain-based routing is a clean v2 upgrade, not built yet.
- **Auth:** JWT access tokens (15 min) + rotating opaque refresh tokens, argon2 password hashing. The Next.js app is a BFF — the browser never calls the API directly; Next's server holds the session and forwards `Authorization`/`X-Tenant-Id` headers server-to-server.
- **ORM:** TypeORM, chosen specifically for its support for per-request dynamic `search_path` binding (see `TenancyService`), which Prisma/Drizzle don't support well for this shape of multi-tenancy.

| Layer | Tech |
|---|---|
| Backend | NestJS + TypeScript, TypeORM, PostgreSQL |
| Frontend | Next.js (App Router) + React |
| Infra | Docker, Kubernetes (Kustomize) |
| CI | GitHub Actions |

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
pnpm --filter web run dev   # http://localhost:3001
```

Or run the whole stack (postgres + api + web, hot-reloading) via:

```bash
docker compose up --build
```

Log in at `/login` with the tenant slug, email and password from the `tenant:provision` step above.

## Repo structure

```
apps/
  api/            NestJS backend
  web/            Next.js frontend
packages/
  shared-types/   DTOs/types shared between api and web
  typescript-config/
  eslint-config/
infra/k8s/        Kustomize base + overlays (dev/staging/prod)
.github/workflows/
```

## Testing & CI

```bash
pnpm turbo run lint typecheck build   # everything, cached/parallelized by Turborepo
pnpm --filter api run test:e2e        # tenancy isolation test - two tenants, asserts no cross-visibility
```

`.github/workflows/ci.yml` runs the same lint/typecheck/build plus the e2e test against a real Postgres service container on every push/PR to `main`. `.github/workflows/docker-build.yml` builds and pushes both images to GHCR on merge to `main`.

## Deployment

Kubernetes manifests are authored with Kustomize under `infra/k8s/` (base + `dev`/`staging`/`prod` overlays) — not yet applied anywhere, since no cluster is configured for this project yet.

```bash
kubectl kustomize infra/k8s/overlays/dev   # render and inspect
kubectl apply -k infra/k8s/overlays/dev    # once a cluster + registry access exist
```

`dev` and `staging` include an in-cluster Postgres `StatefulSet` for convenience. `prod` does not — point it at a managed Postgres instance instead (see `infra/k8s/base/secret.example.yaml`).

## Secrets

Nothing under `infra/k8s` with real credentials is ever committed — `secret.example.yaml` is a template only. Create real secrets out-of-band (`kubectl create secret ...`) or, better, adopt [Bitnami Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) so the encrypted result is safe to commit.

## Conventions

- New ERP modules follow the `apps/api/src/properties` pattern: a tenant-scoped entity (no `tenant_id` column, no schema on the `@Entity()` decorator), accessed only via `TenancyService.getEntityManager()`, never `@InjectRepository`.
- DTOs on the NestJS side `implements` the corresponding interface from `@cerp/shared-types` so frontend/backend drift fails at compile time.
