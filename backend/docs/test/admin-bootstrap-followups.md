# Admin bootstrap — integration follow-ups

## 2026-09-13 — First-admin from env

- Bootstrap is an `ApplicationRunner`, not Flyway. Dev (`ddl-auto=update`) and prod (Flyway) both run it after schema setup.
- Test profile forces `pimienta.bootstrap.admin.enabled=false` so a developer `.env` cannot seed H2.
- Password is never rotated on later boots; only a missing ACTIVE ADMIN plus unused email creates a row.
- Bootstrap admin uses `OTHER` gender and no date of birth; change later via profile APIs if needed.

No extra follow-ups from this pass.
