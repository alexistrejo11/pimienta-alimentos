---
name: pimienta-backend-openapi
description: >-
  Pimienta Alimentos Spring Boot API: OpenAPI 3 (springdoc), per-endpoint Doc* annotations,
  shared DocJwtSecured / DocPublicEndpoint, StandardErrorResponses, global OpenAPI config,
  Swagger UI security. Apply when adding or editing REST controllers or API docs in backend/.
---

# Pimienta backend — OpenAPI & controller documentation

## Dependencies

- Use **springdoc-openapi** for Spring Boot 4: `org.springdoc:springdoc-openapi-starter-webmvc-ui` (e.g. `3.0.3` aligned with the parent BOM / project).
- Do **not** add Springfox; springdoc is the maintained stack for Jakarta + WebMVC.

## Global configuration

- Register the **OpenAPI** `Info`, **JWT security scheme**, and optional **contact/license** in a `@Configuration` bean (e.g. `config.openapi.OpenApiConfig`).
- Security scheme **name** must match annotations: use `OpenApiSecuritySchemes.BEARER_JWT` (`"bearer-jwt"`) in `Components.addSecuritySchemes(...)` and in `@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_JWT)` (inside `DocJwtSecured`).
- Prefer **no** global `addSecurityList` on `OpenAPI` so **public** endpoints stay without Bearer in the spec unless explicitly documented.
- Expose Swagger UI and OpenAPI JSON in **security**: permit `/swagger-ui/**`, `/v3/api-docs/**`, `/v3/api-docs.yaml` (and `/swagger-ui.html` if used) in `SecurityFilterChain`.
- Configure **springdoc** in `application.yaml` (`springdoc.api-docs.path`, `springdoc.swagger-ui.path`, optional tag/operation sorters).

## Shared building blocks (`shared.web.openapi`)

- **`OpenApiSecuritySchemes`**: constant `BEARER_JWT` — single scheme id for `OpenApiConfig` and `@SecurityRequirement`.
- **`StandardErrorResponses`**: common failure responses with `ApiErrorResponse` (`400`, `401`, `403`, `429`, `500`). Composed into `DocJwtSecured` and `DocPublicEndpoint`; do **not** repeat on controllers.
- **`DocJwtSecured`** (`shared.web.openapi.doc`): `@SecurityRequirement(bearer-jwt)` + `@StandardErrorResponses`. Meta-annotate **JWT** endpoint `Doc*` types (not used on controllers directly).
- **`DocPublicEndpoint`**: `@StandardErrorResponses` only (no Bearer). Meta-annotate **public** auth/session `Doc*` types.

**Do not** use generic verb wrappers named `*Operation` (e.g. `GetOperation`, `PostOperation`) on controllers — those are removed in favor of **per-endpoint** `Doc*` annotations.

## Per-controller and per-endpoint `Doc*` annotations

**Controllers stay minimal**: Spring web mappings (`@GetMapping`, …), rate limits, validation, and **one** documentation annotation per class and per method. **No** `io.swagger.v3.oas.annotations.*` imports in controller source files.

### Class-level (`@Target(TYPE)`)

- One annotation per REST controller, e.g. `@DocUserProfile`, `@DocUserManagement`, `@DocAuth`, `@DocContracts`.
- Encapsulates **`@Tag(name = "…", description = "…")`** only (module-specific naming and long descriptions live here).

### Method-level (`@Target(METHOD)`)

- **One annotation per HTTP handler**, named by **resource + intent**, not by HTTP verb alone — e.g. `DocUserProfileGetMe`, `DocContractsRenew`, `DocAuthLogin`.
- Each `Doc*` interface composes:
  - **`@DocJwtSecured`** or **`@DocPublicEndpoint`** (first),
  - **`@Operation(summary = …, description = …)`** (Swagger),
  - **`@ApiResponse`** (and `@Content` / `@Schema` for success bodies; extra `404` / `204` when needed).

Place these in **`…adapter.inbound.web.doc`** next to the controller package.

**Examples in-repo**: `module/account/.../web/doc`, `module/contract/.../web/doc`, **`module/crm/.../web/doc`** (`DocOpportunities`, `DocProjects`, `DocProjectMilestones`, plus one `Doc*` per endpoint).

## Roles and JWT

- Access tokens carry roles as Spring authorities **`ROLE_<NAME>`** (see `JwtAuthenticationFilter` and `Role` enum).
- Document operator expectations in **tag** or **operation** text inside the relevant `Doc*` annotation file.

## Related

- Hexagonal controller rules: `pimienta-backend-layer-conventions`.
- Package layout: `pimienta-backend-project-structure`.
