# Web (Angular)

Public marketing/legal site plus authenticated staff workspace (empleados, CRM, tareas, sedes, contratos, archivos, nómina). UI copy is mostly Spanish.

- **Stack:** Angular 21, standalone, zoneless, SSR, Tailwind CSS v4.
- **Layout:** `src/app/pages/` (routes) and `src/app/core/` (HTTP services, models). No `features/` folder.

## Conventions for this repo

Modern Angular for **new** UI: `signal()`, `input()`, `output()`, `inject()`, built-in control flow (`@if` / `@for` / `@switch`). No `NgModule`. Do not add `standalone: true`.

**Existing HTTP layer:** services return RxJS `Observable`; pages `.subscribe()`. Keep that split unless the task is an explicit HTTP/signals migration (`toSignal`, `httpResource`, etc.).

Visual tokens and layout patterns: use the UI skill below; do not invent ad-hoc brand colors.

## Skills

- [angular-developer](.agents/skills/angular-developer/SKILL.md) — official Angular (signals, CLI, templates, DI)
- [pimienta-frontend-ui](.agents/skills/pimienta-frontend-ui/SKILL.md) — brand, Tailwind theme, workspace/marketing layouts

Skip `angular-new-app`; this application already exists.
