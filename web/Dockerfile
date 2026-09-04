# Multi-stage: Angular SSR build → Node runtime.
# Build context: web/
# Runtime config (API_BASE_URL, ALLOWED_HOSTS) comes from env / compose .env — not build args.

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npx ng build

FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000

COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build /app/dist/frontend ./dist/frontend
COPY docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod +x ./docker-entrypoint.sh

USER node
EXPOSE 4000

ENTRYPOINT ["./docker-entrypoint.sh"]
