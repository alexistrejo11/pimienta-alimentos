# Pimienta Alimentos

Operations software for **Pimienta Alimentos** (food company): public site, staff workspace, REST API, and a school-cafeteria POS in early development.

## Apps

| App | Role |
|-----|------|
| [backend/AGENTS.md](backend/AGENTS.md) | Spring Boot API (`/api/v1`) |
| [web/AGENTS.md](web/AGENTS.md) | Angular 21 marketing site + authenticated workspace |
| [mobile/pos/AGENTS.md](mobile/pos/AGENTS.md) | Android Compose POS (scaffold) |

Work in the app folder that owns the change. Read that app’s `AGENTS.md` before coding.

## Skills

Skills live **per app**, not at the repo root:

- [backend/.agents/skills/](backend/.agents/skills/)
- [web/.agents/skills/](web/.agents/skills/)

Open a skill when the task matches it (OpenAPI, domain model, UI tokens, Angular conventions). For **backend/** work, always start with `backend/.agents/skills/pimienta-backend-conventions/SKILL.md` (see `backend/AGENTS.md`). POS mobile has no skills yet; its comment rule is in `mobile/pos/AGENTS.md`. Backend POS specs live under `backend/docs/v2/pos_integration/`.
