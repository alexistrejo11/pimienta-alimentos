#!/bin/sh
set -eu

if [ -z "${API_BASE_URL:-}" ]; then
  echo "error: API_BASE_URL is required (set it in the deploy .env)" >&2
  exit 1
fi

if [ -z "${ALLOWED_HOSTS:-}" ]; then
  echo "error: ALLOWED_HOSTS is required (set it in the deploy .env)" >&2
  exit 1
fi

# Angular SSR reads this natively for Host / SSRF checks.
export NG_ALLOWED_HOSTS="${NG_ALLOWED_HOSTS:-$ALLOWED_HOSTS}"

exec node dist/frontend/server/server.mjs
