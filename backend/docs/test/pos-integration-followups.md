# POS integration test follow-ups

## 2026-09-08 — B6 incidents & reports

- Waste / shift-close reports project from ACCEPTED `pos_sync_events` ledger rows (JSONB payload). Dedicated fact tables for waste/cancellations/shift closes are still out of B6; richer report columns will need those tables later.
- Product report aggregation loads all matching groups then pages in memory (cafeteria-scale). Revisit if HQ volume grows.
- Incident list `openOnly` Boolean JPQL filter is intentional; confirm index usage when open incidents grow large.
- Method-security denials (`AuthorizationDeniedException` from `@PreAuthorize`) now map to **403** `FORBIDDEN` in `GlobalExceptionHandler` (previously fell through to 500).

No extra follow-ups blocking B6 merge from this pass.

## 2026-09-10 — Public Android APK release endpoint

- `GET /api/v1/pos/releases/android/latest` is public (`permitAll`). Anyone who can hit the API can obtain a time-limited pre-signed APK URL once CI has published the manifest. Acceptable for internal staff distribution via the marketing home; revisit if the APK must stay staff-JWT only.
- Until the first POS CI publish, the endpoint returns `404` / `POS_APK_NOT_FOUND`; the landing CTA shows a soft error rather than a hard failure of the page.

No extra follow-ups blocking this pass.

## 2026-09-11 — Role hardening (operators)

- `GET /api/v1/pos/admin/operators` now filters by HQ for MANAGER (`enforceHeadquarterFilter` + paged `findByHeadquarterId`). Optional `headquarterId` query for ADMIN.
- `CreatePosOperatorRequest.headquarterIds` is `@NotEmpty` (400 `VALIDATION_FAILED` when missing/empty).
- `requireOperatorAccess` denies MANAGER when operator HQs do not contain the manager HQ (previously always allowed).

No extra follow-ups from this pass.
