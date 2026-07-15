# Plan: Transition from workspace:* to caret (^) semver ranges

This document describes when and how we will migrate internal dependencies from `workspace:*` to published semver ranges (e.g., `^1.2.3`) for the `packages/*` libraries consumed by apps/services.

## Why migrate

- Establish reproducible builds from a clean clone, decoupled from workspace linking.
- Enable independent versioning and changelog generation per package via Changesets.
- Allow multi-repo adoption later without code changes in consumers.

## Current state

- Local development uses `workspace:*` for speed and DX.
- Changesets configured in the repo (`.changeset/config.json`) with root scripts:
  - `pnpm changeset`, `pnpm version-packages`, `pnpm release`.
- CI per deployable exists; remote Turborepo cache is configurable via `TURBO_TOKEN`/`TURBO_TEAM`.

## Readiness criteria

Only migrate to caret ranges once all are true:

1. Package quality and stability

- Packages expose a clear public API via `src/index.ts`.
- ESLint boundary rules prevent deep internal imports.
- TypeScript types are stable and exported.

2. Build and publish readiness

- Changesets versioning tested on a dry-run branch.
- Private registry configured (e.g., GitHub Packages or alternative).
- CI can build packages from a clean checkout without workspace linking.

3. Consumer coverage

- All consumer apps/services build and pass tests against the published package tarballs (smoketest).

## Migration steps

1. Prepare registry

- Choose registry (e.g., GH Packages) and configure scopes:
  - Set `publishConfig` in each publishable package:
    ```json
    {
      "name": "@cerp/<pkg>",
      "version": "0.1.0",
      "publishConfig": { "registry": "https://npm.pkg.github.com" }
    }
    ```
  - Configure auth in CI for publish (GITHUB_TOKEN with appropriate scopes or a PAT).

2. Version packages

- On a feature branch, add changesets for affected packages: `pnpm changeset`.
- Apply versions: `pnpm version-packages` (updates versions and changelogs).
- Verify builds: `pnpm turbo run build --filter=./packages/*`.

3. Publish pre-release (optional)

- Optionally use pre-release tags (`changeset pre enter next`) to validate consumers before GA.
- Publish: `pnpm release` (in CI or locally with auth).

4. Update consumers to caret ranges

- Replace `workspace:*` with `^x.y.z` for internal packages in apps/services:
  - Example before:
    ```json
    "@cerp/core-api": "workspace:*",
    "@cerp/shared-types": "workspace:*"
    ```
  - Example after:
    ```json
    "@cerp/core-api": "^0.1.0",
    "@cerp/shared-types": "^0.1.0"
    ```
- Ensure lockfile updates consistently: `pnpm install`.

5. Validate builds and tests

- For each deployable:
  - `pnpm turbo run lint typecheck build --filter=<deployable>...`
  - Run tests/e2e where applicable.

6. Rollout

- Merge the version bump and consumer updates together (atomic change).
- Keep workspace linking only for local package development flows (optional) in separate feature branches.

## Post-migration practices

- Every user-visible package change requires a changeset; CI publishes on release branches.
- Consumers bump on demand; caret ranges admit non-breaking updates automatically.
- Maintain a simple release cadence (e.g., weekly minors; ad-hoc patches).
- Track breaking changes in CHANGELOG; coordinate major releases with consumers.

## Risk management

- DX regression for local linking: mitigate via `npm pack` + local install testing or `pnpm link --global` for iterative dev on a package.
- Registry outages: keep `workspace:*` fallback branches for emergency dev; pin known-good versions in consumers.
- Synchronization of cross-cutting changes: prefer minor increments with compatibility shims; avoid synchronized majors until necessary.

## Checklist (per migration wave)

- [ ] Choose packages to publish and confirm readiness.
- [ ] Configure `publishConfig` and registry secrets in CI.
- [ ] Add changesets and apply versions.
- [ ] Publish to registry (can be pre-release).
- [ ] Update all consumers to caret ranges.
- [ ] CI green for all deployables.
- [ ] Document version in release notes and link to CHANGELOGs.

## Appendix: Example package.json snippets

Publishable package:

```json
{
  "name": "@cerp/core-api",
  "version": "0.1.0",
  "private": false,
  "publishConfig": {
    "access": "restricted",
    "registry": "https://npm.pkg.github.com"
  },
  "files": ["dist/**"],
  "main": "dist/index.js",
  "types": "dist/index.d.ts",
  "scripts": {
    "build": "tsup src/index.ts --dts"
  }
}
```

Consumer app:

```json
{
  "dependencies": {
    "@cerp/core-api": "^0.1.0",
    "@cerp/shared-types": "^0.1.0"
  }
}
```

Tooling note: keep `workspace:*` until the first successful publish and consumer validation; perform the replacement in a single PR per repo/monorepo wave.
