---
name: pimienta-backend-integration-tests
description: >-
  Pimienta Spring Boot MockMvc integration tests: JWT register/activate/login, AccountTestRequests,
  ApiErrorResponse errorCodes, PagedResponse vs Page JSON, follow-up docs. Use when adding or
  changing *IntegrationTest classes, API smoke tests, or verifying REST controllers in backend/.
---

# Pimienta backend — integration tests and follow-up documentation

## When this skill applies

Use whenever adding or changing **integration tests** for the Pimienta Java backend: new `*IntegrationTest` classes, extending HTTP coverage, or verifying controllers end-to-end with the Spring context.

## Required deliverable: follow-up doc (not optional)

For **each new or materially extended** integration test slice (typically one module or bounded API surface):

1. **Create or update** a markdown file under **`backend/docs/test/`** (or `backend/docs/` if already there) that tracks **non-fatal** findings: potential bugs, inconsistent API behavior, missing validation, incomplete flows, tech debt, or product questions discovered while writing or running tests.
2. Prefer **one file per module or area**, e.g. `backend/docs/test/headquarters-integration-followups.md`.
3. If the file already exists, **append** a dated or clearly titled subsection rather than deleting prior content, unless the user asks to consolidate.
4. **Do not** use the follow-up doc for normal test assertions; it is for **issues worth human triage** (even if tests pass).
5. If nothing notable surfaced, add a short line such as: *“No extra follow-ups from this pass.”*

This is **in addition** to merging passing tests.

## Tech stack and placement

- **JUnit 5**, **Spring Boot Test**, **`@AutoConfigureMockMvc`**, **`MockMvc`**.
- Class: `backend/src/test/java/io/github/alexistrejo11/pimienta/module/<module>/integration/<Name>IntegrationTest.java`
- Annotations: **`@SpringBootTest`**, **`@ActiveProfiles("test")`**, **`@Transactional`** (default; avoid committed data unless documented).
- No Testcontainers / no shared abstract IT base / no `@WithMockUser` — use real JWT like production modules.

## Shared HTTP helpers

- Reuse **`io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests`** for JSON + bearer helpers and register/login payloads.

## Authentication pattern (JWT)

1. **`POST /api/v1/auth/register`** with `AccountTestRequests.validRegisterJson(...)`.
2. Load user via **`UserJpaRepository`**, set **`AccountStatus.ACTIVE`**, **`saveAndFlush`**.
3. **`POST /api/v1/auth/login`** → read **`$.accessToken`**.
4. Send **`Authorization: Bearer <accessToken>`**.

**Status codes:**

- No JWT / invalid auth on secured endpoint → assert **`401 Unauthorized`** (see `PimientaAuthenticationEntryPoint` and existing HQ/inventory/CRM ITs).
- Authenticated but lacking role → **`403 Forbidden`**.
- Do **not** assert 403 for “missing bearer” on normal `/api/v1/**` CRUD.

## Rate limiting in tests

- Profile **`test`**: `pimienta.rate-limiting.enabled=false` in `src/test/resources/application-test.properties`.

## Assertions and wire format

- **Errors:** **`$.errorCode`** (`VALIDATION_FAILED`, `MALFORMED_PAYLOAD`, module codes like `HEADQUARTER_NOT_FOUND`, `ITEM_SKU_ALREADY_EXISTS`).
- **Pagination:** **`PagedResponse`** → `$.items` + `$.metadata`. Raw Spring **`Page`** (legacy HQ list) → `$.content` — do not mix.
- **IDs:** Jayway may return `Integer` or `Long` — use **`Number`** + **`longValue()`**.
- **Exports / multipart:** match existing module ITs (`Content-Disposition`, `MockMultipartFile`).

## Coverage checklist (per module)

- No JWT → **401** on a secured read and a secured write.
- Malformed JSON → **`MALFORMED_PAYLOAD`** (when applicable).
- Validation → **`VALIDATION_FAILED`**.
- Happy path create/list/get/update (or main flow).
- **404** / **409** with real module `errorCode` when stable.
- Capture surprises in the **follow-up doc**.

## Running tests

From **`backend/`**:

```bash
mvn -q -Dtest=<SimpleClassName> test
```

## Relation to other skills

- **`pimienta-backend-conventions`**: layout, `PagedResponse`, ErrorCode, RateLimit.
- **`pimienta-backend-openapi`**: Doc* when documenting new endpoints alongside tests.
- **`pimienta-domain-repository-style`**: domain/persistence quirks → follow-up doc; fix only when in scope.

## Agent workflow summary

1. Read controller(s) + DTOs; note status codes and response wrappers.
2. Add/extend `…/integration/*IntegrationTest.java` with JWT pattern above.
3. Run `mvn -Dtest=…` until green; change production code only for correctness/stability.
4. Create or append the follow-up markdown (even if empty findings).
