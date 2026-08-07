# Contributing to CERP

This document defines how to work in this monorepo: ownership, boundaries, versioning, CI, and local environment.

## Repository structure

- apps/
  - web/ — Next.js reference app (wraps Core Web)
- packages/
  - core-web/, shared-types/, typescript-config/, eslint-config/
- services/
  - api-gateway-java/ (Spring Cloud Gateway – API Gateway / BFF)
- examples/
  - demo-overlay/{web} (customer overlay example)
- infra/k8s/ — Kustomize base + overlays

See README.md for details.

## Ownership

- Ownership is enforced via .github/CODEOWNERS.
- Teams:
  - @team-web — apps/web
  - @team-platform — shared packages, examples, .github
- Fallback:
  - /* and /*/** default to @team-platform

Use CODEOWNERS for approvals and review routing.

## Architectural boundaries

- Apps and services must not import from one another. Only import from published package public APIs.
- Packages should export a stable public surface from src/index.ts.
- Enforced by shared ESLint config (@cerp/eslint-config):
  - import/no-internal-modules with allowance only for index entry points
  - no-restricted-imports blocking apps/* and services/* cross-imports
  - Import resolver configured for TS project references

Run:

- pnpm turbo run lint
- pnpm turbo run typecheck

## Branching, commits, PRs

- Create feature branches from main; open draft PRs early.
- Keep PRs scoped and testable. Include e2e coverage when touching tenancy or auth.
- CI must be green:
  - Web CI: .github/workflows/web-ci.yml
  - Contracts generation: .github/workflows/contracts.yml

## Versioning and releases

We use Changesets to manage versions for publishable packages (packages/*). The repository itself is private; publish steps are prepared for later.

- Add a changeset for any user-visible change to a package:
  - pnpm changeset
  - Pick affected packages and bump type (patch/minor/major)
- Versioning:
  - pnpm version-packages (applies changesets; bumps versions and changelogs)
- Publishing:
  - pnpm release (publishes; requires registry/auth when enabled)

Current state:

- Internal packages use workspace:* for speed while core stabilizes.

Planned migration to semver ranges:

- Move workspace:* to ^x.y.z once modules stabilize (see docs/semver-transition.md for plan, criteria, and rollout).

## CI and Turborepo cache

- turbo.json is configured with globalEnv: TURBO_TOKEN, TURBO_TEAM
- To enable remote caching in CI, set:
  - secrets.TURBO_TOKEN
  - TURBO_TEAM=cerp (or your team slug)
- Workflows use turbo filters to scope work to changed graph:
  - web: --filter=web...

## Local development

Baseline (Postgres + Java API + Web):

1. Prerequisites

- Node 24 (see .nvmrc), Docker

2. Startup

- corepack enable
- docker compose up -d postgres api-gateway-java
- pnpm install
- pnpm --filter web run dev # http://localhost:3001

Whole stack with Docker (hot reload):

- docker compose up --build

### Platform baseline services (optional)

A local platform profile includes Keycloak, Kafka, Schema Registry, and MinIO for integration testing.

Enable:

- docker compose --profile platform up -d

Services:

- Keycloak: http://localhost:8081
- Kafka: localhost:9092 (broker), Zookeeper: 2181
- Schema Registry: http://localhost:8082
- MinIO: http://localhost:9001 (console), http://localhost:9000 (S3)
  - default: minioadmin/minioadmin (override via env in docker-compose.yml)

To stop:

- docker compose --profile platform down

## Testing

- Unit/integration:
  - pnpm turbo run test

CI spins up Postgres service containers and runs migrations before tests.

## Contracts (OpenAPI/AsyncAPI)

- Placeholder workflow emits a dummy openapi.json
- Workflow: .github/workflows/contracts.yml (uploads artifact)

## Coding standards

- TypeScript strict mode across packages; prefer DTOs implementing shared-types interfaces.
- No direct repository injections for tenant-scoped entities; use TenancyService.getEntityManager().
- Format with Prettier:
  - pnpm format
  - pnpm format:check

## Runbooks

- New ERP module (Java): follow services/project-service-java patterns (controllers, DTOs, services, configuration); expose REST endpoints and update OpenAPI generation accordingly
- Adding a new package:
  - Create under packages/
  - Add tsconfig that extends packages/typescript-config
  - Export its API from src/index.ts
  - Add eslint and build scripts; wire into turbo tasks
- Ownership: add paths to CODEOWNERS
