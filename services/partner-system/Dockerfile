# syntax=docker/dockerfile:1

FROM node:24-alpine AS base
RUN corepack enable
WORKDIR /app

# ---- deps: install once, cached by lockfile + manifests. All workspace
# manifests are copied (not just this service's) since pnpm needs the full
# workspace shape to reconcile against the lockfile. ----
FROM base AS deps
COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY apps/api/package.json ./apps/api/package.json
COPY apps/web/package.json ./apps/web/package.json
COPY packages/core-api/package.json ./packages/core-api/package.json
COPY packages/core-web/package.json ./packages/core-web/package.json
COPY packages/shared-types/package.json ./packages/shared-types/package.json
COPY packages/typescript-config/package.json ./packages/typescript-config/package.json
COPY packages/eslint-config/package.json ./packages/eslint-config/package.json
COPY examples/demo-overlay/api/package.json ./examples/demo-overlay/api/package.json
COPY examples/demo-overlay/web/package.json ./examples/demo-overlay/web/package.json
COPY services/partner-system/package.json ./services/partner-system/package.json
RUN pnpm install --frozen-lockfile

# ---- build: compile, then produce a pruned, symlink-free deploy dir ----
FROM deps AS build
COPY . .
RUN pnpm --filter partner-system build
# --legacy: pnpm v10+ deploy defaults to requiring workspace dependencies to
# be "injected" (copied) rather than symlinked; --legacy restores the deploy
# behavior that works with this workspace's normal symlinked node_modules.
RUN pnpm deploy --filter=partner-system --prod --legacy /app/deploy/partner-system

# ---- runtime: minimal, non-root ----
FROM node:24-alpine AS runtime
ENV NODE_ENV=production
WORKDIR /app
USER node
COPY --chown=node:node --from=build /app/deploy/partner-system ./
EXPOSE 3020
CMD ["node", "dist/main.js"]
