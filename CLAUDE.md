# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CERP is a multi-tenant ERP SaaS for the Properties & Construction industry. It's a polyglot monorepo with Java (Spring Boot 3.3) backend microservices and a TypeScript (Next.js 16 / React 19) frontend, deployed on Kubernetes.

## Build & Dev Commands

### Prerequisites

Node 24 (.nvmrc), pnpm 11.11.0 (corepack), Java 21 (Temurin), Maven, Docker.

### Common commands (all from repo root)

```bash
pnpm install                          # install JS dependencies
pnpm build                            # build all JS packages (turbo)
pnpm dev                              # dev server for all JS packages
pnpm lint                             # eslint across all JS packages
pnpm typecheck                        # tsc --noEmit across all JS packages
pnpm test                             # run JS tests (turbo)
pnpm format                           # prettier --write
pnpm format:check                     # prettier --check
```

### Scoped commands

```bash
pnpm --filter web run dev             # start only the Next.js app (port 3001)
pnpm --filter @cerp/core-web run build
pnpm turbo run lint --filter=web...   # lint web and its dependency graph
```

### Java services (from individual service directory)

```bash
./mvnw clean verify                   # build + test a single service
./mvnw spring-boot:run                # run locally (needs DB + env vars)
```

### Docker (full stack)

```bash
docker compose up -d postgres keycloak zookeeper kafka   # infra only
docker compose up --build                                # everything
docker compose --profile platform up -d                  # adds Keycloak, Kafka, Zookeeper
```

### Changesets (for package versioning)

```bash
pnpm changeset                        # create a changeset
pnpm version-packages                 # apply changesets, bump versions
```

## Architecture

### Monorepo layout

- **`apps/web`** — Next.js App Router frontend. Thin wrapper that composes components from `@cerp/core-web`. Runs on port 3001.
- **`packages/core-web`** — Shared React components, API clients, and UI building blocks. Uses Radix UI + Tailwind CSS 4. Exports via subpath modules (e.g., `@cerp/core-web/procurement`, `@cerp/core-web/shell`).
- **`packages/shared-types`** — TypeScript types and Zod schemas shared across frontend packages. Exports by domain: `@cerp/shared-types/workflow`, `@cerp/shared-types/auth`, etc.
- **`packages/eslint-config`** — Shared ESLint rules enforcing architectural boundaries.
- **`packages/typescript-config`** — Shared tsconfig bases (`base.json`, `nextjs.json`, `nestjs.json`).
- **`services/`** — Java Spring Boot microservices (see below).
- **`infra/k8s/`** — Kustomize manifests with base + overlays (dev/staging/prod).

### Architectural boundary rule

Apps and services **must not** import from each other. Cross-cutting types go through `packages/*` public APIs only. ESLint enforces this — importing from `apps/*`, `services/*`, `@cerp/*/src/**`, or `@cerp/*/dist/**` is a lint error.

### Backend microservices

All services use Spring Boot 3.3, Java 21, Maven, Flyway migrations, PostgreSQL, and OAuth2 (Keycloak). Each owns its own DB schema within a shared Postgres instance (except supply-chain services which use a separate `supply-postgres`).

| Service | Port | Database | Key responsibility |
|---|---|---|---|
| identity-service-java | 8090 | cerp (identity schema) | Auth, tenants, memberships, Kafka user events |
| workflow-service-java | 8089 | cerp (workflow schema) | Saga/workflow engine, templates, bidding, state machines |
| project-service-java | 8085 (→8095 in compose) | cerp | Projects, sub-projects, construction sites, building levels |
| api-gateway-java | 8080 | — | Spring Cloud Gateway BFF, OIDC, tenant routing |
| vendor-service-java | 8085 | supply_db | Vendor onboarding, categories, favorites |
| catalog-service-java | 8086 | supply_db | Material catalog |
| notification-service-java | 8091 | cerp (notification schema) | Centralized notification jobs, Kafka-driven fan-out to recipients |

### Multi-tenancy

Tenant isolation uses schema-per-tenant in PostgreSQL. The API Gateway extracts the tenant from the JWT and propagates it via headers. Each service uses a `TenantContext` (ThreadLocal) set by a request filter. Flyway migrations run per-schema. Supply-chain services (vendor, catalog, procurement) use RLS policies on a shared schema instead.

### Event-driven communication

Services communicate asynchronously via Kafka. The workflow-service uses an outbox pattern for reliable event publishing. Identity events (user registration, role changes) are published to Kafka topics consumed by downstream services.

### Frontend API pattern

`apps/web` API routes (`app/api/`) act as a BFF proxy layer — they forward authenticated requests to Java backend services. The `@cerp/core-web/proxy` module provides helpers for creating these proxy handlers. Client-side code calls Next.js API routes, not Java services directly.

## Conventions

- **Prettier** config: semicolons, single quotes, trailing commas (all), 100 char line width, 2-space indent.
- **TypeScript** strict mode everywhere. Path alias `@/*` maps to `src/*` in `apps/web`.
- **Java** packages follow `io.cerp.<service-name>` (e.g., `io.cerp.workflow`, `io.cerp.identity`).
- **Flyway** migrations are in `src/main/resources/db/migration/` per service, named `V{N}__description.sql`.
- **Turbo** handles task orchestration — `build`, `dev`, `lint`, `typecheck`, `test` are the defined pipelines. `build` outputs go to `dist/` and `.next/`.
- **Docker images** are published to `ghcr.io/saman1726/*`.

## CI

- **ci-js.yml** — affected-only JS lint/typecheck/build/test via Turbo filter
- **ci-java.yml** — per-service Maven verify, conditionally triggered by path filters
- **web-ci.yml** — web-specific lint/typecheck/build
- **docker-build.yml** — matrix build+push of all service images to GHCR on push to main
