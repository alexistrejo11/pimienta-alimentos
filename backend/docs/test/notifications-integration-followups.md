# Notifications module — integration test follow-ups

## Notes from initial `NotificationIntegrationTest` pass (2026-06-04)

- Read-only APIs are covered by seeding rows through **`NotificationJpaRepository`** (no POST endpoints).
- Manager log search (`GET /api/v1/notifications/logs`) forces **`LOG`** channel and **today’s** `createdAt` window; tests assert EMAIL / yesterday LOG rows are excluded.

## 2026-09-11 — Role hardening

- `/api/v1/notifications/logs/**` is **ADMIN-only** (MANAGER no longer allowed). Channel/today filter assertions now run as ADMIN.

## H2 / DDL (2026-06-04)

- **`NotificationJpaEntity.template_variables`** used `columnDefinition = "jsonb"`, so Hibernate did not create the `notifications` table on the H2 integration profile (`Table "NOTIFICATIONS" not found`). Same class of issue as `TaskJpaEntity.checklist` (see `task-integration-followups.md`). Mapping was adjusted to rely on `@JdbcTypeCode(SqlTypes.JSON)` only; PostgreSQL still uses `JSONB` via Flyway `V10__notifications.sql`.
