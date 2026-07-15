# cerp

ERP SaaS for the Properties & Construction industry. Multi-tenant, built to run in Kubernetes.

Migration note: Backend services now default to Java (Spring Boot). The former Node.js backends have been removed from the repository. See docs/java-backend-migration.md for background and guidance.

The project is split into two parts:

- Core runtime and services (Java + Frontend building blocks)
  - Backend services (Java):
    - services/core-api-java — Core API (Spring Boot, PostgreSQL, Flyway)
    - services/partner-system-java — Partner System (Spring Boot)
  - Frontend libraries and app:
    - packages/core-web — Next.js building blocks (auth/session, API client, UI components)
    - apps/web — Reference Next.js app built from core-web
- Customer overlay
  - examples/demo-overlay/web — Customization example for the web app (branding, extra fields, etc.)

This is still an early scaffold. Additional ERP domains (contracts, budgets, construction projects, accounting, …) will be added iteratively.

## Architecture

- Monorepo: pnpm workspaces + Turborepo
- Backend: Java (Spring Boot), PostgreSQL (schema-per-tenant concept carried forward)
- Frontend: Next.js (App Router) + React
- Infra: Docker, Kubernetes (Kustomize)
- CI: GitHub Actions

## Local development

Prerequisites: Node 24 (see .nvmrc) for the frontend, Docker. Java 21 (Temurin recommended) if you want to build services locally via Maven; otherwise Docker builds images on the fly.

1. Install JS dependencies (frontend and shared packages):

```bash
corepack enable
pnpm install
```

2. Bring up Java backend + Postgres via Docker Compose:

```bash
docker compose up -d postgres partner-system-java api-java
```

Service endpoints:

- Core API (Java)
  - Health: http://localhost:8080/actuator/health
  - OpenAPI (if enabled): http://localhost:8080/api-docs
  - Swagger UI (if enabled): http://localhost:8080/swagger
- Partner System (Java)
  - Health: http://localhost:3020/actuator/health

3. Run the web app:

```bash
pnpm --filter web run dev   # http://localhost:3001
```

Optional: run the demo overlay web app (uses same backend):

```bash
pnpm --filter demo-overlay-web run dev   # http://localhost:3011
```

Notes:

- api-java uses DB_* env vars; defaults are set in docker-compose.yml. PARTNER_SYSTEM_URL defaults to http://partner-system-java:3020.
- The web app talks to api-java via API_URL (see apps/web and packages/core-web/src/lib/env.ts).

## Local test data and login

Quick local test with seeded data:

```bash
corepack enable
pnpm install
docker compose up -d postgres partner-system-java api-java
# Create apps/web/.env.local so the web app points to the local API
printf "API_URL=http://localhost:8080\n" > apps/web/.env.local
pnpm --filter web run dev
# Open http://localhost:3001/login
```

Seeded login accounts (Partner code, Username, Password):

- ACME, john, secret (role: admin)
- ACME, jane, secret (role: user)
- MEGA, mario, secret (role: user)

Core API health endpoint: http://localhost:8080/actuator/health

## Repository structure

```
apps/
  web/                         Reference Next.js app built from @cerp/core-web
examples/demo-overlay/
  web/                         Worked example: extra form field, custom branding
packages/
  core-web/                    Next.js building blocks: session, api-client, components
  shared-types/                DTOs/types shared with the frontend
  typescript-config/
  eslint-config/
services/
  core-api-java/               Spring Boot Core API
  partner-system-java/         Spring Boot Partner System
infra/k8s/                     Kustomize base + overlays (dev/staging/prod)
docs/
.github/workflows/
```

## Testing & CI

Frontend and packages:

```bash
pnpm turbo run lint typecheck build
```

GitHub Actions workflows:

- .github/workflows/api-java-ci.yml — Java Core API CI (build/test)
- .github/workflows/partner-system-java-ci.yml — Java Partner System CI (build/test)
- .github/workflows/web-ci.yml — Frontend CI (lint/typecheck/build)
- .github/workflows/contracts.yml — Placeholder for OpenAPI/AsyncAPI generation and artifact upload
- .github/workflows/docker-build.yml — Builds and pushes Web, Core API (Java), and Partner System (Java) images to GHCR on merge to main

Remote caching for Turborepo in CI can be enabled by setting TURBO_TOKEN (secret) and TURBO_TEAM (org/team slug).

## Deployment

Kubernetes manifests are authored with Kustomize under infra/k8s/ (base + overlays for dev/staging/prod). Reference apps’ deployment is included.

Render and apply an overlay (once your cluster/registry access exist):

```bash
kubectl kustomize infra/k8s/overlays/dev
kubectl apply -k infra/k8s/overlays/dev
```

Ingress routes:

- api.cerp.example.com -> Service api-java:8080
- app.cerp.example.com -> Service web:80

dev and staging include an in-cluster Postgres StatefulSet for convenience. prod does not — point it at a managed Postgres instance instead (see infra/k8s/base/secret.example.yaml).

## Secrets

Nothing under infra/k8s with real credentials is ever committed — secret.example.yaml is a template only. Create real secrets out-of-band (kubectl create secret …) or adopt Bitnami Sealed Secrets so the encrypted result is safe to commit.

## Conventions

- Frontend builds on packages/core-web. Keep server-side calls typed against packages/shared-types where applicable.
- Follow docs/architecture for module boundaries and evolution.
- Java services follow standard Spring Boot layering and should expose health and API docs endpoints for observability and integration.

For migration details and next steps, see docs/java-backend-migration.md.
