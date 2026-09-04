# Backend (Spring API)

Production operations API for Pimienta Alimentos: auth, employees, contracts, CRM, inventory, payroll, tasks, headquarters, files, notifications.

- **Stack:** Java 26, Spring Boot 4, `/api/v1`, PostgreSQL + Flyway, Redis, JWT, S3.
- **Package:** `io.github.alexistrejo11.pimienta.module.<boundedContext>`

## Architecture (hexagonal, thin domain)

This is a **simple server**: structure is hexagonal; **business rules in the domain are low**.

Per module (follow the folder names already used in that module):

- `core/domain` — aggregates as **state holders** (`BaseDomain` + `SafeBuilder`). No Spring, no JPA. No workflow policy on entities.
- `core/application` — `*UseCases` / `*UseCasesImpl`, commands, queries. **Workflow lives here** (status, stock, approvals, imports).
- `core/port/input` and `core/port/output` — ports.
- Inbound web / outbound JPA adapters — some modules use `adapter/`, others `infrastructure/adapter/`. **Match the module you are editing.**

Jakarta validation belongs on **HTTP DTOs**, not on rich domain invariants.

Controllers stay thin: `@Valid` → command → use case → response DTO.

## Skills

Read the matching skill when the task needs it:

- [pimienta-backend-conventions](.agents/skills/pimienta-backend-conventions/SKILL.md) — layout, naming, thin controllers, pagination
- [pimienta-domain-repository-style](.agents/skills/pimienta-domain-repository-style/SKILL.md) — canonical English domain / persistence style
- [pimienta-domain-model](.agents/skills/pimienta-domain-model/SKILL.md) — same topic in Spanish
- [pimienta-backend-openapi](.agents/skills/pimienta-backend-openapi/SKILL.md) — springdoc `Doc*` annotations
- [pimienta-backend-integration-tests](.agents/skills/pimienta-backend-integration-tests/SKILL.md) — MockMvc ITs

Do not treat generated architecture docs as richer DDD than the skills above.
