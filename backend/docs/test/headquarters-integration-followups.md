# Headquarters module — follow-ups after integration tests

Non-fatal behaviors, consistency gaps, and product decisions to revisit. **Resolved in code** items are noted so you can close them when satisfied.

## Resolved during test work

- **JPA `@Version` vs domain bumps:** `Headquarter.revise()` used to set `version = existing.version + 1`, and `touch()` incremented `version` in memory. The JPA entity also increments `@Version` on update, which led to `ObjectOptimisticLockingFailureException` on PUT. **Change:** copy `existing.getVersion()` in `revise()`; `touch()` only updates `updatedAt` so persistence owns version increments.

## API / repository consistency

- **`findById` vs soft delete:** `findById` does not filter `deletedAt`; `findByName` uses `deletedAtIsNull`. After soft delete, GET by id can still return **200** with `deletedAt` set, while GET by name returns **404**. Decide whether clients should get **404** for deleted rows by id as well, or document “tombstone by id” as intentional.

- **Paged list:** `findAll(Pageable)` may include soft-deleted headquarters in `Page.content` if the JPA query does not filter them. Confirm product expectation (hide deleted in UI vs admin views).

## Data model

- **Unique `name`:** If the business requires one row per name, add a DB unique constraint (and handle conflicts in the API). Today duplicate names may be possible.

## HTTP contract

- **`HeadQuarterRequest` has no `version` field:** Updates are “last write wins” on the loaded row; there is no If-Match / optimistic concurrency from the client. If you need conflict detection across tabs or devices, expose `version` on read and require it (or ETag) on write.

## Tests

- **`HeadquarterIntegrationTest`** uses `$.content` for Spring `Page` JSON. If you switch to a custom `PagedResponse` wrapper, update assertions accordingly.

## POS B1 (2026-09-08)

- No extra follow-ups from this pass for headquarters POS settings/catalog endpoints.
- GET `pos-settings` returns `HEADQUARTER_POS_SETTINGS_NOT_FOUND` until the first PUT (by design: settings are created on upsert, not on HQ create).
