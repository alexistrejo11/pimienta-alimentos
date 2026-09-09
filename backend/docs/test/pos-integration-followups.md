# POS integration test follow-ups

## 2026-09-08 — B6 incidents & reports

- Waste / shift-close reports project from ACCEPTED `pos_sync_events` ledger rows (JSONB payload). Dedicated fact tables for waste/cancellations/shift closes are still out of B6; richer report columns will need those tables later.
- Product report aggregation loads all matching groups then pages in memory (cafeteria-scale). Revisit if HQ volume grows.
- Incident list `openOnly` Boolean JPQL filter is intentional; confirm index usage when open incidents grow large.
- Method-security denials (`AuthorizationDeniedException` from `@PreAuthorize`) now map to **403** `FORBIDDEN` in `GlobalExceptionHandler` (previously fell through to 500).

No extra follow-ups blocking B6 merge from this pass.
