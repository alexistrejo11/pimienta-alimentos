---
name: pimienta-backend-conventions
description: >-
  Pimienta Spring Boot backend conventions: hexagonal packages (core/port, adapters),
  Flyway migrations, ErrorCode exceptions, /api/v1 controllers, RateLimit, PagedResponse,
  shared vs module. Use when adding or changing Java modules, REST endpoints, migrations,
  or POS backend work under backend/.
---

# Pimienta backend conventions

Read this **before** writing new backend code. Match the module you edit; prefer **inventory / headquarter / task** layout for new work.

## Before coding (checklist)

1. Ports live in **`core/port/input`** and **`core/port/output`** — not under `core/application`.
2. Adapters: match the module (`adapter/` **or** `infrastructure/adapter/`). **New modules:** prefer `infrastructure/adapter/inbound|outbound` (like inventory).
3. Next Flyway version = max existing `V###` + 1 under `src/main/resources/db/migration/`.
4. New API errors: add `ErrorCode` + module `*Exception` extending shared base — do not invent ad-hoc JSON.
5. Controllers: `@RequestMapping("/api/v1/…")`, thin, `@Valid` → command → use case → DTO.
6. POS Device/Admin API: follow `docs/v2/pos_integration/` + tracker `09-implementation-tracker.md` (do not invent richer DDD).

## Module layout (hexagonal)

```text
module/<context>/
  core/domain/          # aggregates, enums, domain exceptions — no Spring, no JPA
  core/application/     # *UseCasesImpl, command/, query/, workflow
  core/port/input/      # use-case interfaces (*UseCases)
  core/port/output/     # persistence/gateway ports
  infrastructure/adapter/inbound/web/   # controllers, dto, doc/, mapper
  infrastructure/adapter/outbound/persistence/  # *JpaEntity, SpringData, PersistenceMapper
```

Older modules may use `adapter/` instead of `infrastructure/adapter/`, or `outbound` vs `out` vs `output`. **Copy the folder names of the module you are editing.**

- **`shared/`**: only cross-cutting (`BaseDomain`, exceptions, pagination, OpenAPI meta, rate limit, S3). Do **not** put module-specific types in `shared/`.

## Naming

- Use cases: **`ThingManagementUseCases`** / **`ThingManagementUseCasesImpl`** (also `*BulkSyncUseCases`).
- New writes: prefer **`*Command`** under `core/application/command` (avoid new domain `*Params`; legacy CRM/inventory may still use `*Params` or nested command records — do not spread that).
- Web: **`ThingController`**, **`ThingWebMapper`**, DTOs with `@Schema`.
- Persistence: **`ThingJpaEntity`**, **`ThingSpringDataRepository`**, **`ThingPersistenceMapper`**.
- Persistence mappers: prefer **`blankToNull`** / `*OrNull` for optional strings/enums when writing JPA (task/contract/headquarter style). Some inventory mappers skip this — follow the gold style on new code.

## HTTP layer

- Controllers stay **thin**: `@Valid` → map to command → use case → response DTO.
- Class-level `@RateLimit(profile = STANDARD)`; GETs **`READ_HEAVY`**; creates/updates/deletes/imports **`SENSITIVE_OPERATIONS`**; auth login/register **`STRICT`**; session refresh **`AUTH_SESSION`**.
- **Pagination**: `@ModelAttribute` filters extending **`PageableRequest`**; return **`PagedResponse<T>`** via `PagedResponse.map(page, mapper)`. Do not return raw Spring `Page` on new endpoints (headquarter list is legacy `$.content`).
- **URL**: `/api/v1/<resource>` (plural). Nested trees OK (`/api/v1/inventory/items`, `/api/v1/headquarters/{id}/pos-settings`). Staff admin under existing resources or `/api/v1/pos/admin/**` per POS specs — **no** duplicated CRUD under `/api/v1/admin`.

## Flyway

- Path: `src/main/resources/db/migration/V{n}__snake_case_name.sql`.
- Enums in DB: **`VARCHAR` + `ALTER … ADD CONSTRAINT ck_* CHECK (col IN ('A','B'))`**. Do **not** use PostgreSQL `CREATE TYPE`.
- Soft delete + auditing columns: follow existing tables (`created_at`, `updated_at`, `deleted_at`, `version`).
- Partial unique indexes (`WHERE deleted_at IS NULL`) when matching inventory-style uniqueness.

## Exceptions and errors

1. Add constant to **`shared/exception/ErrorCode.java`** (`THING_NOT_FOUND`, `THING_ALREADY_EXISTS`, …).
2. Module exception under `core/domain/exception/`:
   - not found → extend **`ResourceNotFoundException`**
   - conflict → extend **`ConflictException`**
   - pass `ErrorCode`, client message, `Map` context, log details (see `HeadquarterNotFoundException`, `ItemSkuConflictException`).
3. **`GlobalExceptionHandler`** maps to **`ApiErrorResponse`** — do not add one-off handlers per controller.

## Security

- Default: `/api/v1/**` requires staff JWT with **`ADMIN`** (`anyRequest().hasRole("ADMIN")`).
- Exceptions (edit **`SecurityConfig`** only for these):
  - **Public**: auth, health, swagger, POS device enroll/refresh/APK.
      - **`/api/v1/users/me/**`** and **`POST /api/v1/telemetry/web/events`**: any staff JWT (`staffJwtOnly`).
  - **`/api/v1/pos/admin/**`** and HQ **`pos-settings` / `pos-catalog`**: `ADMIN` or `MANAGER` (HQ scope via `HeadquarterAccessService`).
  - **Device** `/api/v1/pos/**`: `SCOPE_pos:sync`.
- Unauthenticated secured call → **401**; authenticated without permission → **403**.
- Client-facing error **`message`** is Spanish (`ClientErrorMessages` + `i18n/messages_es.properties`). Domain / logs stay English. Wire `errorCode` and enum names stay English.

## OpenAPI

- Per-endpoint **`Doc*`** meta-annotations under `…/web/doc/`. No `io.swagger.v3.oas.annotations` imports in controller sources.
- Details: skill **`pimienta-backend-openapi`**.

## Related skills

- **`pimienta-domain-repository-style`** — SafeBuilder, nullability, validation boundaries (canonical; Spanish skill is alias).
- **`pimienta-backend-openapi`** — Doc* / springdoc.
- **`pimienta-backend-integration-tests`** — MockMvc ITs + follow-up docs.
