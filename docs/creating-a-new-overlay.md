# Creating a new customer overlay

`cerp` is split into a reusable **core** (`packages/core-api`, `packages/core-web`) and thin
**reference apps** (`apps/api`, `apps/web`) that consume it with zero customization.
`examples/demo-overlay/{api,web}` is a second, parallel pair of apps that consumes the same
core packages *with* customizations — it's the template to copy.

## Today: same repo, `workspace:*`

Not every customer needs an overlay. A customer happy on the shared multi-tenant SaaS deployment
is just a row in `platform.tenants` — nothing to build. Reach for an overlay only when a customer
needs either genuinely separate infrastructure (see "Later" below) or real code-level
customization (this section).

1. **Copy `examples/demo-overlay/api` and `examples/demo-overlay/web`** as your starting point,
   renaming the packages (`package.json` `name`) and picking free ports.
2. Add the new paths to the root `pnpm-workspace.yaml` `packages:` list (or generalize the
   existing `examples/demo-overlay/*` glob if you're literally sitting next to the demo).
3. Pick the extension mechanism that fits what you need to change:

   | Need | Mechanism | Example |
   |---|---|---|
   | Override/veto a rule (validation, pricing, authorization) | DI token + `forRoot({ xClass })` | `CorePropertiesModule.forRoot({ validatorClass: DemoPropertyValidator })` |
   | React to something that happened, without changing the outcome | `@nestjs/event-emitter`, `@OnEvent(...)` | `DemoApprovalListener` reacting to `property.created` |
   | Add a field to a core entity | A satellite table with a FK back to the core row's `id`, migrated and entity-mapped by your overlay, reached only via `TenancyService.getEntityManager()` | `DemoPropertyExtension` |
   | Add a whole new workflow/module | An ordinary Nest module in your app — `TenancyService`/`@CurrentUser()`/auth enforcement are all already global, no extra imports needed | `DemoApprovalListener`'s module |
   | Extra field on a form | Core's form component's `extraFields` slot (render prop) | `<PropertyForm extraFields={<input name="inspectorNotes" />} />` |
   | Branding/theming | A `BrandingConfig` passed into core's `<AppShell>` | `branding.config.ts` |

   Do **not**: fork a core file and patch it, or add a `tenant_id`/customer-conditional branch
   inside core's own source. If neither mechanism above fits what you need, that's a sign core
   is missing a real extension point — add one there instead of routing around it.

4. **Wire your `app.module.ts`** the same shape as `examples/demo-overlay/api/src/app.module.ts`:
   `CoreModule.forRoot({ database: { extraEntities: [...] }, properties: { validatorClass } })`
   plus your own additional modules.
5. **Migrations**: `@cerp/core-api` ships entity classes (the domain model) but **no migrations at
   all** — DDL is entirely your overlay's to own from day one. Copy
   `examples/demo-overlay/api/src/database/migrations/` (which itself copied the reference app's
   core-table migrations, since it hasn't diverged from them) as your starting point, then add
   your own on top with a real, later timestamp. Your `data-source.tenant-template.ts` passes your
   complete set via `buildTenantDataSourceOptions({ schemaName, extraEntities, migrations })` — see
   the demo's version. This means upgrading your `@cerp/core-api` version never silently changes
   your database: if a new core release adds a column to an entity, running your app against your
   existing tables surfaces that immediately as a real query/constraint error (a missing column,
   a `NOT NULL` violation) rather than an unreviewed migration having already run against your
   data — you write and review the matching migration yourself, on your own schedule.
6. **Frontend**: never export a `'use server'` function from `@cerp/core-web` (or your own
   shared code) — Server Actions don't reliably cross a package boundary. Core exports plain
   async functions; your app's own thin `'use server'` action wraps them (see
   `examples/demo-overlay/web/lib/demo-property-logic.ts` + `app/(app)/properties/actions.ts`).
7. **Prove it**: reuse `registerTenancyIsolationTests` from `@cerp/core-api/testing` against your
   own `AppModule` (pass `buildTenantDataSourceOptions` if you added extraMigrations — otherwise
   its own event-triggered side effects can fail for schemas provisioned without them) plus your
   own tests for whatever you added. Don't skip this — it's what catches an extension point that
   looks right but breaks tenant isolation.

## Later: a real separate repo, when a real customer needs it

This is deliberately **not** built yet — building it now would mean guessing at requirements no
real customer has stated. When the time comes:

- **Repo**: a genuinely separate git repo per customer overlay (not a folder in this repo),
  depending on `@cerp/core-api` / `@cerp/core-web` / `@cerp/shared-types` as installed
  (semver-ranged) dependencies, not `workspace:*`.
- **Publishing**: GitHub Packages (reuses the `GITHUB_TOKEN` / `packages: write` pattern this repo
  already uses for GHCR images — no new registry to stand up). Version all three packages in
  lockstep (Changesets' `fixed` group), not independently — with three packages that always ship
  together, independent semver just creates a "is core-api@2.1 compatible with core-web@1.9?"
  question nobody should have to answer.
- **GitHub Packages requires the `@cerp` scope to match the owning account.** This repo is
  currently under the personal `Saman1726` account — publishing under `@cerp` means creating a
  `cerp` GitHub org and moving the repo there (or renaming the scope). Don't do this preemptively;
  it's a real, slightly disruptive step, worth doing deliberately when it's actually needed (and
  a natural point to stop being a single-account bus-factor-of-one anyway).
- **`shared-types` (and the two core packages) need a real `dist/` build** at that point — raw
  source consumption via `main: ./src/index.ts` (today's setup) only works within one pnpm
  workspace's symlinks.
- **Deployment**: switch overlay repos specifically to Helm (keep this repo's own Kustomize for
  its own dev/staging/prod). Per-customer infra variation (shared in-cluster Postgres vs.
  bring-your-own on-prem DB, different hostnames/sizing) is overwhelmingly a *values* difference,
  which Helm's chart + values-file model fits better than Kustomize's overlay-directory model;
  Helm's conditional subcharts map directly onto "customer A needs our Postgres, customer B
  doesn't." Each overlay repo owns its own chart — don't publish one shared chart from core until
  2-3 real customer charts exist to generalize from.
- **Each overlay repo needs its own Dockerfiles/images.** Code-level customization means the
  actual composed `app.module.ts` has to already be compiled into the image — there's no
  deploy-time way to layer that in externally.
- **Don't build a `create-cerp-overlay` scaffolding tool** until 2+ real overlay repos exist.
  This doc is the interim version of that tool.
