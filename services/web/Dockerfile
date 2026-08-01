# syntax=docker/dockerfile:1

FROM node:24-alpine AS base
RUN corepack enable && corepack prepare pnpm@11.11.0 --activate
WORKDIR /workspace

FROM base AS deps
# Install musl/glibc compat for native deps like sharp
RUN apk add --no-cache libc6-compat
# Copy workspace config and root package.json
COPY pnpm-lock.yaml pnpm-workspace.yaml package.json turbo.json ./
# Copy all workspace package manifests
COPY apps/web/package.json ./apps/web/package.json
COPY packages/core-web/package.json ./packages/core-web/package.json
COPY packages/shared-types/package.json ./packages/shared-types/package.json
COPY packages/typescript-config/package.json ./packages/typescript-config/package.json
COPY packages/eslint-config/package.json ./packages/eslint-config/package.json
RUN corepack install && pnpm install --frozen-lockfile

FROM deps AS build
ENV NEXT_TELEMETRY_DISABLED=1
COPY . .
RUN pnpm --filter web build

# Development stage: runs Next.js dev server with hot-reload
FROM deps AS dev
ENV NODE_ENV=development NEXT_TELEMETRY_DISABLED=1 PORT=3001
# Copy all source code for dev server
COPY . .
EXPOSE 3001
CMD ["pnpm", "--filter", "web", "run", "dev"]

# Production runtime stage
FROM node:24-alpine AS runtime
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3001
WORKDIR /workspace
USER node

# Copy standalone output
COPY --chown=node:node --from=deps /workspace/node_modules ./node_modules
COPY --chown=node:node --from=build /workspace/apps/web/.next/standalone ./
COPY --chown=node:node --from=build /workspace/apps/web/.next/static ./apps/web/.next/static
COPY --chown=node:node --from=build /workspace/apps/web/public ./apps/web/public

EXPOSE 3001
CMD ["node", "apps/web/server.js"]
