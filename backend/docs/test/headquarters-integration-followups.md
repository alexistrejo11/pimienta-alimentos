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

## POS catalog list on PostgreSQL (2026-09-16)

- `GET /headquarters/{id}/pos-catalog` 500’d on PostgreSQL with `function lower(bytea) does not exist`. Hibernate 6 binds unused JPQL `String` params (`search`, `saleCategory`) and `''` in `coalesce` as `bytea`. H2 ITs did not catch it. List search now uses Criteria and only adds predicates when filters are present.

## POS Open Product settings (2026-09-16)

- `allowOpenProducts` is now preserved on create and update.
- Open Product categories are normalized by trimming, dropping blanks, and
  deduplicating case-insensitively. Enabling the policy without a category is
  rejected with `INVALID_ARGUMENT`.
## POS durable change feed concurrency tests

- Added deterministic bootstrap watermark concurrency coverage and retained-history cursor coverage.
- No additional non-fatal follow-ups surfaced from this test pass.

## Version starts at 1 (2026-09-27)

- `version_startsAtOneOnCreate_andIncrementsOnUpdate` asserts create returns `version: 1` and a later GET returns `2`. Hibernate used to seed `@Version` at 0 on insert (the Flyway `DEFAULT 1` never applied); `VersionedJpaEntity` now seeds 1 in `@PrePersist`.
- Under `@Transactional` tests the increment only shows after `EntityManager.flush()`; the PUT response itself still reports the pre-flush version. Clients that cache `version` from a PUT response should re-read if they start sending it back.
- Tests run on H2 with Flyway disabled, so the Flyway `version DEFAULT 1` columns are not exercised by the suite.

## POS product unified create (2026-09-28)

- Staff `POST /headquarters/{id}/pos-products` now uses the same role matcher as `pos-catalog` (ADMIN, DIRECTOR, MANAGER, EMPLOYEE) plus `HeadquarterAccessService`. Before this, the route fell through to ADMIN-only `anyRequest`.
- No extra follow-ups from this pass.
