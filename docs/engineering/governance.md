# Repository Strategy and Engineering Governance (Section 22)

Purpose

- Align the monorepo structure, ownership, CI/CD, versioning, and guardrails for scalable multi-team development.
- Implement the initial guardrails and workflows described in Section 22 of ConstructHub_Concept_vNext.md.

Scope (v1)

- Monorepo-first using pnpm workspaces and Turborepo: apps/_, packages/_, services/*.
- Path-based ownership and review rules via CODEOWNERS.
- Split CI pipelines for JS (apps/packages) and Java services (services/*) with path filters.
- Automatic PR labeling based on changed paths.
- Changesets for independent package versioning within packages/*.

Repository Layout

- apps/
  - web/ – Web Next.js app (TypeScript)
  - api/ – Placeholder for potential Node API (reserved)
- packages/
  - shared-types/ – Shared types and DTOs
  - core-web/ – Shared web UI code for apps/web
  - eslint-config/, typescript-config/ – Shared tooling configs
- services/
  - core-api-java/, procurement-service-java/, vendor-service-java/, catalog-service-java/, identity-service-java/, partner-system-java/
- infra/
  - k8s base/overlays – Kubernetes manifests (will move to a GitOps repo when split)
- docs/ – Product and architecture documentation (this file included)

Ownership and Access

- CODEOWNERS configured:
  - Default: repository owner as fallback.
  - apps/web/, services/_, packages/_, infra/, docs/ mapped to owner paths.
- Enforce with branch protection rules on main:
  - Require PR reviews from code owners for protected paths.
  - Status checks required (CI jobs).

Versioning and Publishing (packages/*)

- Changesets configured at .changeset/config.json for independent versioning of packages/*.
- Initial mode: access: restricted, versions tagged per package.
- Local development uses workspace: links; consumers should depend on semver ranges for published packages once internal registry is enabled (e.g., GitHub Packages).
- Release workflow (added):
  - Changesets version PR on main via .github/workflows/release.yml; publish not configured yet (internal registry TBD).

CI/CD Overview

- JS Monorepo CI: .github/workflows/ci-js.yml
  - Triggers: changes under apps/**, packages/**, turbo.json, pnpm-lock.yaml, pnpm-workspace.yaml, package.json.
  - Steps: pnpm install (frozen lockfile), lint, typecheck, build, test.
  - Caching: .turbo cache; optional Turborepo Remote Cache via TURBO_TOKEN and TURBO_TEAM.
  - Enabled:
    - Affected-only execution via Turborepo filters with base ref detection (see ci-js.yml).
  - Optional:
    - Add upload-artifact for build outputs if needed by downstream jobs.

- Java Services CI: .github/workflows/ci-java.yml
  - Paths-filter detects changed services and conditionally builds only those.
  - Each service builds with Maven using Temurin JDK 21: mvn -B -ntp verify.
  - Caching: actions/setup-java maven cache enabled.

- PR Labeler: .github/workflows/labeler.yml + .github/labeler.yml
  - Applies labels apps, services, packages, infra, docs, config based on path rules.
  - Useful for triage dashboards and code owner notifications.

Module Boundaries and Linting (Policy)

- Policy: Shared code lives only in packages/*; no deep cross-app/service imports.
- Enforcement (near-term tasks):
  - ESLint rules in shared config:
    - no-restricted-imports to forbid importing across apps/services except through packages/* public API barrels.
    - import/no-internal-modules to prevent deep imports bypassing package public API.
  - Path aliases for packages/* to clarify import boundaries.
  - Optional CI guard: graph-based check using Turborepo pipeline or custom script to assert no forbidden edges.

Remote Caching and Performance

- Turborepo remote caching recommended for CI speed:
  - Set secrets: TURBO_TOKEN and TURBO_TEAM at repository level.
  - Consider a self-hosted or Vercel cache. Current pipeline supports these env vars.
- Continue using actions/cache for local .turbo directory to warm caches between runs.

Split Triggers (When to Move to Multiple Repos)

- Consider splitting when:
  - 3+ independent teams require fully decoupled release cadence and access controls.
  - Security/compliance mandates repo-level isolation.
  - Tooling divergence makes pipelines unwieldy (e.g., heavy Java/Gradle vs Node).
  - CI remains slow despite affected-only builds and remote cache.
  - Distribution/public packages that benefit from standalone repos.

Suggested Split Boundaries (If/When)

- cerp-web: apps/web + packages/core-web
- cerp-api: apps/api + packages/core-api
- cerp-services-core-api-java, cerp-services-partner-system
- cerp-infra: infra/k8s (GitOps repo for Argo/Flux)
- cerp-platform: packages/shared-types, eslint-config, typescript-config
- cerp-examples: examples/demo-overlay

Migration Plan Summary (Incremental)

1. Prepare shared artifacts
   - Introduce Changesets (done).
   - Begin publishing packages/* to internal registry; in consumers, replace workspace:* with ^x.y.z semver ranges.
2. Isolate infra
   - Move infra/k8s to dedicated GitOps repo watched by Argo/Flux.
3. Carve out services/apps
   - Use git subtree split or filter-repo per path; stand up per-repo CI and SBOM/scanning.
4. Harden CI/CD and releases
   - Require status checks; sign images and add provenance; tag releases; independent versioning.
5. Decommission workspace links
   - Contract tests across repos; ensure SDK and event schema compatibility.

Security and Compliance in CI (Roadmap)

- Add SBOM generation and image signing (Cosign) for containerized services.
- SAST/SCA and DAST steps aligned with Section 12.
- Enforce “no secrets in logs” and reduce PII exposure in test fixtures.

Local Developer Guide (Quickstart)

- Prereqs: Node (per .nvmrc), pnpm, Java 21, Docker (optional for local infra).
- JS:
  - pnpm install
  - pnpm lint
  - pnpm typecheck
  - pnpm build
  - pnpm test
- Java (per service):
  - cd services/<service-name>
  - mvn -B -ntp verify
- CI Secrets (optional, for caching):
  - TURBO_TOKEN, TURBO_TEAM for remote Turborepo cache.

Open Items / Next Steps

- Completed:
  - ESLint boundary enforcement (no-restricted-imports, import/no-internal-modules) applied via shared config.
  - JS CI affected-only runs via Turborepo filters with base ref detection.
  - Changesets release workflow to create version PRs (.github/workflows/release.yml); publish step deferred until registry is configured.
- Next:
  - Add exports maps to all packages/* to enforce public barrels at runtime (shared-types updated).
  - Consider a composite action for Java service build/test to remove duplication across services.
  - Move infra/k8s to a GitOps repo and wire Argo/Flux in environments.
  - Add SBOM generation and image signing (Cosign) in CI for Java services.
  - Add CODEOWNERS entries for CI workflows and architecture docs if not already present.
  - Optionally enable Turborepo remote caching with TURBO_TOKEN/TURBO_TEAM.

Changelog

- v1.1: Enabled affected-only JS CI; added Changesets release workflow (version PR); updated shared-types exports; documented completed items.
- v1 (this document): Introduced governance file, CODEOWNERS, JS/Java CI, labeler, and Changesets config; defined next steps and split triggers.
