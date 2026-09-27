# Web (Angular)

Public marketing/legal site plus authenticated staff workspace (empleados, CRM, tareas, sedes, contratos, archivos, nómina). UI copy is mostly Spanish.

- **Stack:** Angular 21, standalone, zoneless, SSR, Tailwind CSS v4.
- **Layout:** `src/app/pages/` (routes) and `src/app/core/` (HTTP services, models). No `features/` folder.

## Conventions for this repo

Modern Angular for **new** UI: `signal()`, `input()`, `output()`, `inject()`, built-in control flow (`@if` / `@for` / `@switch`). No `NgModule`. Do not add `standalone: true`.

**Existing HTTP layer:** services return RxJS `Observable`; pages `.subscribe()`. Keep that split unless the task is an explicit HTTP/signals migration (`toSignal`, `httpResource`, etc.).

Visual tokens and layout patterns: use the UI skill below; do not invent ad-hoc brand colors.

### UI copy (Spanish)

All **user-visible** text is Spanish and human-readable: labels, buttons, empty states, errors shown to the user, table headers, and **enum values**.

- Wire/API codes stay English (`ACTIVE`, `DIRECTOR`, `FINISHED_GOOD`).
- Templates must **never** print raw enum codes. Use helpers in `src/app/core/i18n/enum-labels.ts` (e.g. `roleLabel`, `itemCategoryLabel`, `inventoryStatusLabel`).
- When adding a new API enum to the UI, add its Spanish label to `enum-labels.ts` in the same change.

## Skills

- [angular-developer](.agents/skills/angular-developer/SKILL.md) — official Angular (signals, CLI, templates, DI)
- [pimienta-frontend-ui](.agents/skills/pimienta-frontend-ui/SKILL.md) — brand tokens from `src/styles.css` (keep this skill in sync with that file); includes Spanish UI copy rules

Skip `angular-new-app`; this application already exists.
