---
name: pimienta-backend-integration-tests
description: >-
  Pimienta Spring Boot backend: how to add MockMvc integration tests (auth, pagination shape,
  exports, validation), and the required follow-up doc for non-fatal findings. Use when the user
  asks for integration tests, ITs for a module, API smoke tests, or testing REST controllers in
  backend/.
---

# Pimienta backend — integration tests and follow-up documentation

## When this skill applies

Use whenever adding or changing **integration tests** for the Pimienta Java backend: new `*IntegrationTest` classes, extending HTTP coverage, or verifying controllers end-to-end with the Spring context.

## Required deliverable: follow-up doc (not optional)

For **each new or materially extended** integration test slice (typically one module or bounded API surface):

1. **Create or update** a markdown file under **`backend/docs/`** that tracks **non-fatal** findings: potential bugs, inconsistent API behavior, missing validation, incomplete flows, tech debt, or product questions discovered while writing or running tests.
2. Prefer **one file per module or area**, e.g. `backend/docs/<area>-integration-followups.md` (examples: `headquarters-integration-followups.md`, `payroll-integration-followups.md`).
3. If the file already exists, **append** a dated or clearly titled subsection (e.g. `## Notes from <feature> tests`) rather than deleting prior content, unless the user asks to consolidate.
4. **Do not** use the follow-up doc for normal test assertions; it is for **issues worth human triage** (even if tests pass).
5. If nothing notable surfaced, add a short line such as: *“No extra follow-ups from this pass.”* so the habit stays explicit.

This is **in addition** to merging passing tests; the doc is the durable checklist for later fixes or product decisions.

## Tech stack and placement

- **JUnit 5**, **Spring Boot Test**, **`@AutoConfigureMockMvc`**, **`MockMvc`**.
- Test class lives under:
  - `backend/src/test/java/io/github/alexistrejo11/pimienta/module/<module>/integration/<Name>IntegrationTest.java`
- Use **`@SpringBootTest`**, **`@ActiveProfiles("test")`**, and **`@Transactional`** on integration test classes unless a test genuinely requires committed data (then document why and avoid that pattern by default).

## Shared HTTP helpers

- Reuse **`io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests`** for JSON POST/GET/PUT/PATCH/DELETE with bearer token and common auth/register/login payloads.
- Keep helpers **public** if tests in other packages need them.

## Authentication pattern (JWT)

Most APIs require an authenticated user:

1. **`POST /api/v1/auth/register`** with `AccountTestRequests.validRegisterJson(email, phone, password)`.
2. Load the user from **`UserJpaRepository`**, set **`AccountStatus.ACTIVE`**, **`saveAndFlush`** (tests mirror production gate for active accounts).
3. **`POST /api/v1/auth/login`** and read **`$.accessToken`** (e.g. Jayway **JsonPath**).
4. Send **`Authorization: Bearer <accessToken>`** on secured requests.

**Security note:** Unauthenticated calls to secured endpoints in this app typically return **403 Forbidden**, not 401, depending on filter configuration—assert the real status.

## Rate limiting in tests

- Integration profile should keep rate limiting from flaking tests, e.g. in **`backend/src/test/resources/application-test.properties`**: `pimienta.rate-limiting.enabled=false` (or project equivalent).

## Assertions and wire format

- **Errors:** Prefer stable **`ApiErrorResponse`** fields, e.g. **`$.errorCode`** (`VALIDATION_FAILED`, `MALFORMED_PAYLOAD`, `INVALID_ARGUMENT`, module-specific codes like `HEADQUARTER_NOT_FOUND`, etc.), not only HTTP status.
- **Pagination:** If the controller returns **`PagedResponse<T>`**, assert **`$.items`** and **`$.metadata`** (see **`PageMetadata`**). If it returns raw Spring **`Page`**, the JSON uses **`content`**—do not mix them up.
- **IDs from JSON:** Jayway may return **`Integer` or `Long`** for numeric ids—read as **`Number`** and use **`longValue()`** to avoid cast failures.
- **Binary responses:** For exports, assert **`Content-Disposition`** (filename substring) and **`Content-Type`** for spreadsheets when applicable.
- **Multipart:** Use **`MockMultipartFile`** + **`multipart(...)`**; empty file often expects **400**—match controller behavior.

## Coverage checklist (per module)

Adapt to the module, but aim for:

- At least one **no JWT → 403** on a secured read and a secured write.
- **Malformed JSON →** handler code (often **`MALFORMED_PAYLOAD`**).
- **Validation failure →** **`VALIDATION_FAILED`** where Jakarta validation applies.
- One **happy path** that exercises create/list/get/update/delete or the module’s main flow.
- **404 / not found** and **invalid path parameter** behaviors if exposed and stable.
- **Edge cases** that document real behavior (e.g. soft-delete vs get-by-id)—and capture surprises in the **follow-up doc**.

## Running tests

From **`backend/`**:

```bash
mvn -q -Dtest=<FullyQualifiedOrSimpleClassName> test
```

Example:

```bash
mvn -q -Dtest=PayrollIntegrationTest test
```

## Relation to other Pimienta skills

- **`pimienta-backend-conventions`**: module layout, controllers, **`PagedResponse`**, errors.
- **`pimienta-backend-openapi`**: when tests are added alongside documented endpoints.
- **`pimienta-domain-repository-style` / `pimienta-domain-model`**: when tests reveal domain or persistence quirks—mention them in the follow-up doc and fix only when in scope.

## Agent workflow summary

1. Read the controller(s) and DTOs for the module; note status codes and response wrappers.
2. Add or extend **`.../integration/*IntegrationTest.java`** using **`AccountTestRequests`** + JWT pattern above.
3. Run **`mvn -Dtest=...`** until green; fix **production code only when required** for correctness or test stability.
4. **Create or append** **`backend/docs/<area>-integration-followups.md`** with bullets for anything questionable, incomplete, or inconsistent—**even if tests pass**.
