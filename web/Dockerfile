# Multi-stage: deps → Angular SSR build → pruned prod deps → Node runtime.
# Build context: web/
# Runtime config (API_BASE_URL, ALLOWED_HOSTS) comes from env / compose .env — not build args.

FROM node:22-alpine AS deps
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit --no-fund

FROM deps AS build
COPY . .
RUN --mount=type=cache,target=/app/.angular/cache \
    npx ng build

FROM deps AS prod-deps
RUN npm prune --omit=dev

FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000

COPY package.json package-lock.json ./
COPY --from=prod-deps /app/node_modules ./node_modules
COPY --from=build /app/dist/frontend ./dist/frontend
COPY docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod +x ./docker-entrypoint.sh

USER node
EXPOSE 4000

ENTRYPOINT ["./docker-entrypoint.sh"]
