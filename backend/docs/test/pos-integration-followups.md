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

## 2026-09-15 — Sales/cortes sync visibility

- Android now enqueues `SyncWorker` after sale, shift, sangría, and cash-count facts; one-time unique work uses `REPLACE` so drains are not stuck behind `KEEP`.
- Shift materialization failures return per-event `REJECTED` (HTTP 200 batch) instead of aborting the whole ingest with `MALFORMED_PAYLOAD` 400.
- Ventas report includes `REQUIRES_REVIEW` sales and exposes `syncStatus`; product/summary reports remain ACCEPTED-only.
- Web Cortes reads `GET /pos/admin/shifts` with `status=CLOSED` and local-day `closedAt` filters instead of raw ledger JSON.
- POS Mermas web route removed; HQ scrap remains at `/app/inventario/mermas`. Tablet `WASTE_RECORDED` is still ledger-only on the backend.

No extra follow-ups blocking this pass.

## 2026-09-16 — Admin shift list on PostgreSQL

- `GET /pos/admin/shifts?status=OPEN` 500’d with `could not determine data type of parameter $7`. JPQL still bound unused `Instant` filters (`openedFrom`/`closedFrom`) as untyped nulls. Criteria now adds date predicates only when those values are present. H2 ITs did not catch it.

No extra follow-ups blocking this pass.

## 2026-09-17 — Device catalog create

- `POST /api/v1/pos/sync/products` uses device JWT and HQ from the token. Staff JWT is 403 (`SCOPE_pos:sync` only).
- `createdByOperatorId` is validated when present but not persisted on `headquarter_items` (no column). Audit is request-time only.
- Empty barcode is omitted from JSON (`NON_NULL`) rather than returned as `null`.

No extra follow-ups from this pass.

## 2026-09-21 — Open amount without PIN

- Ingest of `OPEN_AMOUNT` no longer requires `authorizedByOperatorId` / `authorizedAt`. Sales still land in `REQUIRES_REVIEW` with `OPEN_PRODUCT`.
- `OPEN_PRODUCT_AUTHORIZATION_INVALID` remains mapped for historic incidents but is no longer emitted.

No extra follow-ups from this pass.

## 2026-09-23 — Sales accepted without review

- `SALE_CONFIRMED` is stored as `ACCEPTED` with no sync incident. Open amount, pending catalog, price mismatch, and negative stock no longer open a review queue.
- A `saleId` already stored under another `eventId` is `REJECTED` and does not overwrite the sale. No incident is created.
- `V20` marks existing `REQUIRES_REVIEW` events `ACCEPTED` and closes open incidents so those tickets enter ACCEPTED-only reports. Sale lines and prices are unchanged.
- The admin accept API remains in the backend. The web no longer lists or accepts incidents.
- Linking a later catalog barcode back onto `PENDING_CATALOG` lines is not implemented in this pass.

No extra follow-ups from this pass.
