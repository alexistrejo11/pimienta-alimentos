# Backend (Spring API)

Production operations API for Pimienta Alimentos: auth, employees, contracts, CRM, inventory, payroll, tasks, headquarters, files, notifications. The POS Device API is implemented; its canonical cross-app contract and Android plan live in `../mobile/pos/docs/integration/`.

- **Stack:** Java 26, Spring Boot 4, `/api/v1`, PostgreSQL + Flyway, Redis, JWT, S3.
- **Package:** `io.github.alexistrejo11.pimienta.module.<boundedContext>`

## Before writing code

**Always** open [`.agents/skills/pimienta-backend-conventions/SKILL.md`](.agents/skills/pimienta-backend-conventions/SKILL.md) for backend work (modules, endpoints, migrations, POS). Then open any other matching skill below. Do not invent packages, Flyway enums, or error JSON.

Quick invariants (detail in that skill):

- Ports: `core/port/input` | `core/port/output`. Adapters: match the module (`adapter/` or `infrastructure/adapter/`); prefer `infrastructure/adapter` for new modules.
- Flyway: `db/migration/V{n}__*.sql`; enums as `VARCHAR` + `ck_*` CHECK — no PG `CREATE TYPE`.
- Errors: `ErrorCode` + module exception extending `ResourceNotFoundException` / `ConflictException` → `ApiErrorResponse`.
- Controllers thin: `@Valid` → `*Command` → use case → DTO; `@RateLimit` profiles; new lists use `PagedResponse`.
- Unauthenticated secured calls → **401**; POS contracts → `../mobile/pos/docs/integration/`.

## Architecture (hexagonal, thin domain)

This is a **simple server**: structure is hexagonal; **business rules in the domain are low**.

Per module (follow the folder names already used in that module):

- `core/domain` — aggregates as **state holders** (`BaseDomain` + `SafeBuilder`). No Spring, no JPA. No workflow policy on entities.
- `core/application` — `*UseCasesImpl`, commands, queries. **Workflow lives here**. Use-case **interfaces** live in `core/port/input`.
- `core/port/input` and `core/port/output` — ports.
- Inbound web / outbound JPA adapters — some modules use `adapter/`, others `infrastructure/adapter/`. **Match the module you are editing.**

Jakarta validation belongs on **HTTP DTOs**, not on rich domain invariants.

## Skills

| Skill | When |
|-------|------|
| [pimienta-backend-conventions](.agents/skills/pimienta-backend-conventions/SKILL.md) | **Default** — layout, Flyway, errors, HTTP, RateLimit |
| [pimienta-domain-repository-style](.agents/skills/pimienta-domain-repository-style/SKILL.md) | Aggregates, SafeBuilder, JPA nullability |
| [pimienta-domain-model](.agents/skills/pimienta-domain-model/SKILL.md) | Spanish alias → same as repository-style |
| [pimienta-backend-openapi](.agents/skills/pimienta-backend-openapi/SKILL.md) | Controllers / `Doc*` / springdoc |
| [pimienta-backend-integration-tests](.agents/skills/pimienta-backend-integration-tests/SKILL.md) | MockMvc `*IntegrationTest` |

POS contract and Android progress: [../mobile/pos/docs/integration/](../mobile/pos/docs/integration/README.md). Backend `docs/v2/pos_integration/` is historical only. Do not treat documentation as richer DDD than the skills above.
