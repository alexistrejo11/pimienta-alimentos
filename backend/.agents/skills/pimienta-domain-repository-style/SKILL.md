---
name: pimienta-domain-repository-style
description: >-
  Pimienta repository-style domain: thin aggregates, BaseDomain, SafeBuilder (register/reconstruct),
  sentinel enums, nullable JPA, Jakarta validation on HTTP DTOs only. Use when adding or
  refactoring domain/persistence in backend Java modules (task, contract, employee, inventory, POS).
---

# Pimienta domain (repository-oriented)

## What the domain is for

Treat bounded-context **aggregates as state holders**, not policy engines. The database may store
`NULL` on many non-critical columns. The domain **does not** enforce business workflows (status
transitions, assignment side effects, import rules, etc.); **application use cases** own
orchestration and invariants that belong to application services.

## What stays out of the domain

- No **static factory methods** such as `Task.create(...)` on aggregates. Construction flows
  through **`SafeBuilder`**: `register()` for new entities, `reconstruct()` for hydration from
  persistence, optional `build()` where applicable.
- No **business rules** in domain methods (e.g. do not auto-flip status on assign inside the
  entity). Prefer **commands + use case** methods that load, mutate via setters or builder steps,
  and persist.
- Avoid domain **parameter records** that mirror HTTP or application DTOs; use **commands** under
  `core/application/command` for writes. Prefer new `*Command` types; do not add CRM-style
  application `*Params` on greenfield code.
- **`revise(existing)`** on SafeBuilder is **Contract-specific** (and similar legacy paths). Most
  aggregates only need `register()` / `reconstruct()` plus setters or use-case mutation. Do not
  add `revise()` to new aggregates unless matching an existing pattern in that module.

## BaseDomain and builder

- Aggregates with identity and auditing extend **`BaseDomain<ID>`** (`id`, `createdAt`, `updatedAt`,
  `deletedAt`, `version`).
- Use a nested **`SafeBuilder`**: `with*` methods normalize nulls for in-memory consistency; map
  persistence nulls when reconstructing.
- **`register()`**: creation path; validate only **creation invariants** you still want in the
  domain (keep this minimal if product rules live in use cases).
- **`reconstruct()`**: load from JPA without business validation.

## Optional / missing data in the model

- Prefer **sentinel enums** (e.g. `UNDEFINED`) for values that can be absent in the database; expose
  safe getters so callers rarely see raw nulls.
- For persistence, add **`getXxxOrNull()`** (or mapper helpers) when the DB must not store the
  sentinel—map in the **outbound persistence mapper**, not in random layers.

## JPA entities

- Use **`nullable = true`** on columns that are **not** critical for row integrity or locking (e.g.
  optional text, status-like fields that can be backfilled).
- Keep **`nullable = false`** where the physical model requires it (`created_at`, `updated_at`,
  `version`, etc.).
- Mappers should use **`blankToNull`** for strings and **`*OrNull`** for enums when writing to JPA.

## Validation boundaries

- **Jakarta Bean Validation** (`@NotBlank`, `@NotNull`, `@Valid`, size, etc.) belongs on **web DTOs**
  (`*Request`) and query objects as **format/shape** checks.
- Do not duplicate the same rules as “domain validation” unless you deliberately keep a tiny
  creation guard in `register()`.

## Reference modules

- **Employee / contract**-style aggregates and mappers under `module/employees`, `module/contract`.
- **Task** after refactor: `module/task/core/domain/Task.java`, `TaskManagementUseCasesImpl`,
  `TaskPersistenceMapper`, `TaskJpaEntity`, web DTOs with `@Schema`.
- **Headquarter**: `module/headquarter/core/domain/Headquarter.java`, `HeadquarterUseCasesImpl`,
  `HeadquarterPersistenceMapper`, `HeadquarterJpaEntity`, `doc/DocHeadquarter*.java`.
- **CRM**: `module/crm/core/domain/Opportunity.java`, `Project.java`, `ProjectMilestone.java` with
  `SafeBuilder` (`register` / `reconstruct`); transitions in `OpportunityUseCasesImpl`,
  `ProjectUseCasesImpl`, `ProjectMilestoneUseCasesImpl`; no domain `*Params` records—application
  commands under `core/application/command` only.

## Related skill

- **`pimienta-backend-conventions`**: packages, OpenAPI `Doc*` annotations, adapters.
