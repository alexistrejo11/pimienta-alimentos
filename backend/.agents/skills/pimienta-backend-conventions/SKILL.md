---
name: pimienta-backend-conventions
description: >-
  General backend conventions for Pimienta Java: hexagonal layout, OpenAPI doc annotations,
  controllers, shared web types. Use alongside pimienta-domain-repository-style.
---

# Pimienta backend conventions

## Module layout (hexagonal)

- **`core/domain`**: aggregates, value objects, domain exceptions—no Spring, no JPA.
- **`core/application`**: ports (`*UseCases`), implementations (`*UseCasesImpl`), **commands**,
  queries, DTOs for application workflows.
- **`core/application/port` / `infrastructure/...`**: inbound adapters (web), outbound adapters
  (JPA, clients)—depend inward on ports and domain.

## Naming

- Use cases: **`ThingManagementUseCases`**, **`ThingBulkSyncUseCases`** with impl classes
  **`ThingManagementUseCasesImpl`**.
- Web adapter for a resource: **`ThingController`** or **`ThingManagerController`**; mapper as
  **`ThingWebMapper`** colocated or under `dto`.
- Persistence: **`ThingJpaEntity`**, **`ThingSpringDataRepository`**, **`ThingPersistenceMapper`**.

## HTTP layer

- Controllers stay **thin**: validate with `@Valid`, map to commands, call use case, map to
  response DTO.
- **Pagination**: extend **`PageableRequest`** for `@ModelAttribute` filters; return
  **`PagedResponse<T>`** with `PagedResponse.map(page, mapper)`.
- **Errors**: use shared **`ApiErrorResponse`**; domain/application exceptions map to HTTP via
  global handlers where configured.

## OpenAPI (Springdoc)

- **Meta-annotations** live under `.../infrastructure/adapter/inbound/web/doc/` (or module CRM
  equivalent): one **`Doc*`** composed annotation per endpoint or per controller tag.
- Each **`Doc*`** method annotation should include where relevant:
  - **`@Operation`** (summary + description),
  - **`@Parameter`** / **`@Parameters`** with **`ParameterIn.PATH`** or **`QUERY`**,
  - **`@RequestBody`** with `schema = @Schema(implementation = …)` and **`@ExampleObject`** for JSON,
  - **`@ApiResponse`** for **200/201/204** with response `schema`,
  - **`@ApiResponse`** for **400** (validation / bad request) and **404** with
    **`ApiErrorResponse`** when applicable.
- **Multipart**: follow **`DocHeadquarterImport`** / **`DocTaskImport`**: `contentType =
  MULTIPART_FORM_DATA`, `schema` with `requiredProperties`, **`@Encoding`** for `file` with Excel
  `contentType`. Headquarters use the same **`Doc*`** style as tasks (`DocHeadquarterCreate`,
  `DocHeadquarterList`, etc.).
- **Controller class**: apply **`@DocTag`**-style annotation (e.g. **`@DocTasks`**) with **`@Tag`**
  for the resource group.

## DTO documentation

- Add **`@Schema`** on request/response records and filter classes: `name`, `description`,
  `example`, `requiredMode`, `type`/`format` for dates.
- Keep **English** descriptions in new API docs unless the module already standardizes on another
  language.

## Security and limits

- Reuse **`@DocJwtSecured`** on documented operations that require a bearer token.
- Apply **`@RateLimit`** on controllers or methods per **`RateLimitProfile`** (e.g. `READ_HEAVY`,
  `SENSITIVE_OPERATIONS`).

## Related skill

- **`pimienta-domain-repository-style`**: domain shape, JPA nullability, validation boundaries.
