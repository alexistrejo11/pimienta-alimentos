# User management — integration test follow-ups

## Notes from initial `UserManagementIntegrationTest` pass (2026-06-04)

- **`/api/v1/users/management/**`** is only protected by **authenticated** in `SecurityConfig` (not `ROLE_ADMIN` / `USERS_*` permissions). Tests use an **ADMIN** operator via JWT, but any active user with a valid token could hit these endpoints today.
- Full lifecycle test exercises: **register** (pending) → **approve** → **login** → management **GET** paths → **roles** → **ban** → blocked login → **unban** → login again → **`GET /api/v1/users/me`** with the target JWT.
- **`GET /by-email`** with invalid `email` query param is validated via **`HandlerMethodValidationException`** → **`INVALID_ARGUMENT`** (not `VALIDATION_FAILED` on the request body). Tests assert the real behavior.
- Prefer **`.param("email", value)`** in MockMvc for `by-email`; encoding `@` into the path manually can double-encode (`%2540`).

## 2026-09-11 — Role hardening

- Default secured API is now **`hasRole("ADMIN")`**. `/api/v1/users/management/**` requires ADMIN; `/api/v1/users/me/**` remains any staff JWT. The earlier “any authenticated user” gap above is closed.
