# Multi-stage: Angular SSR build → Node runtime.
# Build context: frontend/

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG API_BASE_URL=http://localhost:8080/api/v1
ARG ALLOWED_HOSTS=localhost

# Patch angular.json security.allowedHosts from comma-separated ALLOWED_HOSTS.
RUN node -e "\
const fs=require('fs');\
const hosts=process.argv[1].split(',').map(h=>h.trim()).filter(Boolean);\
const cfg=JSON.parse(fs.readFileSync('angular.json','utf8'));\
cfg.projects.frontend.architect.build.options.security.allowedHosts=hosts;\
fs.writeFileSync('angular.json', JSON.stringify(cfg, null, 2));\
" "${ALLOWED_HOSTS}" \
  && npx ng build --define __API_BASE_URL__="'${API_BASE_URL}'"

FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000

COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build /app/dist/frontend ./dist/frontend

USER node
EXPOSE 4000

CMD ["node", "dist/frontend/server/server.mjs"]
