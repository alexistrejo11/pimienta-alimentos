# E2E testing plan

Playwright specs live under `web/e2e/`. They hit the real Angular UI with a strict `MockApi` (`http://api.e2e.test/api/v1`). Production `web/src/` is not changed for coverage.

## Phase 4 — Done

Slices A–E are implemented against the UI that exists today (UI truth over the original audit wording).

### Completed slices

| Slice | Coverage |
|---|---|
| A (infra) | `web/playwright.config.ts`, `MockApi`, workspace fixtures, POMs (`ListSearchField`, `Pagination`, `DataState`, `ConfirmDialog`, `PromptDialog`, `HeadquarterSelect`, `WorkspaceFormPage`, `WorkspaceSidebar`), DTO builders |
| B (P0 HR + POS money) | Empleados (multipart `employee` JSON), usuarios (approve/roles/HQ/`PromptDialog` block), POS ventas + turnos + cortes redirect |
| C (catalogs + remaining ERP) | Productos maestros, bodega, catálogo de stock, POS catálogo, sedes list/edit, nómina, tareas, contratos |
| D (POS admin, dashboards, auth) | POS config/dispositivos/enrolamiento/operadores, dashboards oficina y operaciones, asistencia de hoy, register, RBAC sidebar |
| E (mobile) | Playwright project `mobile` (`devices['Pixel 7']`) limited to `web/e2e/specs/smoke/workspace-mobile.e2e.ts` (drawer: Abrir navegación → Sedes → Cerrar navegación) |

Desktop Chromium ignores the mobile spec so the suite is not doubled.

### Explicit leftovers (not in Phase 4)

- **Archivos** first-load race: `archivos-page.ts` does not `GET /files/management` on first paint.
- **Excel / import** clicks (empleados export, nómina import) — skipped; no file-download assertions.
- **OpenAPI contract job** — not added.
- **CI shards** — not added.
- **`/erp/tasks` stub** — skipped.
- **`mi-asistencia` punch** — not covered.
- **Broken relative redirects** (`pos/sedes`, nested `productos` aliases) — 404 today; not added as smoke rows.
- **Convocatorias UI** — no page in `web/src/`; list/create/lifecycle spec omitted until the screen exists.

### How to run

```sh
cd web
npm test
npx playwright test
```
