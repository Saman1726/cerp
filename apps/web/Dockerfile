# syntax=docker/dockerfile:1

FROM node:24-alpine AS base
RUN corepack enable
WORKDIR /app

FROM base AS deps
COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
# Only copy existing workspace package manifests needed by web to maximize Docker cache hits
COPY apps/web/package.json ./apps/web/package.json
COPY packages/core-web/package.json ./packages/core-web/package.json
COPY packages/shared-types/package.json ./packages/shared-types/package.json
COPY packages/typescript-config/package.json ./packages/typescript-config/package.json
COPY packages/eslint-config/package.json ./packages/eslint-config/package.json
# If building the demo overlay web separately, uncomment the next line
# COPY examples/demo-overlay/web/package.json ./examples/demo-overlay/web/package.json
# Ensure musl/glibc compat for native deps like sharp
RUN apk add --no-cache libc6-compat
RUN pnpm install --frozen-lockfile

FROM deps AS build
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm --filter web build

# next.config.ts sets output:'standalone' + outputFileTracingRoot pointed at
# the monorepo root, so standalone nests server.js under the app's workspace
# path (.next/standalone/apps/web/server.js) - expected, not a bug.
FROM node:24-alpine AS runtime
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000
WORKDIR /app
USER node
COPY --chown=node:node --from=build /app/apps/web/.next/standalone ./
COPY --chown=node:node --from=build /app/apps/web/.next/static ./apps/web/.next/static
COPY --chown=node:node --from=build /app/apps/web/public ./apps/web/public

# Rename .env.local to .env.local.disabled so Next.js uses Docker env vars
# instead of host-local values (e.g. localhost URLs that don't work in Docker).
RUN if [ -f apps/web/.env.local ]; then mv apps/web/.env.local apps/web/.env.local.disabled; fi

EXPOSE 3000
CMD ["node", "apps/web/server.js"]
