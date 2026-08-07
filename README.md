# cerp

ERP SaaS for the Properties & Construction industry. Multi-tenant, built to run in Kubernetes.

Migration note: Backend services now default to Java (Spring Boot). The former Node.js backends have been removed from the repository.

The project is split into two parts:

- Core runtime and services (Java + Frontend building blocks)
  - Frontend libraries and app:
    - packages/core-web — Next.js building blocks (auth/session, API client, UI components)
    - apps/web — Reference Next.js app built from core-web
- Overlay example (optional; not included in workspace builds)
  - examples/demo-overlay/web — Demonstration only; excluded from pnpm workspace and CI by default.

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
docker compose up -d postgres identity-service-java
```

Service endpoints:

- Identity Service (Java)
  - Health: http://localhost:8090/actuator/health

3. Run the web app:

```bash
pnpm --filter web run dev   # http://localhost:3001
```

Notes:

- The web app talks to identity-service-java via NEXT_PUBLIC_IDENTITY_SERVICE_URL for auth (see apps/web and packages/core-web/src/lib/env.ts).

## Local test data and login

Quick local test with seeded data:

```bash
corepack enable
pnpm install
docker compose up -d postgres identity-service-java
# Create apps/web/.env.local so the web app points to the local identity service
printf "NEXT_PUBLIC_IDENTITY_SERVICE_URL=http://localhost:8090\n" > apps/web/.env.local
pnpm --filter web run dev
# Open http://localhost:3001/login
```

Seeded login accounts (Partner code, Username, Password):

- ACME, john, secret (role: admin)
- ACME, jane, secret (role: user)
- MEGA, mario, secret (role: user)

Identity service health endpoint: http://localhost:8090/actuator/health

## Repository structure

```
apps/
  web/                         Reference Next.js app built from @cerp/core-web
examples/demo-overlay/         Optional example; excluded from workspace builds
  web/                         Worked example only; not built/deployed by default
packages/
  core-web/                    Next.js building blocks: session, api-client, components
  shared-types/                DTOs/types shared with the frontend
  typescript-config/
  eslint-config/
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

- .github/workflows/web-ci.yml — Frontend CI (lint/typecheck/build)
- .github/workflows/contracts.yml — Placeholder for OpenAPI/AsyncAPI generation and artifact upload
- .github/workflows/docker-build.yml — Builds and pushes Web and backend service images to GHCR on merge to main

Remote caching for Turborepo in CI can be enabled by setting TURBO_TOKEN (secret) and TURBO_TEAM (org/team slug).

## Deployment

Kubernetes manifests are authored with Kustomize under infra/k8s/ (base + overlays for dev/staging/prod). Reference apps’ deployment is included.

Render and apply an overlay (once your cluster/registry access exist):

```bash
kubectl kustomize infra/k8s/overlays/dev
kubectl apply -k infra/k8s/overlays/dev
```

Ingress routes:

- app.cerp.example.com -> Service web:80

dev and staging include an in-cluster Postgres StatefulSet for convenience. prod does not — point it at a managed Postgres instance instead (see infra/k8s/base/secret.example.yaml).

## Secrets

Nothing under infra/k8s with real credentials is ever committed — secret.example.yaml is a template only. Create real secrets out-of-band (kubectl create secret …) or adopt Bitnami Sealed Secrets so the encrypted result is safe to commit.

## Conventions

- Frontend builds on packages/core-web. Keep server-side calls typed against packages/shared-types where applicable.
- Follow docs/architecture for module boundaries and evolution.
- Java services follow standard Spring Boot layering and should expose health and API docs endpoints for observability and integration.
