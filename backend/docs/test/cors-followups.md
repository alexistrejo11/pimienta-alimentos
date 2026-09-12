# CORS — integration test follow-ups

## Notes from initial `CorsIntegrationTest` pass (2026-09-11)

- Production `https://api.pimienta-alimentos.com` returned **403** `Invalid CORS request` for every `Origin` (including `https://pimienta-alimentos.com`). Same request without `Origin` reached login (401 invalid credentials). Cause: `pimienta.cors.allowed-origins: ${PIMIENTA_CORS_ALLOWED_ORIGINS}` bound the comma-separated env value as **one** origin string, so Spring matched none.
- `OriginLists` now splits/trims/strips quotes. Production compose still needs `PIMIENTA_CORS_ALLOWED_ORIGINS` to include `https://pimienta-alimentos.com` (and www if that host is served).
- Cloudflare beacon `ERR_BLOCKED_BY_CLIENT` and `runtime.lastError` on the login page are browser extensions, not API CORS.
